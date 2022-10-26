# Custom OCI Notification Outputter

This is an optional addition to the LogSimulator. It provides the means to send logs to OCI Notifications for processing. As it uses the CUSTOM framework, it is not picked up and used unless the configuration for the main log simulator points to it.

## Additional Configuration Attributes

| Output Type configuration value | Description                                                  |
| ------------------------------- | ------------------------------------------------------------ |
| TOPICOCID                       | The OCID for the Notifications topic that has been configured.               |
| REGION                          | This is the name of the OCI Region that the Topic has been configure in e.g. us-ashburn-1 |
| BATCHSIZE                       | This sets the number of events to send to OCI at a time. If unset, then each log event is individually sent to OCI |
| OCICONFIGFILE                   | This tells the custom outputter where to find the configuration file that contains the credentials to access OCI. |
| PROPFILEGROUP                   | This tells OCI SDK which profile group to take from the configuration file. If unset then the [DEFAULT] group is used. |

#### Example Properties settings:
> OUTPUTTYPE=CUSTOM
> CUSTOMOUTPUT=CustomOCINotificationsOutputter
> #BATCHLOGS=5
> TOPICOCID=ocid1.onstopic.oc1.iad.aaaaaaaatbbbbbbbbbbbbbbbbbbbbccccccccccccccccvsntijpra
> OCICONFIGFILE=oci.properties
> REGION=us-ashburn-1
## OCI Dependencies

For this plugin we need several additional pieces to be configured and deployed. These are:

* Java SDK for OCI
* OCI Properties file configured
* OCI Log configured

### SDK

The OCI Java SDK can be downloaded from here. The provided/example script assumes that there is a child folder called *lib* that will contain the OCI JAR (e.g. oci-java-sdk-full-2.46.0) and the contents of the *third-party/lib* folder in the download bundle, which includes all the SDK's dependencies such as SLF4J.  All of these resources can be downloaded via [here](https://docs.oracle.com/en-us/iaas/Content/API/SDKDocs/javasdk.htm) or from [here](https://github.com/oracle/oci-java-sdk/releases).

### Properties File

The SDK looks to load a properties file to get the necessary credentials to access OCI. This includes a suitable certificate, fingerprint, and user OCID. This should look something like this:

> [DEFAULT]
>
> user=ocid1.user.oc1..aaaaaaaajgyiqcmpi4dezdfjk;ghdfjbfhjgbhfjghfjghk5u2d2gwf2bpsenslwpfwq
>
> fingerprint=15:26:37:48:aa:bb:cc:dd:ff:34:32:21:44:ee:2a:3b
>
> tenancy=ocid1.tenancy.oc1..aaaaaaaajznesdf;klghjfdgjkhfgkdfhlgaerpo8ibfxoyd4q
>
> region=us-ashburn-1
>
> key_file=/home/my.pem

The process to retrieve these values is described in [Oracle docs here](https://docs.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm).

In our setup, these values are saved as *oci.properties*.

### OCI Notifications Setup

The setup of OCI Notifications is detailed in the Oracle documentation [here](https://docs.oracle.com/en-us/iaas/Content/Notification/home.htm). Once a Topic has been defined in OCI applications can submit messages to it. For the messages to go anywhere and not be deleted because the delivery retries have timed out a consumer needs to be configured. Consumers can range from human consumption end points e.g. Slack, Email etc. to other applications as OCI Functions or applications that have subscribed to a topic.
