import com.oracle.bmc.ons.responses.PublishMessageResponse;
import com.oracle.bmc.ons.NotificationControlPlaneClient;
import com.oracle.bmc.ons.requests.PublishMessageRequest;
import com.oracle.bmc.ons.model.MessageDetails;
import com.oracle.bmc.ons.requests.GetTopicRequest;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.ons.NotificationDataPlaneClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.Arrays;

/*
 * Implements the mechanism to communicate with OCI Logging
 * underlying documentation on how this can be done is detailed at:
 * https://github.com/oracle/oci-java-sdk/blob/master/bmc-examples/src/main/java/NotificationExample.java
 *
 * Controls from this are loaded from the provided properties file
 * connection details to OCI come from a separate properties file
 */
class CustomOCINotificationsOutputter implements LogGenerator.RecordLogEvent
{

  private static final String AGENTTYPE="LOGSIM";
  private static final String AGENTNAME = "AGENTNAME";
  private static final String BATCHSIZE = "BATCHSIZE";
  private static final String CHANNELTOPICOCID = "CHANNELTOPICOCID";
  private static final String REGION = "REGION";
  private static final String OCICONFIGFILE = "OCICONFIGFILE";
  private static final String TOPICID = "TOPICOCID";
  private static final String PROPFILEGROUP = "PROPFILEGROUP";


  private NotificationDataPlaneClient client = null;
  private String agentName = "OCINotificationGenerator";
  private String notificationTopicId = null;
  private String region = null;
  private String compartmentId = null;

  private boolean verbose = false
  private int batchSize = 0;
  private ArrayList batch = null;
  private String profileGroup = "DEFAULT";

  private log (String msg, boolean out=this.verbose)
  {
    if (out) {System.out.println (msg);}
  }


  /*
  * prepare activity by getting the control parameters ready
  * create client object ready to use
  */
public initialize (Properties props, boolean verbose)    
{
  this.verbose = verbose;
  log("initializing OCI Notifications Outputter ....");
  ConfigFileReader.ConfigFile configFile = null;

  String name = props.get(AGENTNAME);
  if ((name != null) && (name.trim().length() > 0))
  {
    agentName = name;
    log ("log id:"+agentName);
  }

  String prfileGrp = props.get(PROPFILEGROUP);
  if ((prfileGrp != null) && (prfileGrp.trim().length() > 0))
  {
    profileGroup = prfileGrp;
    log ("profile group id:"+prfileGrp);
  }

  batch = new ArrayList();
  String batchSze = props.get(BATCHSIZE);
  if ((batchSze != null) && (batchSze.trim().length() > 0 ))
  {
    try
    {
      batchSize = Integer.valueOf(batchSze);
    }
    catch (NumberFormatException numErr)
    {
      log ("Failed to translate batch size default to no batch");
    }
  }

  region = props.get(REGION);
  if ((region == null) || (region.trim().length() < 1))
  {
    log ("Region not configured")
  }
  else
  {
    region = region.trim();
  }

  String OCIconfigLocation = props.get(OCICONFIGFILE);
  if ((OCIconfigLocation == null) || (OCIconfigLocation.trim().length() == 0))
  {
    log("Using default config for OCI properties - " + ConfigFileReader.DEFAULT_FILE_PATH);
    configFile = ConfigFileReader.parseDefault();
  }
  else
  {
    OCIconfigLocation = OCIconfigLocation.trim();
    configFile = ConfigFileReader.parse(OCIconfigLocation, profileGroup);
  }
  final AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFile);

  notificationTopicId = props.get(TOPICID);
  NotificationControlPlaneClient controlPlaneClient =  new NotificationControlPlaneClient(provider);
  if ((notificationTopicId != null) && (notificationTopicId.trim().length() > 0))
  {
    log ("connecting to topic - " + notificationTopicId);
    client = new NotificationDataPlaneClient(provider);
    GetTopicRequest topicRequest =
                GetTopicRequest.builder().topicId(notificationTopicId).build();
    client.setRegion('us-ashburn-1');
  }
  else
  {
    log ("No topic set");
  }

  log ("... initialized OCI Notifications Outputter");
}

  private String batchOutput (ArrayList batch)
  {
    String result = "";
    Iterator iter = batch.iterator();

    while (iter.hasNext())
    {
      result += iter.next().toString() + "\n";
    }
    return result;
  }

  /*
  * Take the provided log entry and either add it to the batch if a batch size is set 
  * or send it immediately to OCI
  */
  public writeLogEntry(String entry)
  {
    batch.add(entry);

    if ((batchSize == 0) || (batchSize == batch.size()))
    {
          log ("tested batchsize");

      String body = batchOutput(batch);
          log ("message body=" + body);

      MessageDetails messageDetails = MessageDetails.builder().body(body).build();

    log ("message built");


      PublishMessageRequest publishMessageRequest =
              PublishMessageRequest.builder()
                      .topicId(notificationTopicId)
                      .messageDetails(messageDetails)
                      .build();

    log ("request built");
        

      PublishMessageResponse response = client.publishMessage(publishMessageRequest);
      log ("OCI Notifications Outputter:" + response.toString());
    }
    batch.clear();
      
  }


  public clearDown()
  {
    if (batch.length() > 0)
    {
      MessageDetails messageDetails = MessageDetails.builder().body(batchOutput(batch)).build();
      PublishMessageRequest publishMessageRequest =
              PublishMessageRequest.builder()
                      .topicId(notificationTopicId)
                      .messageDetails(messageDetails)
                      .build();
        
      PublishMessageResponse respone = client.publishMessage(publishMessageRequest);
      
      batch.clear();
      
      if (verbose) {System.out.println ("OCI Notifications Outputter:" + response.toString());}      
    }
    log ("OCI Notifications Outputter - clearing down")
  }
}