# Summary

This jar represents the process of taking a Groovy script and compiling it with any dependencies into the Jar making it simpler to deploy if you don't have Groovy available.



Originally we created the jar file manually using _groovyc_ and the jar command line. However a more elegant solution is available which includes ensuring that all the Groovy dependencies are bundled This is through the use of _scriptjar_ produced by Dmitrijs Artjomenko (dmitart) and available https://github.com/dmitart/scriptjar

The details of the tool are in the Readme (https://github.com/mp3monster/scriptjar#readme). The translation of this beciomes:

1. Copy scriptjar.groovy to the root folder for the LogGenerator (same folder that has LogSimulator.groovy)
2. run the command _groovy scriptjar.groovy LogSimulator.groovy LogSimulator.jar_
3. run the command _java -jar LogSimulator.jar -h_ to confirm the tool responds correctly with the help information

