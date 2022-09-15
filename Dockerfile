FROM openjdk:17-jdk-slim

EXPOSE 10555

COPY target/post-to-screen.jar /app/

WORKDIR /app

CMD ["java", "-jar", "post-to-screen.jar"]
