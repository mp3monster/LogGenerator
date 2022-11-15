
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;

import com.oracle.bmc.queue.model.Queue;
import com.oracle.bmc.queue.QueueClient;
import com.oracle.bmc.queue.QueueAdminClient;

import com.oracle.bmc.queue.requests.CreateQueueRequest;
import com.oracle.bmc.queue.model.CreateQueueDetails;
import com.oracle.bmc.queue.responses.CreateQueueResponse;

import com.oracle.bmc.queue.model.PutMessagesDetails;
import com.oracle.bmc.queue.requests.PutMessagesRequest;
import com.oracle.bmc.queue.model.PutMessagesDetailsEntry;

import com.oracle.bmc.queue.requests.ListQueuesRequest;
import com.oracle.bmc.queue.model.QueueSummary;
import com.oracle.bmc.queue.responses.PutMessagesResponse;

import com.oracle.bmc.queue.responses.GetQueueResponse;
import com.oracle.bmc.queue.requests.GetQueueRequest;
import com.oracle.bmc.queue.requests.GetStatsRequest;
import com.oracle.bmc.queue.responses.GetStatsResponse;
import com.oracle.bmc.queue.model.Stats;

import com.oracle.bmc.queue.requests.GetWorkRequestRequest;
import com.oracle.bmc.queue.requests.DeleteQueueRequest;

import com.oracle.bmc.queue.model.UpdateMessagesDetails;
import com.oracle.bmc.queue.requests.UpdateMessageRequest;
import com.oracle.bmc.queue.requests.UpdateMessagesRequest;
import com.oracle.bmc.queue.responses.UpdateMessagesResponse;
import com.oracle.bmc.queue.responses.UpdateMessageResponse;
import com.oracle.bmc.queue.model.UpdateMessagesDetailsEntry;
import com.oracle.bmc.queue.model.GetMessage;
import com.oracle.bmc.queue.model.UpdateMessageDetails;
import com.oracle.bmc.queue.requests.GetMessagesRequest;
import com.oracle.bmc.queue.responses.GetWorkRequestResponse;
import com.oracle.bmc.queue.responses.DeleteQueueResponse;
import com.oracle.bmc.queue.model.OperationStatus;
import com.oracle.bmc.queue.model.WorkRequestResource;
import com.oracle.bmc.queue.model.WorkRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.Arrays;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.List;
import java.math.BigDecimal;
import java.util.Iterator;

/*
 * Implements the mechanism to communicate with OCI Logging
 * underlying documentation on how this can be done is detailed at:
 * https://docs.oracle.com/en-us/iaas/api/#/en/logging-dataplane/20200831/LogEntry/PutLogs
 *
 * Controls from this are loaded from the provided properties file
 * connection details to OCI come from a separate properties file
 */
public class SoloOCIQueueOutputter 
{
  private static final String BATCHSIZE = "BATCHSIZE";
  private static final String QUEUEOCID = "QUEUEOCID";
  private static final String QUEUENAME = "QUEUENAME";
  private static final String OCICONFIGFILE = "OCICONFIGFILE";
  private static final String PROPFILEGROUP = "PROPFILEGROUP";
  private static final String REGION = "REGION";
  private static final String QUEUECOMPARTMENTID = "QUEUECOMPARTMENTID";
  private static final String ISVERBOSE = "VERBOSE";
  private static final String POLLDURATIONSECS = "POLLDURATIONSECS";
  private static final String INTERREADELAYSECS = "INTERREADELAYSECS";
  private static final String DELETEDURATIONSECS = "DELETEDURATIONSECS";
  private static final String MAXGETS = "MAXGETS";
  private static final String JSONFMT = "JSONFMT";

  private static final String ACTION_SEND = "send";
  private static final String ACTION_LIST = "list";
  private static final String ACTION_CONSUME = "consume";
  private static final String ACTION_DELETE = "delete";
  private static final String ACTION_DELETE_OCID = "delete-ocid";


  private QueueClient client = null;

  private String queueName = "";
  private String queueId = null;
  private String compartmentId = null;
  private Integer retentionInSeconds = new Integer (600);
  private Integer visibilityInSeconds = new Integer (10);
  private Integer timeoutInSeconds = new Integer (120);
  private Integer deadLetterQueueDeliveryCount = new Integer (10);
  private String customEncryptionKeyId = null;
  private String regionName = null;

  private static boolean verbose = false;
  private int batchSize = 0;
  private List<PutMessagesDetailsEntry> batch = null;
  private String profileGroup = "DEFAULT";
  static final String DTG_FORMAT = "HH:mm:ss";


  /*
   * To use a proper logging framework replace the calls to this method or direct the calls within this method to a logging framework
   * This doesn't use a logging framework to minize the dependencies needing to be retrieved - making it easy to deploy and use as
   * a single file application.
   */
  private static void log (String msg)
  {
    if (verbose) {System.out.println (msg);}
  }

  /*
   * Using thew queue OCID (Id) we can provide information about the queue such as its depth, average message size etc
   */
  static void logQueueStatsFor (String queueId, QueueClient client)
  {
    GetStatsRequest request = GetStatsRequest.builder().queueId(queueId).build();
    GetStatsResponse response = client.getStats(request);
    Stats stats = response.getQueueStats().getQueue();

    String size = "0";
    Long sizeBytes = stats.getSizeInBytes();
    if (sizeBytes != 0)
    {
      size = new BigDecimal(sizeBytes/1024).toString();
    }

    log ("-------------");
    log ("Queue id : " + queueId);
    log ("Visible messages : " + stats.getVisibleMessages().toString());
    log ("InFlight messages : " + stats.getInFlightMessages().toString());
    log ("Queue size (MB) : " + size);
    log ("-------------");
    log ("stats response done");

  }

  /*
   * With a queue OCID we can obtain the configuration about a specific queue
   */
  static void logQueueInfoFor (String queueId, QueueAdminClient adminClient)
  {
    GetQueueRequest request = GetQueueRequest.builder().queueId(queueId).build();
    GetQueueResponse response = adminClient.getQueue(request);
    Queue queue = response.getQueue();

    log ("-------------");
    log("Queue :" + queue.getDisplayName());
    log("Queue Id : " + queue.getId());
    log ("Created :" + queue.getTimeCreated().toString());
    log ("Updated :" + queue.getTimeUpdated().toString());
    log ("Lifecycle state :" + queue.getLifecycleState().toString());
    log ("Lifecycle details :" + queue.getLifecycleDetails());
    log("Compartment Id :" + queue.getCompartmentId());
    log ("Endpoint: " + queue.getMessagesEndpoint());
    log ("retention mins :" + (queue.getRetentionInSeconds() / 60));
    log ("-------------");

     
  }

  /*
   * Get the queue OCID using the queue name. This assumes you're not likely to create the same queue in the same compartment
   * Strictly speaking we should us track the job status. By stipulating no or partial name we can list the queues. The returned OCID
   * is either the first occurence found or the last occurence (of the name found) depending upon the returnFirst
   * As a resault we can use this method to list the compartments queues.
   */
  static String getQueueOCIDFor (String queueName, String compartmentId, QueueAdminClient adminClient, boolean returnFirst)
  {
    for (int attempt=0; attempt < 10; attempt++)
    {
      ListQueuesRequest listRequest = null;
      
      if (queueName != null)
      {
        listRequest = ListQueuesRequest.builder().compartmentId(compartmentId).displayName(queueName).build();
      }
      else
      {
        log ("Getting all queues in compartment");
        listRequest = ListQueuesRequest.builder().compartmentId(compartmentId).build();
      }

      List queueList = adminClient.listQueues(listRequest).getQueueCollection().getItems();
      
      Iterator iter = queueList.iterator();
      QueueSummary queue = null;
      String queueOCID = null;

      log ("Matched " + queueList.size());
      while (iter.hasNext())
      {
        queue = (QueueSummary)iter.next();
        log ("Located queue:" + queue.getDisplayName() + " -- " + queue.getId());
        if ((queueName != null) && queue.getDisplayName().equals(queueName))
        {
          if (queueOCID ==null)
          {
            queueOCID = queue.getId();
            if (returnFirst)
            {
              break;
            }
          }
        }
      }

      if ((queueName == null) || (queueOCID != null))
      {
        return queueOCID;
      }

      pause("get OCID", 10);

      log ("get queue OCID attempt "+ attempt);


    }

    return null;
  }

  /*
   * This deletes the queue. The queue can be iether the queue's name or OCID. If the name is provided then the 1st occurence of the named
   * queue in the compartment is targeted.
   */
  static void deleteQueue (boolean byName, String id, String compartmentId, QueueAdminClient adminClient)
  {
    String ocid = id;
    log ("Deleting " + id);
    if (byName)
    {
      ocid = getQueueOCIDFor (id, compartmentId, adminClient, true);
    }

    
    if (ocid != null)
    {
      DeleteQueueRequest delRequest = DeleteQueueRequest.builder().queueId(ocid).build();
      DeleteQueueResponse delResponse = adminClient.deleteQueue(delRequest);

      // lets find out how the deletion went
      GetWorkRequestRequest workRequest = GetWorkRequestRequest.builder().workRequestId(delResponse.getOpcWorkRequestId()).build();
      GetWorkRequestResponse workResponse = adminClient.getWorkRequest(workRequest);
      WorkRequest workData =  workResponse.getWorkRequest(); 

      log ("Delete request " + id + " result = " + workData.getStatus().toString());
  
      log ("Deleted no resources  = " + workData.getResources().size());
      Iterator iter = workData.getResources().iterator();
      while (iter.hasNext())
      {
        WorkRequestResource requestResource = (WorkRequestResource)iter.next();
      log ("Deleted entity = " + requestResource.getEntityType());
      log ("Deleted OCID  = " + requestResource.getIdentifier());
      }
      log ("Action requested at  = " + workData.getTimeAccepted().toString());
      Date completed = workData.getTimeFinished();
      if (completed != null)
      {
        log ("Action finished at  = " + workData.getTimeFinished().toString());
      }

      log ("Action progress  " + workData.getPercentComplete().toString() + "% complete");


    }
    else{
      log ("Couldn't delete - queue " + id + " OCID " + ocid);
    }
  }

  /*
   * Creates the queue, and then uses the workId to then get the information from the job about the new Queue's OCID
   * this is the correct approach to obtaining the queue OCID which is returned.
   */
  static String createQueue (String queueName, String compartmentId, QueueAdminClient adminClient, Integer deadLetterQueueDeliveryCount, Integer retentionInSeconds)
  {
    String queueOCID = null;
    log ("Creating Queue with name " + queueName);

    CreateQueueDetails details = CreateQueueDetails.builder().compartmentId(compartmentId).
          deadLetterQueueDeliveryCount(deadLetterQueueDeliveryCount).
          displayName(queueName).
          retentionInSeconds(retentionInSeconds).
          build();
    CreateQueueRequest createRequest = CreateQueueRequest.builder().createQueueDetails(details).build();
    CreateQueueResponse createResponse = adminClient.createQueue(createRequest);
    String jobId = createResponse.getOpcWorkRequestId();
    log ("queue requested -  requestid=" + jobId);

    GetWorkRequestRequest workRequestRequest = GetWorkRequestRequest.builder().workRequestId(jobId).build();
    log ("request created");

    while (queueOCID == null)
    {
      GetWorkRequestResponse workResponse = adminClient.getWorkRequest(workRequestRequest);
      log ("work request response");
      WorkRequest workRequestData = workResponse.getWorkRequest();
      log (workRequestData.getStatus().toString());

      if (workRequestData.getStatus() == OperationStatus.Succeeded)
      {
          log ("Queue built");
          queueOCID = workRequestData.getResources().get(0).getIdentifier();
          log ("Work info " + workRequestData.getResources().get(0).toString());          
      }
      else if (workRequestData.getStatus() == OperationStatus.Failed)
      {
        log ("queue build failed");
      }
      else
      {
        log ("queue not ready yet");
        pause("queue not ready", 5);

      } 
    }

    return queueOCID;
  }

  /*
   * Based on the admin client set the appropriate endpoint for the normal queue client. If the admin client hasn't been configured
   * the endpoint is derived from the REGION property.
   */
  static QueueClient setQueueEndpoint (QueueAdminClient adminClient, QueueClient queueClient, String queueOCID, Properties props)
  {
    String endpoint = null;

    if ((adminClient != null) && (queueOCID != null))
    {
      GetQueueResponse getResponse = adminClient.getQueue(GetQueueRequest.builder()
              .queueId(queueOCID)
              .build());
      endpoint = getResponse.getQueue().getMessagesEndpoint();
    }
    else
    {
      log ("setting endpoint from region name");
      endpoint = "https://cell-1.queue.messaging."+props.getProperty(REGION)+".oci.oraclecloud.com";
    }
    queueClient.setEndpoint(endpoint);    

    return queueClient;
  }

  /*
   * This creates an appropriate queueAdminClient - needed for actions relating to creating, deleting queues etc
   */
  static QueueAdminClient getQueueAdminClient (ConfigFileReader.ConfigFile configFile)
  {
    final AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFile);

    QueueAdminClient adminClient = QueueAdminClient.builder().build(provider);

    return adminClient;
  }

  /*
   * This creates the appropriate config file setup based on the parameters provided
   * Any Exceptions as a result of being unable to read  the file are trapped and will result in a null
   * object being returned
   */
  static ConfigFileReader.ConfigFile getConfigFile(Properties props)
  {
    ConfigFileReader.ConfigFile configFile = null;

    String profileGroup = props.getProperty(PROPFILEGROUP);
    if ((profileGroup != null) && (profileGroup.trim().length() > 0))
    {
      profileGroup = profileGroup.trim();
      log ("profile group id:"+profileGroup);
    }

    String OCIconfigLocation = props.getProperty(OCICONFIGFILE);
    if ((OCIconfigLocation == null) || (OCIconfigLocation.trim().length() == 0))
    {
        log("Using default config for OCI properties - " + ConfigFileReader.DEFAULT_FILE_PATH);
        try
        {
        configFile = ConfigFileReader.parseDefault();
        }
        catch (Exception err)
        {
          log ("Error trying to process default config file\n" + err.getMessage());
          err.printStackTrace();
        }
    }
    else
    {
      OCIconfigLocation = OCIconfigLocation.trim();
      try{
        configFile = ConfigFileReader.parse(OCIconfigLocation, profileGroup);
      }
      catch (Exception err)
      {
        log ("Error trying to process config file" +OCIconfigLocation + " \n" + err.getMessage());
        err.printStackTrace();
      }      
    }

    return configFile;
  }

  /*
  * prepare activity by getting the control parameters ready
  * create client object ready to use
  * This method is required by the LogGenerator interface
  */
  public void initialize (Properties props)    
  {
    log("initializing OCI Queue Outputter ....");
    ConfigFileReader.ConfigFile configFile = null;

    batch = new ArrayList<PutMessagesDetailsEntry>();

    batchSize = Integer.parseInt(props.getProperty(BATCHSIZE));
    verbose = Boolean.parseBoolean(props.getProperty(ISVERBOSE, "true"));

    regionName = props.getProperty(REGION);
    compartmentId = props.getProperty(QUEUECOMPARTMENTID);

    configFile = getConfigFile(props);
    AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFile);

    client = QueueClient.builder().build(provider);
    client.setEndpoint("https://cell-1.queue.messaging."+regionName+".oci.oraclecloud.com");
    log ("initialized OCI Queue client ");

    QueueAdminClient adminClient = getQueueAdminClient(configFile);

    String queueName = props.getProperty(QUEUENAME);
    queueId = props.getProperty(QUEUEOCID);
    log (queueId + "|" + queueName);
    if ((queueName != null) && (queueName.trim().length() > 0) && (queueId == null))
    {
      queueName = queueName.trim();
      queueId = createQueue(queueName, compartmentId, adminClient, deadLetterQueueDeliveryCount, retentionInSeconds);
    }  

    logQueueInfoFor(queueId, adminClient);
    logQueueStatsFor(queueId, client);

    log ("... initialized OCI Queue Outputter");
  }


  /*
  * Take the provided log entry and either add it to the batch if a batch size is set 
  * or send it immediately to OCI
  * this method is defined based on using the LogGenerator interface
  */
  public void writeLogEntry(String entry)
  {
    log ("add message to batch: " + entry);
    batch.add(PutMessagesDetailsEntry.builder().content(entry).build());
    
    if ((batchSize == 0) || (batchSize == batch.size()))
    {
      log ("time to send the batch");
      PutMessagesDetails msgDetails = PutMessagesDetails.builder().messages(batch).build();
      PutMessagesRequest request =  PutMessagesRequest.builder().queueId(queueId).putMessagesDetails(msgDetails).build();

      log ("msgrequest created - " + msgDetails.toString() + "\n queueId=" + queueId);

      /* Send request to the Client */
      PutMessagesResponse response = client.putMessages(request);
      batch.clear();
      
      log ("OCI Queue Outputter:" + response.toString());
    }
  }

  /*
   * This method performs any cleardown, releasing messages currently in a batch, but not yet sent
   * The method is required by the LogGenerator interfacing
   */
  public void clearDown()
  {
    if ((batch != null) && (batch.size() > 0))
    {
      PutMessagesDetails msgDetails = PutMessagesDetails.builder().messages(batch).build();
      PutMessagesRequest request = PutMessagesRequest.builder().queueId(queueId).putMessagesDetails(msgDetails).build();

      /* Send request to the Client */
      PutMessagesResponse response = client.putMessages(request);
      batch.clear();
      
      if (verbose) {System.out.println ("OCI Queue Outputter:" + response.toString());}      
    }
    log ("OCI Queue Outputter - clearing down");
  }

 /*
  * This method will cycle round generating messages to send
  */
 static void send (SoloOCIQueueOutputter queueOut, Properties props, boolean useJSONformat, boolean verbose)
 {

    queueOut.initialize (props);

    int index = 0;
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DTG_FORMAT);  
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
        pause("send delay", 5);

        index++;
      }
    }
    catch (Exception err)
    {
      System.out.println ("loop disturbed " + err.toString());
      err.printStackTrace();
    }  
 }


/*
 * For certain messages the processing of the message may require longer than the default  period during which the message
 * is hidden from other possible consumers. For example if the queue is being used for mixed types of message, and one type 
 * of message requires longer to be processed. Another  scenario maybe that transactions are taking longer to complete
 * Here we're applying the extension of the non-visible time on an individual message
 */
static void extendInvisibility (SoloOCIQueueOutputter queue, String receipt, int delayDuration)
{
  UpdateMessageResponse response = queue.client.updateMessage(UpdateMessageRequest.builder()
        .queueId(queue.queueId)
        .messageReceipt(receipt)
        .updateMessageDetails(UpdateMessageDetails.builder()
                .visibilityInSeconds(delayDuration)
                .build())
        .build());
  // any issues with sending the receipt - log. A response here should either trigger a transaction rollback or compensating action
  //String errors = response.getUpdateMessageResult().getClientFailures();
  //if ((errors != null) && (errors.length() > 0))
  //{
  //  log ("Error with requesting a delay in visibility"+ errors + " for " + receipt);
  //}
}

/*
 * Messages are deleted once a receipt is returned back to the queue.  Here we're responding with a bulk delete
 */
static void deleteMessages (SoloOCIQueueOutputter queue, List<String> receipts)
{
  List<UpdateMessagesDetailsEntry> entries = new ArrayList<>();
  Iterator receiptsIter = receipts.iterator();

  while (receiptsIter.hasNext())
  {
    String receipt = (String)receiptsIter.next();
      entries.add(UpdateMessagesDetailsEntry.builder().receipt(receipt).build());
  }
  UpdateMessagesResponse batchResponse = queue.client
                .updateMessages(UpdateMessagesRequest.builder().queueId(queue.queueId)
                        .updateMessagesDetails(UpdateMessagesDetails.builder().entries(entries).build()).build());

  String errors = batchResponse.getUpdateMessagesResult().getClientFailures().toString();  

  if ((errors != null) && (errors.length() > 0))
  {
    log ("Errors deleting messages:" + errors);
  }
}

/*
 * Wrap up thread sleep logic into 1 declaration with exception handling - makes the
 * rest of the code a little neater and easier to read
 */
static void pause (String pauseName, int forSecs)
{
  try{ 
    Thread.sleep(forSecs*1000);
  }
  catch (Exception err) 
  {
    log(pauseName + " disturbed " + err.getMessage());
  }

}

/*
 * Implements the logic for reading from queues. Behaviour can be modified with the use of additional properties
 * POLLDURATIONSECS - non zero values will initiate log poll behaviour
 * DELETEDURATIONSECS - as long transactions may result in the need to delay making the queue entry for a process to read
 * this setting when configured will request OCI to delay exposing the message on the assumption the client had a problem.
 * INTERREADELAYSECS - if set will induce the code to pause between each read cycle
 */
static void readQueue (SoloOCIQueueOutputter queue, Properties props)
{
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DTG_FORMAT);  
    LocalDateTime now = LocalDateTime.now();
    String display_time = now.format(dtf);
    int loopCtr =0;
    int pollDurationSecs = 0;
    int interReadDelaySecs = 10;
    int deleteDelaySecs = 0;
    int maxLoops = 10;

    boolean extendMessageInvisibility = false;
    boolean unlimitedLoops = false;

    queue.initialize (props);

    try{
      // protect against risk of value set not parsing
      pollDurationSecs = Integer.parseInt(props.getProperty(POLLDURATIONSECS, "0"));
      interReadDelaySecs = Integer.parseInt(props.getProperty(INTERREADELAYSECS, "10"));
      deleteDelaySecs = Integer.parseInt(props.getProperty(DELETEDURATIONSECS, "0"));
      maxLoops = Integer.parseInt(props.getProperty(MAXGETS, "0"));
      unlimitedLoops = (maxLoops == 0);

    }
    catch (Exception err)
    {
      log ("trying to initialise config values -" + err.getMessage());
      unlimitedLoops = true;
    }

    log ("Read unlimited-"+ unlimitedLoops 
        + " maxLoops " + maxLoops 
        + "; each call duration " 
        + pollDurationSecs 
        + " will delay deletes " + (deleteDelaySecs>0));

    while (unlimitedLoops || (loopCtr < maxLoops))
    {
      loopCtr++;
      // we can make the request perform a long pole by setting a timeout. If not set then the request will immediately
      // respond with any currently available messages
      List<GetMessage> messages = queue.client.getMessages(GetMessagesRequest.builder()
              .queueId(queue.queueId)
              .timeoutInSeconds(pollDurationSecs)
              .build()).getGetMessages().getMessages();

      // iterate through each message - send message and receipt to the console and then acknowledge the receipt
      // for each messages
      if (messages.size() > 0)
      {
        ArrayList<String>deleteReceipts = new ArrayList();

        Iterator iter = messages.iterator();
        while (iter.hasNext())
        {
          GetMessage message = (GetMessage)iter.next();
          String receipt = message.getReceipt();
          String content = message.getContent();    
          log (display_time + " --message:" + message + " -- receipt:"+receipt);

          // if we expect the processing of an event to take longer than usual
          if (deleteDelaySecs > 0)
          {
            extendInvisibility (queue, receipt, deleteDelaySecs);
          }
          deleteReceipts.add (receipt);
        }

        // if we're asking for the visibility to be delayed, then lets also wait
        if (deleteDelaySecs > 0)
        {
          pause("delete delay", deleteDelaySecs);
        }

        deleteMessages(queue, deleteReceipts);
      }

      log ("Read attempt " + loopCtr + " done");

      if (interReadDelaySecs > 0)
      {
        pause("inter read", interReadDelaySecs);
      }
    }
  }

  /*
  * Simple main function that drives the class to generate a simple log message
  * To use this approach the environment needs the OCI Config file for the SDK
  * All the configuration details are described in the associated Readme file.
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
    SoloOCIQueueOutputter queue = new SoloOCIQueueOutputter();

    String action = null;
    Properties props = new Properties();
    // to avoid needing the base classes we grab environment variables and set the properties that way
    props.setProperty (BATCHSIZE, "1");
    if (System.getenv(QUEUEOCID) != null) 
    {
      props.setProperty (QUEUEOCID, System.getenv(QUEUEOCID));
      log ("queueId=" + props.getProperty(QUEUEOCID));
    }

    // if we try to set a property with a null value - then we'll get an exception
    // this is ok as these values are essential
    props.setProperty (REGION, System.getenv(REGION));
    props.setProperty (QUEUENAME, System.getenv(QUEUENAME));
    props.setProperty (OCICONFIGFILE, System.getenv(OCICONFIGFILE));
    boolean useJSONformat = (System.getenv(JSONFMT) != null);
    props.setProperty (QUEUECOMPARTMENTID, System.getenv(QUEUECOMPARTMENTID));
    props.setProperty (ISVERBOSE, System.getenv(ISVERBOSE));
    verbose =((System.getenv(ISVERBOSE) == null) || (System.getenv(ISVERBOSE).trim().equalsIgnoreCase("true")));

    if (args.length >0)
    {
      action = args[0].trim();
    }
    else
    {
      action = ACTION_SEND;
    }

    log ("action is:" + action);
    if (action.equalsIgnoreCase(ACTION_SEND))
    {
      send (queue, props, useJSONformat, verbose);
    }
    else if (action.equalsIgnoreCase(ACTION_DELETE))
    {
      deleteQueue (true,props.getProperty(QUEUENAME), props.getProperty(QUEUECOMPARTMENTID),  getQueueAdminClient(getConfigFile(props)));
    }
    else if (action.equalsIgnoreCase(ACTION_DELETE_OCID))
    {
      deleteQueue (false,props.getProperty(QUEUEOCID), props.getProperty(QUEUECOMPARTMENTID),  getQueueAdminClient(getConfigFile(props)));
    }      
    else if (action.equalsIgnoreCase(ACTION_LIST))
    {
      queue.verbose = true;
      getQueueOCIDFor (null,  props.getProperty(QUEUECOMPARTMENTID), getQueueAdminClient(getConfigFile(props)), false);
      queue.verbose = false;
    }
    else if (action.equalsIgnoreCase(ACTION_CONSUME))
    {
      readQueue (queue, props);
    }    
    else
    {
      log ("Action " + action + " not understood");
    }

    queue.clearDown();

  }  
}