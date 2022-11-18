# Custom OCI Outputter

This is an optional addition to the LogSimulator. It provides the means to send logs to OCI Logging for processing. As it uses the CUSTOM framework, it is not picked up and used unless the configuration for the main log simulator points to it.

## Additional Configuration Attributes

| Variable Name      | Variable Description                                         | Value                                 |
| ------------------ | ------------------------------------------------------------ | ------------------------------------- |
| QUEUENAME          | The display name to use with the Queue when created. Creation only happens if no QueueOCID is provided.  When running as a consumer, if this is provided without the OCID, then the 1st occurrence of a queue with this name will be used. | myTest                                |
| QUEUEOCID          | The OCID for an existing Queue to be used. In the send mode, if this is provided, then the queue isn't created. We simply connect to this queue. | ocid1.queue.oc1.iad.aaaa.....bbbbb    |
| OCICONFIGFILE      | The location of the properties file that can be used by the SDK to authenticate with OCI. | oci.properties                        |
| PROPFILEGROUP                   | This tells OCI SDK which profile group to take from the configuration file. If unset then the [DEFAULT] group is used. | DEFAULT |
| QUEUECOMPARTMENTID | The OCID for the compartment in which the queue operates.    | ocid1.queue.oc1.iad.aaaa.....bbbbb    |
| REGION             | The name of the region being used for the querues. Needs to be set to ensure we talk to the correct region | us-ashburn-1 |
| VERBOSE            | This controls how much information is displayed on the console - if you're using the application to help understand how things work, then we recommend having this set to true. any other value will switch off the console logging. | true                                  |
| JSONFMT            | Tells the application to generate its messages using a JSON format. If not set to true, then the message is simply plaintext | true                                  |
| MAXGETS                    | The number of times the application will loop through and try to consume messages. If not set, then the loop will run infinitely | 10 |
|POLLDURATIONSECS | The queue read can operate with long polling to retrieve messages. This value defines how long the poll session can wait for in seconds before returning. If unset then the API will call retrieve what is immediately available, and return. | 10 |
|INTERREADELAYSECS | This imposes a delay between message read API calls. If unset, then the read logic will immediately poll OCI Queue again. | 5 |
|DELETEDURATIONSECS | To demonstrate the ability to change the visibility control of a message. This value, when set, will be used for messages being read. If set, the reading process will also pause this long before invoking the deletion command as well. If not set, then the visibility setting is not amended on a message. | 20 |
|DLQCOUNT | Provides a value for the queue creation for the Dead Letter queue size. If not set, then no DLQ will be set. | 100 |
|RETENTIONSECONDS | The time in seconds that will be used to tell OCI Queue how long a message should be retained before deleting the message. A default value is applied if this isn't set | 2400 |
| BATCHSIZE                       | This sets the number of events to send to OCI at a time. If unset, then each log event is individually sent to OCI | 1|


## OCI Dependencies

For this plugin we need several additional pieces to be configured and deployed. These are:

*  Java SDK for OCI
* OCI Properties file configured

### SDK

The OCI Java SDK can be downloaded from here. The provided/example script assumes that there is a child folder called *lib* that will contain the OCI JAR (e.g. oci-java-sdk-full-2.46.0) and the contents of the *third-party/lib* folder in the download bundle, which includes all the SDK's dependencies such as SLF4J.  All of these resources can be downloaded via [here](https://docs.oracle.com/en-us/iaas/Content/API/SDKDocs/javasdk.htm) or from [here](https://github.com/oracle/oci-java-sdk/releases).



### Properties File

The SDK looks to load a properties file to get the necessary credentials to access OCI. This includes a suitable certificate, fingerprint, and user OCID. This should look something like this:



> [DEFAULT]
>
> user=ocid1.user.oc1..aaaabbbbccccddddeeeeffff11112222233334444555566667777ababxxx
>
> fingerprint=11:22:33:44:aa:bb:cc:dd:ff:34:32:21:44:ee:2a:3b
>
> tenancy=ocid1.tenancy.oc1..aaaabbbbccccddddeeeeffff11112222233334444555566667777ababxxx
>
> region=us-ashburn-1
>
> key_file=/home/my.pem

The process to retrieve these values is described in [Oracle docs here](https://docs.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm).

In our setup, these values are saved as *oci.properties*. 

### OCI Logging Setup

The setup of OCI logging is detailed in the Oracle documentation [here](https://docs.oracle.com/en-us/iaas/Content/Logging/Concepts/custom_logs.htm#creating_custom_logs). Note there is no need for an agent in the configuration. Once the Log is created, the OCID is required in our configuration, so the correct Log is written into. Remember that the correct policies need to be established to allow our application to write to the log.

