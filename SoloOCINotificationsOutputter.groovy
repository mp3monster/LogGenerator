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
import java.util.Iterator;
import java.util.HashMap;
import java.util.Date;
import java.util.Arrays;
import java.util.Properties;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;

/*
 * Implements the mechanism to communicate with OCI Logging
 * underlying documentation on how this can be done is detailed at:
 * https://github.com/oracle/oci-java-sdk/blob/master/bmc-examples/src/main/java/NotificationExample.java
 *
 */
class SoloOCINotificationsOutputter 
{

  private static final String BATCHSIZE = "BATCHSIZE";
  private static final String CHANNELTOPICOCID = "CHANNELTOPICOCID";
  private static final String REGION = "REGION";
  private static final String OCICONFIGFILE = "OCICONFIGFILE";
  private static final String TOPICID = "TOPICOCID";
  private static final String PROPFILEGROUP = "PROPFILEGROUP";


  private NotificationDataPlaneClient client = null;
  private String notificationTopicId = null;
  private String region = null;
  private String compartmentId = null;

  private boolean verbose = false;
  private int batchSize = 0;
  private ArrayList batch = null;
  private String profileGroup = "DEFAULT";

  private void log (String msg)
  {
    if (verbose) {System.out.println (msg);}
  }


  /*
  * prepare activity by getting the control parameters ready
  * create client object ready to use
  */
  public void initialize (Properties props, boolean verbose)    
  {
    this.verbose = verbose;
    log("initializing OCI Notifications Outputter ....");
    ConfigFileReader.ConfigFile configFile = null;

    String prfileGrp = props.getProperty(PROPFILEGROUP);
    if ((prfileGrp != null) && (prfileGrp.trim().length() > 0))
    {
      profileGroup = prfileGrp;
      log ("profile group id:"+prfileGrp);
    }

    batch = new ArrayList();
    String batchSze = props.getProperty(BATCHSIZE);
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

    region = props.getProperty(REGION);
    if ((region == null) || (region.trim().length() < 1))
    {
      log ("Region not configured");
    }
    else
    {
      region = region.trim();
    }

    String OCIconfigLocation = props.getProperty(OCICONFIGFILE);
    try
    {
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
    }
    catch (Exception err)
    {
        log ("Error loading the config file" + err.toString());
    }

    final AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFile);

    notificationTopicId = props.getProperty(TOPICID);
    NotificationControlPlaneClient controlPlaneClient =  new NotificationControlPlaneClient(provider);
    if ((notificationTopicId != null) && (notificationTopicId.trim().length() > 0))
    {
      log ("connecting to topic - " + notificationTopicId);
      client = new NotificationDataPlaneClient(provider);
      GetTopicRequest topicRequest =
                  GetTopicRequest.builder().topicId(notificationTopicId).build();
      client.setRegion(region);
    }
    else
    {
      log ("No topic set");
    }

    log ("... initialized OCI Notifications Outputter");
  }

 /*
  * Builds a compound output
  */
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
  public void writeLogEntry(String entry)
  {
    batch.add(entry);

    if ((batchSize == 0) || (batchSize == batch.size()))
    {
          log ("tested batchsize");

      String body = batchOutput(batch);
          log ("message body=" + body);

      MessageDetails messageDetails = new MessageDetails.Builder().body(body).build();

      PublishMessageRequest publishMessageRequest =
              new PublishMessageRequest.Builder()
                      .topicId(notificationTopicId)
                      .messageDetails(messageDetails)
                      .build();        

      PublishMessageResponse response = client.publishMessage(publishMessageRequest);
      log ("OCI Notifications Outputter:" + response.toString());
    }
    batch.clear();
      
  }


  /*
   * This will send the remaining events still being held and release any resources necessary
   */
  public void clearDown()
  {
    if (batch.size() > 0)
    {
      MessageDetails messageDetails = MessageDetails.builder().body(batchOutput(batch)).build();
      PublishMessageRequest publishMessageRequest =
              PublishMessageRequest.builder()
                      .topicId(notificationTopicId)
                      .messageDetails(messageDetails)
                      .build();
        
      PublishMessageResponse response = client.publishMessage(publishMessageRequest);
      
      batch.clear();
      
      log("OCI Notifications Outputter:" + response.toString());      
    }
    log ("OCI Notifications Outputter - clearing down");
  }


  /*
  * Simple main function that drives the class to generate a simple log message
  * To use this approach the environment needs the OCI Config file for the SDK
  * Plus the following environment variables (shell set or export):
  * TOPICOCID=ocid1.onstopic.oc1.iad.aaaaaaaatbbbbbbbbbbbbbbbbbbbqcccccccccccccccdddddddddddddddd
  * OCICONFIGFILE=oci.properties
  * REGION=us-ashburn-1
  * CLASSPATH=./lib/*
  *
  * TOPCOCID is the OCID for the notification topic created
  * REGION is the name of the Region in which the Topic has been defined
  * CLASSPATH points to where the SDK lib folder is
  *
  * to run independently of the core LogGenerator features the line implements LogGenerator.RecordLogEvent needs to be commented out
  * the command can then be run as 
  */
  public static void main (String[] args)
  {
    SoloOCINotificationsOutputter notificationsOut = new SoloOCINotificationsOutputter();
    String dtgFormat = "HH:mm:ss";
    Properties props = new Properties();
    // to avoid needing the base classes we grab environment variables and set the properties that way
    props.setProperty (BATCHSIZE, "1");
    props.setProperty (TOPICID, System.getenv(TOPICID));
    props.setProperty (REGION, System.getenv(REGION));
    props.setProperty (OCICONFIGFILE, System.getenv(OCICONFIGFILE));
    boolean useJSONformat = (System.getenv("JSONFmt") != null);

    notificationsOut.initialize (props, ((System.getenv("verbose") == null) || (System.getenv("verbose").trim().equalsIgnoreCase("true"))));

    int index = 0;
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dtgFormat);  
    LocalDateTime now = null;
    try{
      while (true)
      {
        now = LocalDateTime.now();

        if (useJSONformat)
        {
          //format for sending in JSON format
          notificationsOut.writeLogEntry("{\"MessageNo\" : " + index + ", \"at\": " + "\""+ now.format(dtf) + "\", \"sent\" : \"from client app\"}");        
        }
        else
        {
          // format for a human readable message:
          notificationsOut.writeLogEntry("Message " + index + " at " + now.format(dtf) + " sent from client app");
        }
        Thread.sleep (5000);
        index++;
      }
    }
    catch (Exception err)
    {
      System.out.println ("loop disturbed " + err.getMessage());
       err.printStackTrace();

    }

    // if the loop becomes conditional then the following needs to be uncommented
    //notificationsOut.clearDown();

  }
}
