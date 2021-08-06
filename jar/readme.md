# Summary

This jar represents the process of taking a Groovy script and compiling it with any dependencies into the Jar making it simpler to deploy if you don't have Groovy available.



1. clear out any lingering class and jar files in the ./jar and ./classes folders
4. Generate Java class files from the groovy code: *groovyc -d classes LogSimulator.groovy* 
5. Package up the new classes into a Jar file : *jar cfe ./jar/logSimulator.jar LogSimulator -C ./classes 

The details of the tool are in the Readme (https://github.com/mp3monster/scriptjar#readme). The translation of this beciomes:

1. Copy scriptjar.groovy to the root folder for the LogGenerator (same folder that has LogSimulator.groovy)
2. run the command _groovy scriptjar.groovy LogSimulator.groovy LogSimulator.jar_
3. run the command _java -jar LogSimulator.jar -h_ to confirm the tool responds correctly with the help information

