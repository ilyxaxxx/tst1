FROM openjdk
ADD app.jar . 
ENTRYPOINT ["java","-jar","/app.jar"]
