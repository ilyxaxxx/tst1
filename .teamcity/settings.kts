import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.dockerRegistry
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2020.2"

project {

    vcsRoot(V1)

    buildType(Build)

    params {
        param("APP_NAME", "app1")
        param("APP_VER", "v1")
        param("STEP1_NAME", "building code")
        param("DOCKER_REGISTRY", "100.100.100.101:1186")
        param("NEXUS_ADDR", "100.100.100.101:8081")
        param("NEXUS_URL", "http://%NEXUS_ADDR%")
    }

    features {
        dockerRegistry {
            id = "PROJECT_EXT_3"
            name = "Private Docker Registry"
            url = "http://100.100.100.101:1186"
            userName = "docker"
            password = "credentialsJSON:902a9ebf-a94d-48e2-af79-33ba35f8b046"
        }
    }
}

object Build : BuildType({
    name = "Build"

    artifactRules = """
        Dockerfile
        initial/target/gs-maven-0.1.0.jar => app.jar
    """.trimIndent()

    vcs {
        root(V1)
    }

    steps {
        maven {
            goals = "deploy"
            pomLocation = "initial/pom.xml"
            runnerArgs = "-Dmaven.test.failure.ignore=true -DaltDeploymentRepository=nexus-public::default::%NEXUS_URL%/repository/maven-releases/"
            workingDir = "complete"
            isIncremental = true
        }
        dockerCommand {
            name = "docker"
            commandType = build {
                source = content {
                    content = """
                        FROM %DOCKER_REGISTRY%/openjdk
                        ADD %NEXUS_URL%/repository/maven-releases/org/springframework/gs-maven/0.1.0/gs-maven-0.1.0.jar app.jar 
                        ENTRYPOINT ["java","-jar","app.jar"]
                    """.trimIndent()
                }
                namesAndTags = "%DOCKER_REGISTRY%/%APP_NAME%:%APP_VER%"
                commandArgs = "--pull"
            }
            param("dockerImage.platform", "linux")
        }
        dockerCommand {
            name = "push"
            commandType = push {
                namesAndTags = "%DOCKER_REGISTRY%/%APP_NAME%:%APP_VER%"
            }
        }
    }

    triggers {
        vcs {
            triggerRules = "+:root=${V1.id}:**"

            branchFilter = ""
        }
    }

    failureConditions {
        executionTimeoutMin = 30
        failOnMetricChange {
            metric = BuildFailureOnMetric.MetricType.ARTIFACT_SIZE
            units = BuildFailureOnMetric.MetricUnit.DEFAULT_UNIT
            comparison = BuildFailureOnMetric.MetricComparison.LESS
            compareTo = value()
        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3"
            }
        }
    }

    dependencies {
        artifacts(RelativeId("Build")) {
            buildRule = lastSuccessful()
            artifactRules = "+:app.jar => apps"
        }
    }
})

object V1 : GitVcsRoot({
    name = "v1"
    url = "https://github.com/spring-guides/gs-maven/"
    branch = "refs/heads/master"
})
