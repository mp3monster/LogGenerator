 class CustomConsoleOutputter implements LogGenerator.RecordLogEvent
 {
  public CustomConsoleOutputter(){}

    public initialize (Properties props, boolean verbose)    { System.out.println ("initializing ....")}

    public writeLogEntry(String entry)
    {
        System.out.println ("Custom loaded logger - " + entry);
    }
 }