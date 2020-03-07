import java.util.StringTokenizer;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;    

// this utility will either delete the nominated node from the API management logical OR
// approve a pending gateway join. The behaviour is dictated by the parameters

public class LogSimulator 
{

final static String HELPMSG = "Help available at https://github.com/mp3monster/LogGenerator";

final static String YPROPVAL = "y";
final static String YESPROPVAL = "yes";
final static String TPROPVAL = "t";
final static String TRUEPROPVAL = "true";
final static String SOURCESEPARATOR= "SOURCE-SEPARATOR";
final static String TARGETSEPARATOR= "TARGET-SEPARATOR";
final static String SOURCEFORMAT = "SOURCEFORMAT";
final static String TARGETFORMAT = "TARGETFORMAT";
final static String SOURCEFILE = "SOURCE";
final static String TARGETFILE = "TARGET";
final static String TARGETDTG = "TARGETDTG";
final static String SOURCEDTG = "SOURCEDTG";
final static String OUTTYPE = "OUTPUTTYPE";
final static String CONSOLE = "console";
final static String HTTP = "HTTP";
final static String FILE = "file";
final static String DEFAULTLOC= "DEFAULT-LOCATION";
final static String DEFAULTPROC= "DEFAULT-PROCESS";

final static String TIME = "%t";
final static String LOGLEVEL = "%l";
final static String LOCATION = "%c";
final static String MESSAGE = "%m";
final static String PROCESS = "%p";

final static String PROPFILENAMEDEFAULT = "tool.properties";

private boolean debugAll = false; // allows us to pretty print all the API calls if necessary

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

static String logToString (LogEntry log, String dtgFormat, String separator, String outTemplate)
{

            String output = null;

            if (outTemplate != null)
            {
                 output = outTemplate.clone();
            }
            else
            {
                output = TIME + separator + MESSAGE;
            }

            if (output.indexOf (TIME) > -1)
            {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dtgFormat);  
                LocalDateTime now = LocalDateTime.now();
                println (TIME + "   " + now.format(dtf));
                output = output.replace(TIME, now.format(dtf));
            }

            if (output.indexOf (LOGLEVEL) > -1)
            {
                output = output.replace(LOGLEVEL, log.logLevel);
            }

            if (output.indexOf (PROCESS) > -1)
            {
                output = output.replace(PROCESS, log.process);
            }            

            if (output.indexOf (LOCATION) > -1)
            {
                output = output.replace(LOCATION, log.location);
            }  

            if (output.indexOf (MESSAGE) > -1)
            {
                output = output.replace(MESSAGE, log.message);
            }

            return output;
}


static ArrayList<LogEntry> loadLogs (String source, String separator, String format)
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
                    System.out.println ("Unrecognized formatter code : " + formatArray[fmtIdx]);
                }       
                fmtIdx++;       
            }
            valueSet = false;
        }

        lines.add(logEntry);
        System.out.println (logEntry.toString());
        line = sourceReader.readLine();

    }

    return lines;
}

public void main (String[] args)
{

    String propFilename = PROPFILENAMEDEFAULT;
    Properties props = new Properties();

    // process the command line properties
    if (args.size() > 0)
    {
        if (args[0].equalsIgnoreCase("-h"))
        {
            System.out.println("Parameters needed are:\n" + HELPMSG);
        }

        if (args.size() > 0)
        {
            propFilename = args[0];
        }
    }
    else
    {
        System.out.println("going to use default properties file");
    }


    try
    {
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
        assert ((props.get(TARGETFILE) != null) && (props.get(TARGETFILE).size() > 0)): "SOURCE not defined";
        assert ((props.get(SOURCEFILE) != null) && (props.get(SOURCEFILE).size() > 0)): "TARGET not defined";
        assert ((props.get(SOURCEFORMAT) != null) && (props.get(SOURCEFORMAT).size() > 0)): "No formatting for output defined";

    }
    catch (AssertionError err)
    {
        if (debugAll)
        {
            System.out.println(err.getMessage());
            System.out.println(HELPMSG);

        }
        System.exit(-1);
    }

    if ((props.get(DEFAULTLOC) != null) && (props.get(DEFAULTLOC).length() > 0))
    {
        LogEntry.defaultLocation = props.get(DEFAULTLOC);
    }
    if ((props.get(DEFAULTPROC) != null) && (props.get(DEFAULTPROC).length() > 0))
    {
        LogEntry.defaultProcess= props.get(DEFAULTPROC);
    }    


    ArrayList<LogEntry> logs = loadLogs (props.get(SOURCEFILE), props.get (SOURCESEPARATOR), props.get (SOURCEFORMAT));
    LogEntry log = null;
    String dtgFormat = "HH:mm:ss";
    if ((props.get(TARGETDTG) != null) && (props.get(TARGETDTG).length() > 0))
    {
        dtgFormat = props.get(TARGETDTG);
    }

    while (true)
    {
        Iterator iter = logs.iterator();
        String separator = props.get (TARGETSEPARATOR);

        int outType = 0;
        if ((props.get(OUTTYPE)  != null) && (props.get(OUTTYPE).length() > 0))
        {
            if (props.get(OUTTYPE).equalsIgnoreCase(FILE))
            {
                outType = 1;
            }
            else if (props.get(OUTTYPE).equalsIgnoreCase(CONSOLE))
            {
                outType = 0;
            }
            else if (props.get(OUTTYPE).equalsIgnoreCase(HTTP))
            {
                outType = 2;
            }            
        }
        BufferedWriter writer = null;


        while (iter.hasNext())
        {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dtgFormat);  
            LocalDateTime now = LocalDateTime.now();

            log = (LogEntry) iter.next();
            String output =  logToString(log, dtgFormat, separator,  props.get(TARGETFORMAT));

            switch(outType) 
            {
                case 0: // console
                    System.out.println (output);
                break

                case 1: // file
                    if (writer == null)
                    {
                        writer = new BufferedWriter(new FileWriter(props.get(TARGETFILE), true));
                    }
                    writer.write(output+"\n");
                    writer.flush();
                break

                case 2: // HTTP
                    System.out.println (output);
                break
            }
            //System.out.println ("debug:" + output);

            if (log != null)
            {
                sleep (log.offset);
            }
        }

    }

    writer.close();

    System.exit(0);
}
}

    new LogSimulator().main(args);

