/*
 * Implements the mechanism to communicate with OCI Logging
 * underlying documentation on how this can be done is detailed at:
 * https://github.com/oracle/oci-java-sdk/blob/master/bmc-examples/src/main/java/NotificationExample.java
 *
 * Controls from this are loaded from the provided properties file
 * connection details to OCI come from a separate properties file
 */
class CustomOCINotificationsOutputter extends SoloOCINotificationsOutputter implements LogGenerator.RecordLogEvent
{

}
