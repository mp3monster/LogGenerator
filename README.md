# LogGenerator

#### aka LogSimulator and EventSimulator

A utility for creating log files or sending Log events to a defined URL. The tool's original intent was to help test Fluentd configurations. Through the use of this tool, we can simulate different kinds of log files. However, the ability to read a source of events, format and send the events to an event-based solution (which may or not be log events) or event store, such as a log file, has resulted in the tool being extended to make it usable with other channels.

The tool uses a separate source file for its log entries. This file is identified in the properties file, which controls all aspects of the utility. Full documentation at [/docs/readme.md](./docs/readme.md). Which includes all the details for targeting the different outputs. The list includes:

- single and multiline log files in different formats
- TCP socket
- HTTP
- Java Logging Utility (JUL)
- stdout, errout
- Custom, which allows bringing your own destination, and provided custom plugins for:
  - OCI Logging
  - OCI Notifications
  - OCI Queue


The code is licensed under [Apache 2](https://www.apache.org/licenses/LICENSE-2.0).

## Batch & Shell Scripts

A number of batch and shell scripts are provided in this directory. They are for the purpose of illustrating the different approaches to using the tool.

## Configuration File Properties

Details on how the configuration works can be found at [./docs/readme.md](https://github.com/mp3monster/LogGenerator/tree/master/docs)

#### Book exercises

If you're looking for the exercise content and results used in the book [Logging In Action with Fluentd](https://www.manning.com/books/unified-logging-with-fluentd?utm_source=Phil&utm_medium=affiliate&utm_campaign=book_wilkins_unified_6_3_20&a_aid=Phil&a_bid=c50c008d) - these are in the download pack available with the book.

## Additional Packaging and Deployment Options for LogGenerator

The log generator can be deployed either as a JAR file or in a Docker container. The information for these configs can be read at:

- [./jar/readme.md](https://github.com/mp3monster/LogGenerator/tree/master/jar) - how to generate the  Groovy app as a JAR, including a previously generated JAR ready to be taken and used
- [./docker/readme.md](https://github.com/mp3monster/LogGenerator/tree/master/docker) - the details of the Docker image creation and deployment.
- [./Kubernetes/readme.md](https://github.com/mp3monster/LogGenerator/tree/master/Kubernetes) - details of the Kubernetes configuration to deploy the simulator

