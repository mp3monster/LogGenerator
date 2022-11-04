
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.QueueAdminClient;
import com.oracle.bmc.queue.requests.CreateQueueRequest;
import com.oracle.bmc.queue.model.CreateQueueDetails;
import com.oracle.bmc.queue.model.PutMessagesDetails;
import com.oracle.bmc.queue.requests.PutMessagesRequest;
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry;
import com.oracle.bmc.queue.requests.ListQueuesRequest;
import com.oracle.bmc.queue.model.QueueSummary;
import com.oracle.bmc.queue.responses.CreateQueueResponse;
import com.oracle.bmc.queue.responses.PutMessagesResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.Arrays;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;
import java.util.Properties;


/*
 * Implements the mechanism to communicate with OCI Logging
 * underlying documentation on how this can be done is detailed at:
 * https://docs.oracle.com/en-us/iaas/api/#/en/logging-dataplane/20200831/LogEntry/PutLogs
 *
 * Controls from this are loaded from the provided properties file
 * connection details to OCI come from a separate properties file
 */
class CustomOCIQueueOutputter //implements LogGenerator.RecordLogEvent
{

  private static final String AGENTTYPE="LOGSIM";
  private static final String AGENTNAME = "AGENTNAME";
  private static final String BATCHSIZE = "BATCHSIZE";
  private static final String QUEUEOCID = "QUEUEOCID";
  private static final String QUEUENAME = "QUEUENAME";
  private static final String OCICONFIGFILE = "OCICONFIGFILE";
  private static final String PROPFILEGROUP = "PROPFILEGROUP";
  private static final String REGION = "REGION";
  private static final String QUEUECOMPARTMENTID = "QUEUECOMPARTMENTID";


  private QueueClient client = null;
  private String agentName = "LogSimulator";
  private String queueName = "";
  private String queueId = null;
  private String compartmentId = null;
  private Integer retentionInSeconds = new Integer (600);
  private Integer visibilityInSeconds = new Integer (10);
  private Integer timeoutInSeconds = new Integer (120);
  private Integer deadLetterQueueDeliveryCount = new Integer (10);
  private String customEncryptionKeyId = null;

  private static boolean verbose = false;
  private int batchSize = 0;
  private List<PutMessagesDetailsEntry> batch = null;
  private String profileGroup = "DEFAULT";

  private static log (String msg)
  {
    if (verbose) {System.out.println (msg);}
  }

  static String getQueueOCIDFor (String queueName, String compartmentId, QueueAdminClient adminClient)
  {
    for (int attempt=0; attempt < 10; attempt++)
    {
      log ("get queue OCID attempt "+ attempt)
      ListQueuesRequest listRequest = new ListQueuesRequest.Builder().compartmentId(compartmentId).displayName(queueName).build();
      List queueList = adminClient.listQueues(listRequest).getQueueCollection().getItems();
      
      Iterator iter = queueList.iterator();
      QueueSummary queue = null;
      String queueOCID = null;

      while (iter.hasNext() && queueOCID == null)
      {
        queue = iter.next();
        queueOCID = queue.getId();
      }
      if (queueOCID != null)
      {
        log (queue.getDisplayName() + " -- " + queue.getId());
        return queueOCID;
      }

      sleep (10000);

    }

    log ("didnt find queue");
    return null;
  }

  static void createQueue (String queueName, String compartmentId, QueueAdminClient adminClient, Integer deadLetterQueueDeliveryCount, Integer retentionInSeconds)
  {
    log ("Creating Queue with name " + queueName);

    CreateQueueDetails details = new CreateQueueDetails.Builder().compartmentId(compartmentId).
          deadLetterQueueDeliveryCount(deadLetterQueueDeliveryCount).
          displayName(queueName).
          retentionInSeconds(retentionInSeconds).
          build();
    CreateQueueRequest createRequest = new CreateQueueRequest.Builder().createQueueDetails(details).build();
    CreateQueueResponse response = adminClient.createQueue(createRequest);
    log ("queue requested - " + response.toString());

  }

  /*
  * prepare activity by getting the control parameters ready
  * create client object ready to use
  */
  public void initialize (Properties props, boolean verbose)    
  {
    this.verbose = verbose;
    log("initializing OCI Queue Outputter ....");
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

    String batchSze = props.get(BATCHSIZE);
    if ((batchSze != null) && (batchSze.trim().length() > 0 ))
    {
      try
      {
        batchSize = Integer.valueOf(batchSze);
        batch = new ArrayList<PutMessagesDetailsEntry>();
      }
      catch (NumberFormatException numErr)
      {
        log ("Failed to translate batch size default to no batch");
      }
    }

    compartmentId = props.get(QUEUECOMPARTMENTID);

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

    client =new QueueClient.Builder().build(provider);
    log ("initialized OCI Queue client ");

    String queueName = props.get(QUEUENAME);
    queueId = props.get(QUEUEOCID);
    log (queueId + "|" + queueName);
    if ((queueId == null) || (queueId.trim().length()==0))
    {
      if ((queueName != null) && (queueName.trim().length() > 0))
      {
        queueName = queueName.trim();
        QueueAdminClient adminClient = new QueueAdminClient.Builder().build(provider);
        createQueue(queueName, compartmentId, adminClient, deadLetterQueueDeliveryCount, retentionInSeconds);
        queueId = getQueueOCIDFor(queueName, compartmentId, adminClient);
      } 
    }  

    log ("... initialized OCI Queue Outputter");
  }


  /*
  * Take the provided log entry and either add it to the batch if a batch size is set 
  * or send it immediately to OCI
  */
  public void writeLogEntry(String entry)
  {
    log ("added message to batch");
    batch.add(new PutMessagesDetailsEntry.Builder().content(entry).build());
    
    if ((batchSize == 0) || (batchSize == batch.size()))
    {
      log ("time to send the batch");
      PutMessagesDetails msgDetails = new PutMessagesDetails.Builder().messages(batch).build();
      log ("msgDetails created");
      PutMessagesRequest request = new PutMessagesRequest.Builder().queueId(queueId).putMessagesDetails(msgDetails).build();

      /* Send request to the Client */
      PutMessagesResponse response = client.putMessages(request);
      batch.clear();
      
      log ("OCI Queue Outputter:" + response.toString());
    }
  }


  public void clearDown()
  {
    if (batch.length() > 0)
    {
      PutMessagesDetails msgDetails = PutMessagesDetails.Builder().messages(batch).build();
      PutMessagesRequest request = PutMessagesRequest.builder().queueId(queueId).PutMessagesDetails(msgDetails);

      /* Send request to the Client */
      PutMessagesResponse response = client.putMessages(request);
      batch.clear();
      
      if (verbose) {System.out.println ("OCI Queue Outputter:" + response.toString());}      
    }
    log ("OCI Queue Outputter - clearing down");
  }


  /*
  * Simple main function that drives the class to generate a simple log message
  * To use this approach the environment needs the OCI Config file for the SDK
  * Plus the following environment variables (shell set or export):
  * QUEUEOCID=ocid1.queue.oc1.iad.aaaaaaaatbbbbbbbbbbbbbbbbbbbqcccccccccccccccdddddddddddddddd
  * OCICONFIGFILE=oci.properties
  * REGION=us-ashburn-1
  * CLASSPATH=./lib/*
  *
  * QUEUEOCID is the OCID for the notification queue created
  * REGION is the name of the Region in which the queue has been defined
  * CLASSPATH points to where the SDK lib folder is
  *
  * to run independently of the core LogGenerator features the line implements LogGenerator.RecordLogEvent needs to be commented out
  * the command can then be run as 
  */
  public static void main (String[] args)
  {
    CustomOCIQueueOutputter queueOut = new CustomOCIQueueOutputter();
    String dtgFormat = "HH:mm:ss";
    Properties props = new Properties();
    // to avoid needing the base classes we grab environment variables and set the properties that way
    props.setProperty (BATCHSIZE, "1");
    props.setProperty (QUEUEOCID, System.getenv(QUEUEOCID));
    props.setProperty (REGION, System.getenv(REGION));
    props.setProperty (QUEUENAME, System.getenv(QUEUENAME));
    props.setProperty (OCICONFIGFILE, System.getenv(OCICONFIGFILE));
    boolean useJSONformat = (System.getenv("JSONFmt") != null);
    props.setProperty (QUEUECOMPARTMENTID, System.getenv(QUEUECOMPARTMENTID));

    queueOut.initialize (props, ((System.getenv("verbose") == null) || (System.getenv("verbose").trim().equalsIgnoreCase("true"))));

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
          queueOut.writeLogEntry("{\"MessageNo\" : " + index + ", \"at\": " + "\""+ now.format(dtf) + "\", \"sent\" : \"from client app\"}");        
        }
        else
        {
          // format for a human readable message:
          queueOut.writeLogEntry("Message " + index + " at " + now.format(dtf) + " sent from client app");
        }
        Thread.sleep (5000);
        index++;
      }
    }
    catch (Exception err)
    {
      System.out.println ("loop disturbed " + err.toString() + "\n" + err.getStackTrace().toString());
    }

    // if the loop becomes conditional then the following needs to be uncommented
    //queueOut.clearDown();

  }  
}