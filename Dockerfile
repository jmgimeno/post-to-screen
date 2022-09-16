FROM openjdk:17-jdk-slim

WORKDIR /home/app

ADD ./target/*standalone.jar ./post-to-screen.jar

EXPOSE 10555

CMD java $JAVA_OPTS -jar post-to-screen.jar
