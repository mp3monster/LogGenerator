# LogGenerator
A utility for creating log files, or sending Log events to a defined URL. The tool is intended to help designed to help test Fluentd configurations. Through the use of this tool we can simulate different kinds of log file.



The tool uses a separate source file for its log entries. This file is identified in the properties file which controls all aspects of the utility. Full documentation at /docs/readme.md


The code is licensed under Apache 2 

## Batch & Shell Scripts

A number of batch and shell scripts are provided in this directory. They are for the purpose of illustrating the different approaches to using the tool.



# Configuration File Properties



| Configuration Name | Example Value                        | Description                                                  |
| ------------------ | ------------------------------------ | ------------------------------------------------------------ |
| SOURCE-SEPARATOR   |                                      |                                                              |
| TARGET-SEPARATOR   | ----                                 | Provides the means to define the way that each field is separated |
|                    | SOURCEFORMAT%t %m                    | Defines how to read the log file being sourced. This is expressed as a series of characters preceded by % |
| TARGETFORMAT       | %t >> %m                             | Defines how the log event text should be output. This is a combination of value indicates which are substituted during the output |
| SOURCE             | testData\\source.txt                 | The file containing the logs  to be replayed.                |
| TARGETFILE         | log.log                              | The file to be written to.                                   |
| TARGETIP           | 127.0.0.1                            | Defines the IP for to direct log events to                   |
| TARGETPORT         |                                      |                                                              |
| TARGETDTG          |                                      |                                                              |
| TARGETURL          |                                      |                                                              |
| SOURCEDTG          |                                      |                                                              |
| OUTPUTTYPE         | HTTP, file, TCP, JUL, STDOUT, ERROUT | The way the logs will be output. JUL is the Java Logging Utility |
| DEFAULT-LOGLEVEL   |                                      | Defines the log level to be set for JUL e.g. INFO, DEBUG etc |
| ACCELERATEBY       | 2                                    | A numeric value used to accelerate the rate of playback rather than applying the real-time defined intervals. |
| JULCONFIG          |                                      |                                                              |
| VERBOSE            | y, t, yes, true                      | When enables, information about what the log simulator is perform to be sent to to std out. |
| REPEAT             | 5                                    | Repeat the data set output 5 times.                          |



The tags to describe the source and target formats are:

| Tag  | Description                                                  |
| ---- | ------------------------------------------------------------ |
| %t   | The position to include the date time stamp                  |
| %m   | The position for the log message                             |
| %l   | The position for the log level e.g DEBUG, INFO etc           |
| %c   | If you want to include a class path / class style piece of information this is positioned using this code |
| %p   | represents the process / thread Id                           |

