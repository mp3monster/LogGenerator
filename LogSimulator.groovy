import java.util.StringTokenizer;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;    
import java.xml.*;
import java.net.ServerSocket;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// this utility will either delete the nominated node from the API management logical OR
// approve a pending gateway join. The behaviour is dictated by the parameters

//public class LogSimulator 
public class LogGenerator 

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
    final static int SYSSTD = 5;
    final static String SYSSTDOUT="STDOUT";
    final static int SYSERR = 6;
    final static String SYSERROUT="ERROUT";
    final static String ALLOWNL="ALLOWNL";
    final static String FIRSTOFMULTILINEREGEX="FIRSTOFMULTILINEREGEX"



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
    private static boolean debug = false; // these log messages are for debugging only

    private Logger juLogger = null;


    class LogSimulatorException extends Exception
    {
    }

   /**
    * This class holds the parsed log entry to be used
    */
    static class LogEntry 
    {
        public static defaultLogLevel = "";
        public static defaultProcess = "";
        public static defaultLocation = "";

        public int offset = 0; // time in millis
        public String logLevel = defaultLogLevel; // string representing the log level
        public String process = defaultProcess; // presents the process name or thread
        public String location = defaultLocation; // class path etc
        public String message = ""; // core message

        /**
         * Used for inspacting the log values held
        */
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
            else if (propOut.equalsIgnoreCase(SYSSTDOUT))
            {
                outType = SYSSTD;
            }     
            else if (propOut.equalsIgnoreCase(SYSERROUT))
            {
                outType = SYSERR;
            }                       
            else
            {
                if (verbose){System.out.println("Unknown output type :" + props.get(OUTTYPE));}
            }        
        }   

        return outType;
    }

    /**
     * Takes the log event elements and builds the output using the formatting template
     */
    static String logToString (LogEntry log, String dtgFormat, String separator, String outTemplate, 
                                boolean verbose, int counter, int iterCount)
    {

        String output = null;

        if (log == null)
        {
            if (debug) {System.out.println ("logToString - no log object" );}
            return "";
        }

        if (outTemplate != null)
        {
            output = new String(outTemplate);
        }
        else
        {
            output = TIME + separator + MESSAGE;
        }

        if (debug) {System.out.println ("logToString>"+iterCount + "<>" + counter + "<>" + output + "<>" + dtgFormat + "<\n log>   " + log +"<----------" );}

        if (output.indexOf (TIME) > -1)
        {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dtgFormat);  
            LocalDateTime now = LocalDateTime.now();
            if (debug) {System.out.println (TIME + " == " + now.format(dtf));}
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

    static String displayTokens(StringTokenizer tokens)
    {
        String output = "";
        int elementCtr = 1;

        if (tokens == null)
            {return "tokens is null object"}

        while (tokens.hasMoreElements())
        {
            output = output + "elem " + elementCtr + ">" + (String)tokens.nextElement() + "<; ";
            if (false) // if the token string should be multiline set to true
            {output = output + "\n"}

            elementCtr++;
        }

        return output;
    }

    static LogEntry createLogEntry (String line, String[] formatArray, String separator, boolean verbose)
    {
        LogEntry aLogEntry = new LogEntry();

        if ((line == null) || (line.length() == 0))
        {
            if (verbose) {System.out.println ("createLogEntry - empty line");} 
            return null;
        }

        StringTokenizer st = new StringTokenizer(line, separator); // why does it fail when we pass in the separator

        int fmtIdx = 0;
        String element = null;
        boolean valueSet = false;
        if (debug) 
        {
            System.out.println ("createLogEntry token count>" + st.countTokens());
            System.out.println ("createLogEntry Line>" + line + "<\n"+displayTokens(st)+"<--");
        }


        while (st.hasMoreElements())
        {
            element = (String)st.nextElement();

            if (debug) {System.out.println ("createLogEntry Line>" + fmtIdx + "< >" + formatArray.length+"<" + " " + valueSet);}                

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
                        if (verbose) {System.out.println ("createLogEntry - need to convert to offset");}                        
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
            if (verbose){System.out.println ("entry ==>" + aLogEntry.toString());}
            return aLogEntry;
    }

    static String mergeToString (ArrayList<String> staging, boolean verbose = false, boolean allowNL = false)
    {
        Iterator iter = staging.iterator();
        String mergeStr = null;

        while (iter.hasNext())
        {
            if (mergeStr == null)
            {
                if (debug){System.out.println ("mergeToString start string");};
                mergeStr = iter.next();
            }
            else
            {
                if (debug){System.out.println ("mergeToString EXTEND string");};
                mergeStr = mergeStr + "\n" + iter.next();
            }
        }

        if (allowNL)
        {
            mergeStr = mergeStr.replace ('\\n', '\n');
        }

        if (debug){System.out.println ("mergeToString result >>>>"+mergeStr+"<<<<");}
        return mergeStr;
    }

    static ArrayList<LogEntry> simpleRead (BufferedReader sourceReader, String separator, String[] formatArray, boolean verbose, boolean allowNL)
    {
        ArrayList<LogEntry> lines = new ArrayList<LogEntry> ();

        if (verbose){System.out.println ("simpleRead>"+separator+"<\n" + formatArray+ "\n" + allowNL);}

        String line = sourceReader.readLine();
        while (line != null)
        {
            if (allowNL)
            {
                line = line.replace ('\\n', '\n');
            }    
            LogEntry log = createLogEntry (line, formatArray, separator, verbose);

            if (log != null)
            {
                lines.add(log);
            }
            else
            {
                if (verbose){System.out.println ("simpleRead rec'd null line entry - ignoring");}
            }
            line = sourceReader.readLine();
        }      

        return lines;  
    }

    static ArrayList<LogEntry> multiLineRead (BufferedReader sourceReader, 
                                                String separator, 
                                                String[] formatArray, 
                                                boolean verbose, 
                                                String regex, 
                                                boolean allowNL)
    {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = null;
        ArrayList<String> lines = new ArrayList<String> ();
        ArrayList<String> staging = new ArrayList<String> ();
        String line = sourceReader.readLine();
        Boolean foundNewLogLine = false;

        while (line != null)
        {
            matcher = pattern.matcher(line);
            foundNewLogLine =matcher.find();
            if (verbose){System.out.println ("foundNewLogLine="+foundNewLogLine);}

            if (foundNewLogLine)
            {
                if (debug){println ("new line identified, staging is " + staging.size());}
                
                if (!staging.isEmpty())
                {
                    LogEntry log = createLogEntry (mergeToString (staging, verbose, allowNL), formatArray, separator, verbose);
                    if (log != null)
                    {
                        lines.add (log);
                        if (verbose){System.out.println ("multiLineRead - added log");}
                    }
                    else
                    {
                        if (verbose){System.out.println ("multiLineRead - rec'd a null log entry ignoring");}

                    }

                    staging.clear();
                }

                staging.add(line);

                if (debug){System.out.println ("adding (1)>"+line+"< to staging");}

            }
            else
            {
                staging.add(line);
                if (debug){System.out.println ("adding (2)>"+line+"< to staging")};
            }

            line = sourceReader.readLine();
        }

        if (!staging.isEmpty())
        {
            if (verbose){System.out.println ("final merge")};
            String merged = mergeToString (staging, verbose, allowNL);
          
            lines.add (createLogEntry (merged, formatArray, separator, verbose));
        }

        return lines;
    }

    static ArrayList<LogEntry> loadLogs (String source, 
                                        String separator, 
                                        String format, 
                                        boolean verbose, 
                                        String multiLineREGEX, 
                                        boolean allowNL)
    {

        BufferedReader sourceReader = new BufferedReader(new FileReader(source));  //creates a buffering character input stream  
        ArrayList<LogEntry> lines = null;

        String[] formatArray = format.split (" ");
        for (int idx = 0; idx < formatArray.length; idx++)
        {
            formatArray[idx] = formatArray[idx].trim();
        }


        if (verbose){System.out.println ("multiline="+multiLineREGEX)}
        if (multiLineREGEX == null)
        {
            lines = simpleRead (sourceReader, separator, formatArray, verbose, allowNL);
        }
        else
        {
            lines = multiLineRead (sourceReader, separator, formatArray, verbose, multiLineREGEX, allowNL);
        }

        return lines;
    }

    public boolean getPropAsBoolean (Properties props, String propName)
    {
        boolean property = false;

        if (props == null)
        {
            return false;
        }

        if ((props.get(propName) != null) && (props.get(propName).equalsIgnoreCase("true")))
        {
            property=true;
        }
        else
        {
            property = false;
        }

        return property;
    }

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
                sourceSeparator = props.get (SOURCESEPARATOR);
            }
            if ((targetSeparator == null) || (targetSeparator.size() == 0))
            {
                props.put (TARGETSEPARATOR, " ");
                targetSeparator = props.get (TARGETSEPARATOR);
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
                if (verbose) {System.out.println ("Replay acceleration by " + accelerationFactor);}
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

        ArrayList<LogEntry> logs = loadLogs (props.get(SOURCEFILE), 
                                            props.get (SOURCESEPARATOR), 
                                            props.get (SOURCEFORMAT), 
                                            verbose, 
                                            props.get(FIRSTOFMULTILINEREGEX), 
                                            getPropAsBoolean(props, ALLOWNL));

        if (verbose) {System.out.println ("Logs now loaded");}


        LogEntry log = null;
        String dtgFormat = "HH:mm:ss";
        if ((props.get(TARGETDTG) != null) && (props.get(TARGETDTG).length() > 0))
        {
            dtgFormat = props.get(TARGETDTG);
            if (verbose) {System.out.println ("Date time format " + dtgFormat);}
        }

        int loopTotal = 1;
        int loopCount = 0;
        if (props.get(LOOP) != null)
        {
            try
            {
                loopTotal = Integer.parseInt(props.get(LOOP));
                if (verbose) {System.out.println ("Number of loops set is " + loopTotal);}
            }
            catch (NumberFormatException err)
            {
                System.out.println ("Couldn't process loop counter >" + props.get(LOOP)+"<");
            }
        }

        try
        {
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
                                assert ((props.get(TARGETFILE) != null) && (props.get(TARGETFILE).size() > 0)): "No target file for output defined";
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

                            boolean sent = false;
                            boolean errCaught = false;

                            while (!sent)
                            {
                                try{                        
                                    webConnection.with {
                                        outputStream.withWriter { writer ->  writer << output }
                                        outputStream.flush();
                                    }
                                    String response= webConnection.getContent();
                                    sent = true;

                                    if (errCaught && verbose)
                                    {
                                    System.out.println ("Connection resolved, event sent");
                                    }
                                }
                                catch (Exception err)
                                {
                                    if (verbose) {System.out.println ("Err - try again in a a moment \n"+err.toString())}
                                    sleep (100);
                                    errCaught = true;
                                }
                            }
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
                                juLogger.log (toJULLevel(log.logLevel, props), log.location,"", output);
                            }
                            catch (Exception err)
                            {
                                if (verbose) {System.out.println ("Failed to log " + log.toString());}
                            }

                        break;

                        case SYSSTD:
                            System.out.println (output);
                        break;

                        case SYSERR:
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
        }
        catch (AssertionError err)
        {

            System.out.println(err.getMessage());
            System.out.println(HELPMSG);

            System.exit(-1);
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
}
    public static void main (String[] args)
    {
        try{
            new LogGenerator().core(args); 
            System.out.println ("bye")

        }
        catch (Exception err)
        {
            System.out.println ("oh")
            System.out.println (err)
            System.out.println (err.getStackTrace().toString())
        }
    }



 //LogGenerator().core(args);