import java.util.StringTokenizer;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;    
import java.xml.*;
import java.net.ServerSocket;
import java.util.logging.*;

// this utility will either delete the nominated node from the API management logical OR
// approve a pending gateway join. The behaviour is dictated by the parameters

public class LogSimulator 
{

final static String HELPMSG = "Parameters :\n" +
                                "   1. Expected: properties file e.g. tool.properties containing all the controls - \n      if not defined then a default tool.properties will attempt to be loaded\n" +
                                "   2. Optional: simulated log messages e.g. data.txt - \n      if provided this overrides the source file defined in the properties" +
                                "\n\nFull help available at https://github.com/mp3monster/LogGenerator \n\n";

final static String YPROPVAL = "y";
final static String YESPROPVAL = "yes";
final static String TPROPVAL = "t";
final static String TRUEPROPVAL = "true";
final static String LOOP = "REPEAT";
final static String JULCONFIG = "JULCONFIG";
final static String JULNAME = "JULName";
final static String SOURCESEPARATOR= "SOURCE-SEPARATOR";
final static String TARGETSEPARATOR= "TARGET-SEPARATOR";
final static String SOURCEFORMAT = "SOURCEFORMAT";
final static String TARGETFORMAT = "TARGETFORMAT";
final static String SOURCEFILE = "SOURCE";
final static String TARGETFILE = "TARGETFILE";
final static String TARGETIP="TARGETIP";
final static String TARGETPORT="TARGETPORT";
final static String TARGETDTG = "TARGETDTG";
final static String TARGETURL = "TARGETURL";
final static String SOURCEDTG = "SOURCEDTG";
final static String OUTTYPE = "OUTPUTTYPE";
final static String DEFAULTLOGLEVEL = "DEFAULT-LOGLEVEL";
final static String ACCELERATOR = "ACCELERATEBY";
final static String CONSOLE = "console";
final static int UNKNOWNOUTPUT = -1;
final static int CONSOLEOUTPUT = 0;
final static String HTTP = "HTTP";
final static int HTTPOUTPUT = 1;
final static String FILE = "file";
final static int FILEOUTPUT = 2;
final static String TCPOUT="TCP";
final static int TCPOUTPUT = 3;
final static int JUL = 4;
final static String JULOUT="JUL";
final static int STD = 5;
final static String STDOUT="STDOUT";
final static int ERR = 6;
final static String ERROUT="ERROUT";


final static String DEFAULTLOC= "DEFAULT-LOCATION";
final static String DEFAULTPROC= "DEFAULT-PROCESS";
final static String VERBOSE= "VERBOSE";


final static String TIME = "%t";
final static String LOGLEVEL = "%l";
final static String LOCATION = "%c";
final static String MESSAGE = "%m";
final static String PROCESS = "%p";
final static String LOOPCOUNTER = "%i";
final static String ITERCOUNTER = "%j";

final static String PROPFILENAMEDEFAULT = "tool.properties";

private boolean verbose = false; // allows us to pretty print all the API calls if necessary

private Logger juLogger = null;

class LogSimulatorException extends Exception
{
}

static class LogEntry 
{
    public static defaultLogLevel = "";
    public static defaultProcess = "";
    public static defaultLocation = "";

    public int offset = 0; // time in millis
    public String logLevel = defaultLogLevel; // string representing the log level
    public String process = defaultProcess; // presents the process name or thread
    public String location = defaultLocation; // class path etc
    public String message = null; // core message

    public String toString ()
    {
        return "offset="+offset+"|"+logLevel+"|"+process+"|"+location+"|"+message;
    }
}

static Level toJULLevel (String level, Properties props)
{
    if ((level == null) || (level.length() == 0))
    {
        if ((props.get(DEFAULTLOGLEVEL) != null) || (props.get(DEFAULTLOGLEVEL).length() > 0))
        {
            level = props.get(DEFAULTLOGLEVEL);
        }
        else
        {
            return Level.INFO;
        }
    }
    
    if (level.equalsIgnoreCase("SEVERE") || level.equalsIgnoreCase("ERROR") || level.equalsIgnoreCase("FATAL"))
    {
        return Level.SEVERE;
    }
    else if (level.equalsIgnoreCase("WARNING") || level.equalsIgnoreCase("WARN"))
    {
        return Level.WARNING;
    }
    else if (level.equalsIgnoreCase("INFO") || level.equalsIgnoreCase("INFORMATION"))
    {
        return Level.INFO;
    }
    else if (level.equalsIgnoreCase("CONFIG"))
    {
        return Level.CONFIG;
    }
    else if (level.equalsIgnoreCase("FINE") || level.equalsIgnoreCase("TRACE"))
    {
        return Level.FINE;
    }
    else if (level.equalsIgnoreCase("FINER"))
    {
        return Level.FINER;
    }
    else if (level.equalsIgnoreCase("FINEST"))
    {
        return Level.FINEST;
    }
    else
    {
        return Level.INFO;
    }
}


static int getOutputType (Properties props, boolean verbose)
{
        int outType = UNKNOWNOUTPUT;
        String propOut = props.get(OUTTYPE);
        if ((propOut  != null) && (propOut.length() > 0))
        {
            if (propOut.equalsIgnoreCase(FILE))
            {
                outType = FILEOUTPUT;
            }
            else if (propOut.equalsIgnoreCase(CONSOLE))
            {
                outType = CONSOLEOUTPUT;
            }
            else if (propOut.equalsIgnoreCase(HTTP))
            {
                outType = HTTPOUTPUT;
            }    
            else if (propOut.equalsIgnoreCase(TCPOUT))
            {
                outType = TCPOUTPUT;
            }
            else if (propOut.equalsIgnoreCase(JULOUT))
            {
                outType = JUL;
            }
            else if (propOut.equalsIgnoreCase(STD))
            {
                outType = STDOUT;
            }     
            else if (propOut.equalsIgnoreCase(ERR))
            {
                outType = ERROUT;
            }                       
            else
            {
                if (verbose){System.out.println ("Unknown output type :" + props.get(OUTTYPE));}
            }        
        }   

        return outType;
}

// Takes the log event elements and builds the output using the formatting template
static String logToString (LogEntry log, String dtgFormat, String separator, String outTemplate, 
                            boolean verbose, int counter, int iterCount)
{

            String output = null;

            if (outTemplate != null)
            {
                 output = new String(outTemplate);
            }
            else
            {
                output = TIME + separator + MESSAGE;
            }

            if (output.indexOf (TIME) > -1)
            {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dtgFormat);  
                LocalDateTime now = LocalDateTime.now();
                if (verbose) {System.out.println (TIME + " == " + now.format(dtf));}
                output = output.replace(TIME, now.format(dtf));
            }

            if (output.indexOf (LOGLEVEL) > -1)
            {
                if (log.logLevel != null)
                {
                    output = output.replace(LOGLEVEL, log.logLevel);
                }
                else
                {
                    output = output.replace(LOGLEVEL, "");
                }
            }

            if (output.indexOf (PROCESS) > -1)
            {
                if (log.process != null)
                {
                    output = output.replace(PROCESS, log.process);
                }
                else
                {
                    output = output.replace(PROCESS, "");
                }
            }            

            if (output.indexOf (LOCATION) > -1)
            {
                if (log.location != null)
                {
                    output = output.replace(LOCATION, log.location);
                }
                else
                {
                    output = output.replace(LOCATION, "");
                }
            }  

            if (output.indexOf (MESSAGE) > -1)
            {
                if (log.message != null)
                {
                    output = output.replace(MESSAGE, log.message);
                }
                else
                {
                    output = output.replace(MESSAGE, "");
                }
            }
            if (output.indexOf (LOOPCOUNTER) > -1)
            {
                output = output.replace(LOOPCOUNTER, String.valueOf (counter));
            }
            if (output.indexOf (ITERCOUNTER) > -1)
            {
                output = output.replace(ITERCOUNTER, String.valueOf (iterCount));
            }

            return output;
}


static ArrayList<LogEntry> loadLogs (String source, String separator, String format, boolean verbose)
{

    BufferedReader sourceReader = new BufferedReader(new FileReader(source));  //creates a buffering character input stream  
    ArrayList<LogEntry> lines = new ArrayList<LogEntry> ();
    String[] formatArray = format.split (" ");
    for (int idx = 0; idx < formatArray.length; idx++)
    {
        formatArray[idx] = formatArray[idx].trim();
    }

    String line = sourceReader.readLine();
    while (line != null)
    {
        LogEntry aLogEntry = new LogEntry();
        StringTokenizer st = new StringTokenizer(line, separator); // why does it fail when we pass in the separator

        int fmtIdx = 0;
        String element = null;
        boolean valueSet = false;

        while (st.hasMoreElements())
        {
            element = (String)st.nextElement();

            while ((fmtIdx < formatArray.length) && (!valueSet))
            {
                element = element.trim();
                if (formatArray[fmtIdx].equals (TIME))
                {
                    if (element.charAt(0) == '+')
                    {
                        aLogEntry.offset = Integer.parseInt(element.substring(1));
                        valueSet = true;
                    }
                    else
                    {
                        // to convert date time to offset
                    }
                }
                else if (formatArray[fmtIdx].equals (LOGLEVEL))
                {
                    aLogEntry.logLevel = element;
                    valueSet = true;

                }
                else if (formatArray[fmtIdx].equals (LOCATION))
                {
                    aLogEntry.location = element;
                    valueSet = true;

                }    
                else if (formatArray[fmtIdx].equals (MESSAGE))
                {
                    aLogEntry.message = element;
                    while (st.hasMoreElements())
                    {
                        aLogEntry.message = aLogEntry.message + separator + st.nextElement();
                    }
                    valueSet = true;

                } 
                else if (formatArray[fmtIdx].equals (PROCESS))
                {
                    aLogEntry.process = element;
                    valueSet = true;

                }                   
                else
                {
                    if (verbose) {System.out.println ("Unrecognized formatter code : " + formatArray[fmtIdx]);}
                }       
                fmtIdx++;       
            }
            valueSet = false;
        }

        lines.add(aLogEntry);
        if (verbose){System.out.println ("entry ==>" + aLogEntry.toString());}
        line = sourceReader.readLine();

    }

    return lines;
}

// public void main (String[] args)
public void core (String[] args)
{
    System.out.println ("Starting ...");

    String propFilename = PROPFILENAMEDEFAULT;
    String sourceFilename = null;
    Properties props = new Properties();

    // process the command line properties
    if (args.size() > 0)
    {
        if (args[0].equalsIgnoreCase("-h"))
        {
            System.out.println(HELPMSG);
            System.exit(-1);
        }
        else 
        {
            propFilename = args[0];
            println ("Going to use " + propFilename);

            if (args.size() > 1)
            {
                // we've been given the data file in the command line - this trumps any file set in the properties
                sourceFilename = args[1];

                if (sourceFilename != null)
                {
                    sourceFilename = sourceFilename.trim();

                    if (sourceFilename.length() < 1)
                    {
                        sourceFilename = null;
                    }

                }
            }
        }
    }
    else
    {
        println("going to use default properties file");
    }

    try
    {
        println ("handling properties file - " + propFilename);

        File propFile = new File(propFilename);
        props.load(propFile.newDataInputStream());

        String sourceSeparator = props.get (SOURCESEPARATOR);
        String targetSeparator = props.get (TARGETSEPARATOR);

        if ((sourceSeparator == null) || (sourceSeparator.size() == 0))
        {
            props.put (SOURCESEPARATOR, " ");
        }
        if ((targetSeparator == null) || (targetSeparator.size() == 0))
        {
            props.put (TARGETSEPARATOR, " ");
        }

        if (sourceFilename != null)
        {
            props.put (SOURCEFILE, sourceFilename);
        }

    }
    catch (Exception err)
    {
        println("Couldn't manage properties:\n" + err.getMessage());
        println(err.getStackTrace());
        println(HELPMSG);
        System.exit(-1);
    }


    // verify all the parameters
    try
    {
        // assert ((props.get(TARGETFILE) != null) && (props.get(TARGETFILE).size() > 0)): "SOURCE not defined";
        assert ((props.get(SOURCEFILE) != null) && (props.get(SOURCEFILE).size() > 0)): "TARGET not defined";
        assert ((props.get(SOURCEFORMAT) != null) && (props.get(SOURCEFORMAT).size() > 0)): "No formatting for output defined";

    }
    catch (AssertionError err)
    {

        System.out.println(err.getMessage());
        System.out.println(HELPMSG);

        System.exit(-1);
    }

    if ((props.get(VERBOSE) != null) && (props.get(VERBOSE).equalsIgnoreCase("true")))
    {
        verbose=true;
        System.out.println ("In verbose mode");
    }
    else
    {
        verbose = false;
    }



    if ((props.get(DEFAULTLOC) != null) && (props.get(DEFAULTLOC).length() > 0))
    {
        LogEntry.defaultLocation = props.get(DEFAULTLOC);
    }
    if ((props.get(DEFAULTPROC) != null) && (props.get(DEFAULTPROC).length() > 0))
    {
        LogEntry.defaultProcess= props.get(DEFAULTPROC);
    } 

    int accelerationFactor =1;
    if ((props.get(ACCELERATOR) != null) && (props.get(ACCELERATOR).length() > 0))
    {
        try
        {
            accelerationFactor = Integer.parseInt(props.get(ACCELERATOR));
        }
        catch (NumberFormatException err)
        {
            System.out.println ("Couldn't process accelerator value >" + props.get(ACCELERATOR)+"<");
        }
    }

    

    // initialize the default values
    LogEntry.defaultLocation = props.get(DEFAULTLOC, "");
    LogEntry.defaultLogLevel = props.get(DEFAULTLOGLEVEL, "");
    LogEntry.defaultProcess = props.get(DEFAULTPROC, "");

    ArrayList<LogEntry> logs = loadLogs (props.get(SOURCEFILE), props.get (SOURCESEPARATOR), props.get (SOURCEFORMAT), verbose);
    LogEntry log = null;
    String dtgFormat = "HH:mm:ss";
    if ((props.get(TARGETDTG) != null) && (props.get(TARGETDTG).length() > 0))
    {
        dtgFormat = props.get(TARGETDTG);
    }

    int loopTotal = 1;
    int loopCount = 0;
    if (props.get(LOOP) != null)
    {
        try
        {
            loopTotal = Integer.parseInt(props.get(LOOP));
        }
        catch (NumberFormatException err)
        {
            System.out.println ("Couldn't process loop counter >" + props.get(LOOP)+"<");
        }
    }

    while (loopCount < loopTotal)
    {
        int lineCount = 0;
        loopCount++;
        if (verbose) {System.out.println ("Performing data set pass " + loopCount + " of " + loopTotal);}

        Iterator iter = logs.iterator();
        String separator = props.get (TARGETSEPARATOR);

        BufferedWriter bufferedWriter = null;

        while (iter.hasNext())
        {
            lineCount++;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dtgFormat);  
            LocalDateTime now = LocalDateTime.now();

            log = (LogEntry) iter.next();
            String output =  logToString(log, dtgFormat, separator,  props.get(TARGETFORMAT),  verbose, loopCount, lineCount);
            String iterCount = "";

            switch(getOutputType(props, verbose)) 
            {
                case CONSOLEOUTPUT:
                    if (verbose) {System.out.println ("Console:" + output);}
                break

                case FILEOUTPUT: 
                    if (bufferedWriter == null)
                    {
                        bufferedWriter = new BufferedWriter(new FileWriter(props.get(TARGETFILE), true));
                    }
                    bufferedWriter.write(output+"\n");
                    bufferedWriter.flush();
                break;

                case HTTPOUTPUT:
                    if (verbose) {System.out.println ("HTTP:" + output);}

                    URL baseUrl = new URL(props.get(TARGETURL));
                    URLConnection webConnection = baseUrl.openConnection();
                    webConnection.doOutput = true;
                    webConnection.requestMethod = 'POST';
                    webConnection.setRequestProperty("content-type", "application/json");

                    webConnection.with {
                        outputStream.withWriter { writer ->  writer << output }
                        outputStream.flush();
                    }
                    String response= webConnection.getContent();
                break;

                case TCPOUTPUT:
                    if (verbose) {System.out.println ("about to fire TCP:" + output);}
                    Socket sock = new Socket(props.get(TARGETIP), Integer.parseInt(props.get(TARGETPORT)));
                    OutputStream outStream = sock.getOutputStream();
                    Writer writer = new PrintWriter (outStream, true);
                    writer.println (output);
                    writer.close();
                    sock.close();
                break;

                case JUL:
                    if (juLogger == null)
                    {
                        LogManager manager = LogManager.getLogManager();
                        String loggerConfig = props.get(JULCONFIG);
                        if (loggerConfig != null)
                        {
                            System.out.println ("properties:" + loggerConfig);
                            manager.readConfiguration(new FileInputStream(loggerConfig));
                        }
                        String loggerName = props.get(JULNAME);
                        if (loggerName == null)
                        {
                            loggerName = "";
                        }
                        juLogger = Logger.getLogger (loggerName);

                        if (verbose) {System.out.println ("Created JUL Logger called " + juLogger.getName());}

                    }

                    if (verbose) {System.out.println ("about to fire Java Util Logging ("+toJULLevel(log.logLevel, props)+") " + log.message);}
                    try 
                    {
                        juLogger.logp (toJULLevel(log.logLevel, props), log.location,"", output);
                    }
                    catch (Exception err)
                    {
                        if (verbose) {System.out.println ("Failed to log " + log.toString());}
                    }

                break;

                case STD:
                    System.out.println (output);
                break;

                case ERR:
                    System.err.println (output);
                break;                

                default:
                    if (verbose) {System.out.println ("defaulted==>" + getOutputType(props, verbose));}

            }

            if (log != null)
            {
                try
                {
                    sleep (Math.round((log.offset)/accelerationFactor));
                }
                catch (Exception err)
                {
                    sleep (log.offset);
                }
            }
        }

    }

    // tidy up
    switch(getOutputType(props, verbose)) 
    {
        case HTTPOUTPUT:
            sleep (60);
        break;

        case JUL:
            juLogger.finalize();
            juLogger = null;
        break;

        default:
            if (verbose) {System.out.println ("No clear down needed");}
    }

    System.exit(0);
}

public static void main (String[] args)
{
    new LogSimulator().core(args); 
}

}

    //new LogSimulator().core(args);

