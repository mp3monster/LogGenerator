 /*
  * this is the most vanilla implementation of a Custom Outputter
 */
 class CustomConsoleOutputter implements LogGenerator.RecordLogEvent
 {
  public CustomConsoleOutputter(){}

    public void initialize (Properties props, boolean verbose)    
    { 
      System.out.println ("initializing custom console outputter ....")
    }

    public void writeLogEntry(String entry)
    {
        System.out.println ("Custom console logger event - " + entry);
    }

    public void clearDown ()
    {
      System.out.println ("...clearing down custom console outputter")
    }
 }