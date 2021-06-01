# LogGenerator
A utility for creating log files, or sending Log events to a defined URL. The tool is intended to help designed to help test Fluentd configurations. Through the use of this tool we can simulate different kinds of log file.



The tool uses a separate source file for its log entries. This file is identified in the properties file which controls all aspects of the utility. Full documentation at /docs/readme.md


The code is licensed under Apache 2 

## Batch & Shell Scripts

A number of batch and shell scripts are provided in this directory. They are for the purpose of illustrating the different approaches to using the tool.



# Configuration File Properties



- | Configuration Name | Example Value                        | Description                                                  |
  | ------------------ | ------------------------------------ | ------------------------------------------------------------ |
  | OUTPUTTYPE         | TCP                                  | Defines the output type to use. This will dictate which other attributes are needed. The values can be: file, HTTP, TCP, JUL, STDOUT, ERROUT |
  | SOURCE-SEPARATOR   | ----                                 | Defines a means to identify field separation within the log source file. If undefined this is white space. |
  | TARGET-SEPARATOR   | ----                                 | Provides the means to define the way that each field is separated. By default this is simply whitespace. |
  | SOURCEFORMAT       | %t %m                                | Defines how to read the log file being sourced. This is expressed as a series of characters preceded by % |
  | TARGETFORMAT       | %t >> %m                             | Defines how the log event text should be output. This is a combination of value indicates which are substituted during the output |
  | SOURCE             | testData\\source.txt                 | The file containing the logs  to be replayed or synthetic data |
  | TARGETFILE         | log.log                              | The file to be written to.                                   |
  | TARGETIP           | 127.0.0.1                            | Defines the IP for to direct log events to. This is used in conjunction with TCP based targets for log events |
  | TARGETPORT         | 28080                                | The port number to be used in conjunction with the TARGETIP for directing log events using TCP traffic |
  | TARGETDTG          |                                      | Uses the Java date time formatter string to describe how to write the date time into the log. Details for the definition of formats can be read at https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html |
  | TARGETURL          |                                      |                                                              |
  | SOURCEDTG          |                                      | Uses the Java date time formatter string to describe how to read the date time from the log. Details for the definition of formats can be read at https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html |
  | OUTPUTTYPE         | HTTP, file, TCP, JUL, STDOUT, ERROUT | The way the logs will be output. JUL is the Java Logging Utility |
  | DEFAULT-LOGLEVEL   | DEBUG                                | Defines the log level to be set for JUL e.g. INFO, DEBUG etc |
  | ACCELERATEBY       | 2                                    | A numeric value used to accelerate the rate of playback rather than applying the real-time defined intervals. |
  | JULCONFIG          |                                      | Identifies the configuration file to be used by the Java Utility Logger framework. |
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
| %i   | the iteration counter when looping over the same log events  |
| %j   | A means to include the line counter - so you can trace the generated log event back to the source log file if desired. |

