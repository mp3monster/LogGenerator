The tool is executed with the command: *groovy logSourceSimulator.groovy <properties file>* for example: groovy *logSourceSimulator.groovy testConfigurations\\tool.properties*

The properties file drives all the different possible behaviours. The following table describes each of the properties and when they are needed.



| Property         | Description                                                  | Example                                                      | Required |
| ---------------- | ------------------------------------------------------------ | ------------------------------------------------------------ | -------- |
| OUTPUTTYPE       | Identifies the type of output to use for the log content. The supported values are: console, file, and HTTP | HTTP                                                         | Y        |
| SOURCE           | The file which will be used as the source for the log events | .\source.txt                                                 | Y        |
| SOURCEFORMAT     | This describes how the source file will be structured. Each of the parts needed are denoted using the pattern of %<character> where the character will represent the element type.  The values supported are:  %t - time (which can be expressed as +nnn or as a date time group. When defined as +nnn this is used as the number of milliseconds from the previous log event). | %t %m                                                        | Y        |
| REPEAT           | How many times the data set should be iterated over          | 1                                                            | N        |
| TARGETFILE       | This is the name of the file to be written to.               | test.log                                                     | N        |
| TARGETFORMAT     | This describes which values are written and where they get written to using the same %<character> notation used in the SOURCEFORMAT. See below for more details on the characters codes available. | A JSON output could be described with:{"message": "%m"} for example | Y        |
| TARGETDTG        | Describes the formatting of the date time group to be used. This aligns to standard Java notation | yyyy/MM/dd HH:mm:ss                                          | N        |
| DEFAULT-PROPCESS | Some applications like to also record a thread identifier. This defines the string to be used when this is required | Thread-1                                                     | N        |
| DEFAULT-LOCATION | Java applications and some other logging solutions record not just the message but also a class path or similar detail. This provides a default value to use in such a use case. |                                                              |          |
| DEFAULT-LOGLEVEL | Some log formats require a log level - this defines a default log level to be recorded |                                                              | N        |
| ACCELERATEBY     | Integer that can be specified to define a rate of acceleration of the output. So if the log derived velocity is once per second, then setting the ACCELERATEBY value to  2 would produce output at 1/2 second.  If a value is not set then the log derived velocity is used. | 2                                                            | N        |
| VERBOSE          | Sets the tool to be verbose or not, in verbose mode it will write to console what the utility is doing.  Accepted values are true \|  false.  If the property is not set the the value is treated as false. |                                                              | N        |



## Character Codes Available

| Purpose                | Character Code | Description                                                  | Input / Output |
| ---------------------- | -------------- | ------------------------------------------------------------ | -------------- |
| Time stamp             | %t             | This can be expressed as a datetime string, or when prefixed with + a number of milliseconds offset from the previous message. Note when a fulltime stamp is provided it can be used to determine the equivelent time delay between events when replaying for now (essential for using the payload to loop) | I/O            |
| Message                | %m             | The actual log message part of the payload                   | I/O            |
| Log Level              | %l             | When simulating logs - we often want to include the relevant log level label | I/O            |
| Process                | %p             | Sometimes logs will include thread identifiers - this allows us to define those threads or processes in the log | O              |
| Location               | %c             | This is typically the class path (aka location in code) that is incorporated into the log files | I/O            |
| loop/iteration counter | %i             | This provides the ability to include into the output the count of which loop through the data set that is occuring | O              |
| record in loop         | %j             | This can be used to include the index of the line in the source file into the output | O              |

When consuming a log file - it is necessary for there different elements to have a consistent separator.  The separator to use can be defined in the properties file.

## Output Types

The following table describes the output type options available:

| Output Type configuration value                | Description                                                               |
| ---------------------------------------------- | ------------------------------------------------------------------------- |
| file | Generates a file based on the other attributes |
| HTTP | Will make an HTTP POST to the configure URL  |
| TCP | Opens a socket and sends TCP traffic |
| JUL | Java Logging with the Java Native logging mechanism which can be controlled through the logging configuration options|
| STDOUT | Sends to Standard Out (console unless overridden by configurations) |
| ERROUT | Sends to Standard Error (console unless overridden by configurations) |

