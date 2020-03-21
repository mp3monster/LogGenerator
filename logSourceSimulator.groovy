import java.util.StringTokenizer;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;    
import java.xml.*;

// this utility will either delete the nominated node from the API management logical OR
// approve a pending gateway join. The behaviour is dictated by the parameters

public class LogSimulator 
{

final static String HELPMSG = "Help available at https://github.com/mp3monster/LogGenerator";

final static String YPROPVAL = "y";
final static String YESPROPVAL = "yes";
final static String TPROPVAL = "t";
final static String TRUEPROPVAL = "true";
final static String LOOP = "REPEAT";
final static String SOURCESEPARATOR= "SOURCE-SEPARATOR";
final static String TARGETSEPARATOR= "TARGET-SEPARATOR";
final static String SOURCEFORMAT = "SOURCEFORMAT";
final static String TARGETFORMAT = "TARGETFORMAT";
final static String SOURCEFILE = "SOURCE";
final static String TARGETFILE = "TARGETFILE";
final static String TARGETDTG = "TARGETDTG";
final static String TARGETURL = "TARGETURL";
final static String SOURCEDTG = "SOURCEDTG";
final static String OUTTYPE = "OUTPUTTYPE";
final static String CONSOLE = "console";
final static int UNKNOWNOUTPUT = -1;
final static int CONSOLEOUTPUT = 0;
final static String HTTP = "HTTP";
final static int HTTPOUTPUT = 1;
final static String FILE = "file";
final static int FILEOUTPUT = 2;

final static String DEFAULTLOC= "DEFAULT-LOCATION";
final static String DEFAULTPROC= "DEFAULT-PROCESS";
final static String VERBOSE= "VERBOSE";


final static String TIME = "%t";
final static String LOGLEVEL = "%l";
final static String LOCATION = "%c";
final static String MESSAGE = "%m";
final static String PROCESS = "%p";

final static String PROPFILENAMEDEFAULT = "tool.properties";

private boolean verbose = false; // allows us to pretty print all the API calls if necessary

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


static int getOutputType (Properties props, boolean verbose)
{
        int outType = UNKNOWNOUTPUT;
        if ((props.get(OUTTYPE)  != null) && (props.get(OUTTYPE).length() > 0))
        {
            if (props.get(OUTTYPE).equalsIgnoreCase(FILE))
            {
                outType = FILEOUTPUT;
            }
            else if (props.get(OUTTYPE).equalsIgnoreCase(CONSOLE))
            {
                outType = CONSOLEOUTPUT;
            }
            else if (props.get(OUTTYPE).equalsIgnoreCase(HTTP))
            {
                outType = HTTPOUTPUT;
            }    
            else
            {
                if (verbose){System.out.println ("Unknown output type :" + props.get(OUTTYPE));}
            }        
        }   

        return outType;
}

static String logToString (LogEntry log, String dtgFormat, String separator, String outTemplate, boolean verbose)
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
                if (verbose) {System.out.println (TIME + "   " + now.format(dtf));}
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
        LogEntry logEntry = new LogEntry();
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
                        logEntry.offset = Integer.parseInt(element.substring(1));
                        valueSet = true;
                    }
                    else
                    {
                        // to convert date time to offset
                    }
                }
                else if (formatArray[fmtIdx].equals (LOGLEVEL))
                {
                    logEntry.logLevel = element;
                    valueSet = true;

                }
                else if (formatArray[fmtIdx].equals (LOCATION))
                {
                    logEntry.location = element;
                    valueSet = true;

                }    
                else if (formatArray[fmtIdx].equals (MESSAGE))
                {
                    logEntry.message = element;
                    while (st.hasMoreElements())
                    {
                        logEntry.message = logEntry.message + separator + st.nextElement();
                    }
                    valueSet = true;

                } 
                else if (formatArray[fmtIdx].equals (PROCESS))
                {
                    logEntry.process = element;
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

        lines.add(logEntry);
        if (verbose){System.out.println (logEntry.toString());}
        line = sourceReader.readLine();

    }

    return lines;
}

public void main (String[] args)
{
    System.out.println ("Starting ...");

    String propFilename = PROPFILENAMEDEFAULT;
    Properties props = new Properties();

    // process the command line properties
    if (args.size() > 0)
    {
        if (args[0].equalsIgnoreCase("-h"))
        {
            System.out.println("Parameters needed are:\n" + HELPMSG);
        }
        else 
        {
            propFilename = args[0];
            println ("Going to use " + propFilename);
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
        try{
        loopTotal = Integer.parseInt(props.get(LOOP));
        }
        catch (NumberFormatException err)
        {
            System.out.println ("Couldn't process loop counter >" + props.get(LOOP)+"<");
        }
    }

    while (loopCount < loopTotal)
    {
        loopCount++;
        if (verbose) {System.out.println ("Performing data set pass " + loopCount + " of " + loopTotal);}

        Iterator iter = logs.iterator();
        String separator = props.get (TARGETSEPARATOR);

        BufferedWriter bufferedWriter = null;

        while (iter.hasNext())
        {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dtgFormat);  
            LocalDateTime now = LocalDateTime.now();

            log = (LogEntry) iter.next();
            String output =  logToString(log, dtgFormat, separator,  props.get(TARGETFORMAT),  verbose);

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
                break

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

                break

                default:
                    if (verbose) {System.out.println (outType);}

            }

            if (log != null)
            {
                sleep (log.offset);
            }
        }

    }

    if (getOutputType(props, verbose) == HTTPOUTPUT)
    {
        sleep (60);
    }

    System.exit(0);
}
}

    new LogSimulator().main(args);

