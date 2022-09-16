# post-to-screen

For sharing code on the screen.

## To Build

```shell
lein uberjar
````

## To Run

```shell
java -jar target/post-to-screen-0.1.0-SNAPSHOT-standalone.jar
open http://localhost:10555
````

or use the environment variable `PORT` to change the port

## To Build Image

```shell
docker build -t post-to-screen .
````

## To Run Image

```shell
docker run --detach --publish 8000:10555 --name post-to-screen-1 post-to-screen
````

## To Deploy

* The `Dockerfile` used to deploy to fly.io does not work
* But the nixpacks used on the web work
* But an alternative `Dockerfile` does work instead

