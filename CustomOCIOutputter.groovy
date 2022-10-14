
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.loggingingestion.LoggingClient;
import com.oracle.bmc.loggingingestion.model.*;
import com.oracle.bmc.loggingingestion.requests.*;
import com.oracle.bmc.loggingingestion.responses.*;
import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.Arrays;

/*
 * Implements the mechanism to communicate with OCI Logging
 * underlying documentation on how this can be done is detailed at:
 * https://docs.oracle.com/en-us/iaas/api/#/en/logging-dataplane/20200831/LogEntry/PutLogs
 *
 * Controls from this are loaded from the provided properties file
 * connection details to OCI come from a separate properties file
 */
class CustomOCIOutputter implements LogGenerator.RecordLogEvent
{

  private static final String AGENTTYPE="LOGSIM";
  private static final String AGENTNAME = "AGENTNAME";
  private static final String BATCHSIZE = "BATCHSIZE";
  private static final String LOGICID = "LogOCID";
  private static final String OCICONFIGFILE = "OCICONFIGFILE";
  private static final String PROPFILEGROUP = "PROPFILEGROUP";


  private String logOCID = null;
  private LoggingClient client = null;
  private String agentName = "LogSimulator";
  private String logSubject = "";
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
    log("initializing OCIOutputter ....");
    ConfigFileReader.ConfigFile configFile = null;

    logOCID = props.get(LOGICID);

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
        batch = new ArrayList();
      }
      catch (NumberFormatException numErr)
      {
        log ("Failed to translate batch size default to no batch");
      }
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

    client = new LoggingClient(provider);

    log ("... initialized OCIOutputter");
  }


  /*
  * Take the provided log entry and either add it to the batch if a batch size is set 
  * or send it immediately to OCI
  */
  public writeLogEntry(String entry)
  {
    PutLogsDetails putLogsDetails = null;
    Date timestamp = new Date();
    
    if (batchSize > 0)
    {
      batch.add(entry);
    
      if (batchSize == batch.size())
      {
        log ("preparing to send " + batch.size())
        putLogsDetails = PutLogsDetails.builder()
        .specversion("1.0")
          .logEntryBatches(new ArrayList<>(Arrays.asList(LogEntryBatch.builder()
              .entries(new ArrayList<>(Arrays.asList(LogEntry.builder()
                  .data(entry)
                  .id(logOCID)
                  .time(timestamp).build())))
              .source(agentName)
              .type(AGENTTYPE)
              .subject(logSubject)
              .defaultlogentrytime(timestamp).build()))).build();
        batch.clear();
      }
    }
    else
    {
      /* Create a request and dependent object(s). */
      putLogsDetails = PutLogsDetails.builder()
      .specversion("1.0")
        .logEntryBatches(new ArrayList<>(Arrays.asList(LogEntryBatch.builder()
            .entries(new ArrayList<>(Arrays.asList(LogEntry.builder()
                .data(entry)
                .id(logOCID)
                .time(timestamp).build())))
            .source(agentName)
            .type(AGENTTYPE)
            .subject(logSubject)
            .defaultlogentrytime(timestamp).build()))).build();
    }

    if (putLogsDetails != null)
    {
      PutLogsRequest putLogsRequest = PutLogsRequest.builder()
        .logId(logOCID)
        .putLogsDetails(putLogsDetails)
        .timestampOpcAgentProcessing(timestamp)
        .build();

      /* Send request to the Client */
      PutLogsResponse response = client.putLogs(putLogsRequest);
      
      if (verbose) {System.out.println ("OCI Outputter:" + response.toString());}
    }
  }


  public clearDown()
  {
    log ("OCI Outputter - clearing down")
    if ((batch != null) && (batch.size() > 0))
    {
      putLogsDetails = PutLogsDetails.builder()
      .specversion("1.0")
        .logEntryBatches(new ArrayList<>(Arrays.asList(LogEntryBatch.builder()
            .entries(new ArrayList<>(Arrays.asList(LogEntry.builder()
                .data(entry)
                .id(logOCID)
                .time(timestamp).build())))
            .source(agentName)
            .type(AGENTTYPE)
            .subject(logSubject)
            .defaultlogentrytime(timestamp).build()))).build();
      batch.clear();

      PutLogsRequest putLogsRequest = PutLogsRequest.builder()
        .logId(logOCID)
        .putLogsDetails(putLogsDetails)
        .timestampOpcAgentProcessing(timestamp)
        .build();

      /* Send request to the Client */
      PutLogsResponse response = client.putLogs(putLogsRequest);
      
      if (verbose) {System.out.println ("OCI Outputter:" + response.toString());}
    }
  }
}