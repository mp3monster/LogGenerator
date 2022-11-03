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

class tests{
   static final boolean verbose = ((System.getenv("verbose") == null) || 
                                    (System.getenv("verbose").trim().equalsIgnoreCase("true")));

   static void log (String msg)
   {
      if (verbose) {
      System.out.println (msg)
      }
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

      }
      catch (Exception err)
      {
         log("caught:\n" + err.toString());
      }
   }

}