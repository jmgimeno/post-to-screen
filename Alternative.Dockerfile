FROM openjdk:8

RUN apt-get -y update && \
    apt-get -y install leiningen 

COPY . /app

WORKDIR /app

RUN lein uberjar

EXPOSE 10555

CMD java $JVM_OPTS -cp /app/target/post-to-screen.jar clojure.main -m post-to-screen.server