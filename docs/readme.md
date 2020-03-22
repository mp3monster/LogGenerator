The tool is executed with the command: *groovy logSourceSimulator.groovy <properties file>* for example: groovy *logSourceSimulator.groovy testConfigurations\\tool.properties*

The properties file drives all the different possible behaviours.. The following table describes each of the properties and when they are needed.



| Property         | Description                                                  | Example                                                      | Required |
| ---------------- | ------------------------------------------------------------ | ------------------------------------------------------------ | -------- |
| OUTPUTTYPE       | Identifies the type of output to use for the log content. The supported values are: console, file, and HTTP | HTTP                                                         | Y        |
| SOURCE           | The file which will be used as the source for the log events | .\source.txt                                                 | Y        |
| SOUECEFORMAT     | This describes how the source file will be structured. Each of the parts needed are denoted using the pattern of %<character> where the character will represent the element type.  The values supported are:  %t - time (which can be expressed as +nnn or as a date time group. When defined as +nnn this is used as the number of milliseconds from the previous log event). | %t %m                                                        |          |
| TARGETFILE       | This is the name of the file to be written to.               | test.log                                                     | N        |
| TARGETFORMAT     | This describes which values are written and where they get written to using the same %<character> notation used in the SOURCEFORMAT | A JSON output could be described with:{"message": "%m"} for example | Y        |
| TARGETDTG        | Describes the formatting of the date time group to be used. This aligns to standard Java notation | yyyy/MM/dd HH:mm:ss                                          | N        |
| DEFAULT-PROPCESS | Some applications like to also record a thread identifier. This defines the string to be used when this is required | Thread-1                                                     |          |
| DEFAULT-LOCATION | Java applications and some other logging solutions record not just the message but also a class path or similar detail. This provides a default value to use in such a use case. |                                                              |          |
| DEFAULT-LOGLEVEL | Some log formats require a log level - this defines a default log level to be recorded |                                                              |          |
| VERBOSE          | Sets the tool to be verbose or not, in verbose mode it will write to console what the utility is doing.  Accepted values are true \|  false.  If the property is not set the the value is treated as false. |                                                              |          |





