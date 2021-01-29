FROM openjdk
ADD http://100.100.100.101:8081/repository/maven-releases/org/springframework/gs-maven/0.1.0/gs-maven-0.1.0.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
