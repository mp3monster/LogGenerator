
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
public class SoloOCIQueueDemoTool 
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
  private static final String RETENTIONSECONDS = "RETENTIONSECONDS";
  private static final String DLQCOUNT = "DLQCOUNT";
  private static final String JSONFMT = "JSONFMT";


  private static final String ACTION_SEND = "send";
  private static final String ACTION_SEND_NEW = "send-new";
  private static final String ACTION_LIST = "list";
    private static final String ACTION_INFO = "info";
  private static final String ACTION_CONSUME = "consume";
  private static final String ACTION_DELETE = "delete";
  private static final String ACTION_DELETE_OCID = "delete-ocid";


  private QueueClient client = null;
  private QueueAdminClient adminClient = null;

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
   * As permissions for information on the queues is split between the normal client and the admin (i.e. normal client can see queue depth)
   * admin can see the queue metadata like created etc   - need both clients
   * We have multiline so we can return all the details in a table like format for 1 queue, or a result more suited to displaying a table of multiple queues
   */
  static String logQueueInfoFor (String queueId, QueueClient client, QueueAdminClient adminClient, boolean multiline)
  {
    GetStatsRequest statsRequest = null;
    GetStatsResponse statsResponse = null;
    Stats stats = null;
    GetQueueRequest request = null;
    GetQueueResponse response = null;
    Queue queue = null;

    if (adminClient != null)
    {
      request = GetQueueRequest.builder().queueId(queueId).build();
      // you might want to consider including the Queue status into the attributes here. If not then recommend you examine the status of the queue in the response
      response = adminClient.getQueue(request);
      queue = response.getQueue();
    }

    if (client != null)
    {
      statsRequest = GetStatsRequest.builder().queueId(queueId).build();
      statsResponse = client.getStats(statsRequest);
      stats = statsResponse.getQueueStats().getQueue();
    }


    String queueInfo = "";
    String separator =" | ";    

    String size = "0";
    if (stats != null)
    {
      Long sizeBytes = stats.getSizeInBytes();
      if (sizeBytes != 0)
      {
        size = new BigDecimal(sizeBytes/1024).toString();
      }
    }
    
    if (multiline)
    {
      separator = "\n";
      queueInfo= "-------------" + separator;
    }

    queueInfo+= "Queue id : " + queueId + separator;
    if (queue != null)
    {
      queueInfo+= "Queue name :" + queue.getDisplayName()+separator;
    }

    if (queue != null)
    {

      queueInfo+= "Lifecycle state :" + queue.getLifecycleState().toString() + separator;
      if (queue.getLifecycleDetails() != null)
      {
        queueInfo+= "Lifecycle details :" + queue.getLifecycleDetails() + separator;
      }
      queueInfo+= "Created :" + queue.getTimeCreated().toString() + separator;
      queueInfo+= "Updated :" + queue.getTimeUpdated().toString() + separator;      
      queueInfo+= "Compartment Id :" + queue.getCompartmentId() + separator;
      queueInfo+= "Endpoint: " + queue.getMessagesEndpoint() + separator;
      queueInfo+= "retention mins :" + (queue.getRetentionInSeconds() / 60) + separator;
    }

    if (stats != null)
    {
      queueInfo+= "Visible messages : " + stats.getVisibleMessages().toString() + separator;
      queueInfo+= "InFlight messages : " + stats.getInFlightMessages().toString() + separator;
      queueInfo+= "Queue size (MB) : " + size + separator;
    }

    if (multiline)
    {
      queueInfo+= "-------------";
    }

    return (queueInfo);
     
  }

  /*
   * Get the queue OCID using the queue name. This assumes you're not likely to create the same queue in the same compartment
   * Strictly speaking we should us track the job status. By stipulating no or partial name we can list the queues. The returned OCID
   * is either the first occurrence found or the last occurrence (of the name found) depending upon the returnFirst
   * As a resault we can use this method to list the compartments queues.
   */
  static String getQueueOCIDFor (String queueName, String compartmentId, QueueAdminClient adminClient, boolean returnFirst, boolean displayInfo)
  {
    String queueOCID = null;

    // we'll make a number of attempts to locate the desired queue
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

      log ("Matched " + queueList.size());
      while (iter.hasNext())
      {
        queue = (QueueSummary)iter.next();
        String additionalInfo = "";
        if (displayInfo)
        {
          //additionalInfo = " -->" + logQueueInfoFor( queue.getId(), null, adminClient, false);
          log (logQueueInfoFor( queue.getId(), null, adminClient, false));
        }
        else
        {
          log ("Located queue:" + queue.getDisplayName() + " -- " + queue.getId() );
        }

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

      // once we've got a match return the result - no point in retrying
      if ((queueName == null) || (queueOCID != null))
      {
        return queueOCID;
      }

      pause("get OCID", 10);

      log ("get queue OCID attempt "+ attempt);


    }

    // failed to locate 
    return null;
  }

  /*
   * This deletes the queue. The queue can be either the queue's name or OCID. If the name is provided then the 1st occurrence of the named
   * queue in the compartment is targeted.
   * We can delete by using the queue display name or via the OCID
   */
  static void deleteQueue (boolean byName, String id, String compartmentId, QueueAdminClient adminClient)
  {
    String ocid = id;
    log ("Deleting " + id);
    if (byName)
    {
      ocid = getQueueOCIDFor (id, compartmentId, adminClient, true, false);
    }

    if (ocid != null)
    {
      // request the queue deletion
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
        log ("Deleted entity = " + requestResource.getEntityType() + " | Deleted OCID  = " + requestResource.getIdentifier());
      }

      // share the timing information
      log ("Action requested at  = " + workData.getTimeAccepted().toString());
      Date completed = workData.getTimeFinished();
      if (completed != null)
      {
        log ("Action finished at  = " + workData.getTimeFinished().toString());
      }

      log ("Action progress  " + workData.getPercentComplete().toString() + "% complete");

    }
    else
    {
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
    // provide the queue configuration - setting options such as rention, DLQ etc
    CreateQueueDetails details = CreateQueueDetails.builder().compartmentId(compartmentId).
          deadLetterQueueDeliveryCount(deadLetterQueueDeliveryCount).
          displayName(queueName).
          retentionInSeconds(retentionInSeconds).
          build();

    // submit the request and then track the progress 
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
    deadLetterQueueDeliveryCount = new Integer(props.getProperty(DLQCOUNT, "0"));
    retentionInSeconds = new Integer(props.getProperty(RETENTIONSECONDS, "1200"));

    regionName = props.getProperty(REGION);
    compartmentId = props.getProperty(QUEUECOMPARTMENTID);

    configFile = getConfigFile(props);
    AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFile);

    adminClient = getQueueAdminClient(configFile);

    String queueName = props.getProperty(QUEUENAME);
    queueId = props.getProperty(QUEUEOCID);
    log (queueId + "|" + queueName);
    if ((queueName != null) && (queueName.trim().length() > 0) && (queueId == null))
    {
      queueName = queueName.trim();
      queueId = createQueue(queueName, compartmentId, adminClient, deadLetterQueueDeliveryCount, retentionInSeconds);

    }  

    log ("initialized OCI Queue client ");
    client = QueueClient.builder().build(provider);
    client = setQueueEndpoint (adminClient, client, queueId, props);

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
 static void send (SoloOCIQueueDemoTool queueOut, Properties props, boolean useJSONformat, boolean verbose)
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
static void extendInvisibility (SoloOCIQueueDemoTool queue, String receipt, int delayDuration)
{
  log ("Extending visibility for " + delayDuration);
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
  log ("visibility delayed " + response.toString());
  //}
}

/*
 * Messages are deleted once a receipt is returned back to the queue.  Here we're responding with a bulk delete
 */
static void deleteMessages (SoloOCIQueueDemoTool queue, List<String> receipts)
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
static void readQueue (SoloOCIQueueDemoTool queue, Properties props)
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
        + "; each call duration " + pollDurationSecs 
        + " will delay deletes for" + deleteDelaySecs);

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

  static void setPropertyFromVar(String propname, String envName, Properties props)
  {
    String envVal = System.getenv(envName);
    if (envVal != null)
    {
      props.setProperty(propname, envVal);
    }
  }

  static void displayInfo(SoloOCIQueueDemoTool queue, Properties props)
  {
      queue.initialize (props);
      queue.verbose = true;
      log (logQueueInfoFor(props.getProperty(QUEUEOCID), queue.client, queue.adminClient, true));
      queue.verbose = true;
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
    SoloOCIQueueDemoTool queue = new SoloOCIQueueDemoTool();

    String action = null;
    Properties props = new Properties();
    // to avoid needing the base classes we grab environment variables and set the properties that way
    props.setProperty (BATCHSIZE, "1");

    boolean useJSONformat = (System.getenv(JSONFMT) != null);
    setPropertyFromVar (REGION, REGION, props);
    setPropertyFromVar (QUEUENAME, QUEUENAME, props);
    setPropertyFromVar (QUEUEOCID, QUEUEOCID, props);
    setPropertyFromVar (OCICONFIGFILE, OCICONFIGFILE, props);
    setPropertyFromVar (QUEUECOMPARTMENTID, QUEUECOMPARTMENTID, props);
    setPropertyFromVar (ISVERBOSE, ISVERBOSE, props);
    setPropertyFromVar (MAXGETS, MAXGETS, props);
    setPropertyFromVar (DELETEDURATIONSECS, DELETEDURATIONSECS, props);
    setPropertyFromVar (POLLDURATIONSECS, POLLDURATIONSECS, props);
    setPropertyFromVar (DLQCOUNT, DLQCOUNT, props);
    setPropertyFromVar (RETENTIONSECONDS, RETENTIONSECONDS, props);

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
    if (action.equalsIgnoreCase(ACTION_SEND_NEW))
    {
      props.remove(QUEUEOCID);
      send (queue, props, useJSONformat, verbose);
    }    
    else if (action.equalsIgnoreCase(ACTION_DELETE))
    {
      deleteQueue (true,props.getProperty(QUEUENAME), props.getProperty(QUEUECOMPARTMENTID),  getQueueAdminClient(getConfigFile(props)));
    }
    else if (action.equalsIgnoreCase(ACTION_DELETE_OCID))
    {
      String deleteOCID = props.getProperty(QUEUEOCID);
      if (args.length >1)
      {
        String altOCID = args[1].trim();
        if (altOCID.length() > 0)
        {
          deleteOCID = altOCID;
        }
      }
      deleteQueue (false, deleteOCID, props.getProperty(QUEUECOMPARTMENTID),  getQueueAdminClient(getConfigFile(props)));
    }    
    else if (action.equalsIgnoreCase(ACTION_LIST))
    {
      queue.verbose = true;
      getQueueOCIDFor (null,  props.getProperty(QUEUECOMPARTMENTID), getQueueAdminClient(getConfigFile(props)), false, true);
      queue.verbose = false;
    }
    else if (action.equalsIgnoreCase(ACTION_CONSUME))
    {
      readQueue (queue, props);
    }    
    else if (action.equalsIgnoreCase(ACTION_INFO))
    {
      displayInfo(queue, props);
    }
    else
    {
      log ("Action " + action + " not understood");
    }

    queue.clearDown();

  }  
}