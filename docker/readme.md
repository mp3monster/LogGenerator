# Summary
This is a containerization of the LogGenerator utility. So it can be used to help see the effect of log drivers or become part of a Kubernetes solution. As the solution uses Groovy we build upon the __Groovy__ Docker Hub image which itself is built on a Linux image.

## Build Process
The build process is implemented simply by using the __create.sh__ script, which can be run within the retrieved code. 

The output of the build process has then been pushed to Docker Hub - __https://hub.docker.com/repository/docker/mp3monster/log-simulator__

## Execution Behaviour & Configuration to customize
With a vanilla deployment, the container will run the LogGenerator tool using an internal small test data set and configuration which will mean that the logs are generated and sent to standard out. The configuration can then be replaced with specific scenarios.

The override can be achived by mapping __/vol/conf/__ folder which should retain the external configuration.
