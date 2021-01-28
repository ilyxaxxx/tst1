FROM openjdk
ADD apps/app.jar . 
ENTRYPOINT ["java","-jar","/app.jar"]
