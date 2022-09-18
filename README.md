# post-to-screen

For sharing code on the screen.

## To compile and run for development

```shell
lein clean
lein cljsbuild one dev
lein run
open http://localhost:10555
```

## To build the standalone jar

```shell
lein clean
lein uberjar
```

## To run the standalone jar

```shell
java -jar target/post-to-screen-0.1.0-SNAPSHOT-standalone.jar
open http://localhost:10555
```

or use the environment variable `PORT` to change the port

## To build the docker image

```shell
docker build -t post-to-screen .
```

## To run the docker image

```shell
docker run --detach --publish 8000:10555 --name post-to-screen-1 post-to-screen
```

## Deploy to fly.io

```shell
flyctl auth login
flyctl launch
```

And the app is deployed at [post-to-screen](https://post-to-screen.fly.dev/)
