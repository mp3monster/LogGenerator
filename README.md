# LogGenerator
A utility for creating log files, or sending Log events to a defined URL. The tool is intended to help designed to help test Fluentd configurations. Through the use of this tool we can simulate different kinds of log file.



The tool uses a separate source file for its log entries. This file is identified in the properties file which controls all aspects of the utility. Full documentation at /docs/readme.md


The code is licensed under Apache 2 

## Batch & Shell Scripts

A number of batch and shell scripts are provided in this directory. They are for the purpose of illustrating the different approaches to using the tool.



## Configuration File Properties

Details on how the configuration works can be found at [./docs/readme.md](https://github.com/mp3monster/LogGenerator/tree/master/docs)



## Additional Packaging and Deployment Options for LogGenerator

The log generator can be deployed either as a JAR file or in a Docker container. The information for these configs can be read at:

- [./jar/readme.md](https://github.com/mp3monster/LogGenerator/tree/master/jar) - how to generate the the Groovy app as a JAR including a previously generated JAR ready to be taken and used
- [./docker/readme.md](https://github.com/mp3monster/LogGenerator/tree/master/docker) - the details of the Docker image creation and deployment.
- [./Kubernetes/readme.md](https://github.com/mp3monster/LogGenerator/tree/master/Kubernetes) - details of the Kubernetes configuration to deploy the simulator

