/*
 * Simple test mechanism to ensure that as we make improvements we don't break the individual operations
 * can be run by setting environment variable called verbose tov be true to get all output e.g. 
 *     set verbose=true
 * The test are then run with the command:
 *    groovy LogSimulatorTest.groovy -enableassertion
 * note the flag needed to ensure the asserts work
 */

import java.util.logging.Level;
import LogSimulator;

class tests {
   static final boolean verbose = ((System.getenv("verbose") == null) || 
                                    (System.getenv("verbose").trim().equalsIgnoreCase("true")));

   static void log (String msg)
   {
      if (verbose) {
      System.out.println (msg)
      }
   }

   static void testLogToString ()
   {
      final String msg = "testing A B C";
      String dtgFormat = "";
      String separator = "!";
      String outTemplate = "(%j-%i) %m";
      boolean verbose = true;
      int counter = 2;
      int iterCount = 3;
      LogGenerator.testLogEntry.message = msg;
      
      String result = LogGenerator.logToString (LogGenerator.testLogEntry,  dtgFormat,  separator,  outTemplate, verbose,  counter,  iterCount);
      int strMsgIdx = result.indexOf(msg);
      int strIterCountIdx = result.indexOf(new String(iterCount));
      int strCounterIdx = result.indexOf(new String(counter));
                                 
      System.out.println ("testLogToString>" + result + " indexed at " + strIdx);
      assert (strIdx > -1) : "Missing message content";
      assert (strIterCountIdx > -1) : "Missing iteration counter content";
      assert (strCounterIdx > -1) : "Missing message counter content";
      
   }

   static void testJULMappings ()
   {
      try
      {
         Properties props = new Properties();
         props.setProperty("DEFAULT-LOGLEVEL", Level.INFO.toString());
         log ("testJULMappings prep'd");

         Level test = LogGenerator.toJULLevel (null, props);
         assert (test == Level.INFO) : "Default to INFO failed";

         test = LogGenerator.toJULLevel ("", props);
         assert (test == Level.INFO) : "Default to INFO failed";

         test = LogGenerator.toJULLevel ("warning", props);
         assert (test == Level.WARNING) : "Translate WARNING error";

         test = LogGenerator.toJULLevel ("severe", props);
         assert (test == Level.SEVERE) : "Translate SEVERE error";        

                   test = LogGenerator.toJULLevel ("fatal", props);
         assert (test == Level.SEVERE) : "Translate SEVERE (fatal) error";  

         test = LogGenerator.toJULLevel ("error", props);
         assert (test == Level.SEVERE) : "Translate SEVERE (error) error";  

         test = LogGenerator.toJULLevel ("info", props);
         assert (test == Level.INFO) : "Translate INFO error";                    

         test = LogGenerator.toJULLevel ("information", props);
         assert (test == Level.INFO) : "Translate INFO(rmation) error";    

         test = LogGenerator.toJULLevel ("config", props);
         assert (test == Level.CONFIG) : "Translate CONFIG error";        

         test = LogGenerator.toJULLevel ("fine", props);
         assert (test == Level.FINE) : "Translate FINE error";                                       

         test = LogGenerator.toJULLevel ("trace", props);
         assert (test == Level.FINE) : "Translate FINE (trace) error";  

         test = LogGenerator.toJULLevel ("finer", props);
         assert (test == Level.FINER) : "Translate FINER error";  

         test = LogGenerator.toJULLevel ("finest", props);
         assert (test == Level.FINEST) : "Translate FINEST error";           

      }
      catch (AssertionError err)
      {
         System.out.println ("JUL Mapping assertions failed\n" + err.toString());
      }

         System.out.println ("testJULMappings completed");

   }

  /*
   * invokes the required test methods
   */
   public static void main (String[] args)
   {
      try
      {
         log ("Starting tests ...");

         testJULMappings();
         testLogToString();

      }
      catch (Exception err)
      {
         log("caught:\n" + err.toString());
      }
   }

}