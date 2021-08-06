# Summary

This jar represents the process of taking a Groovy script and compiling it with any dependencies into the Jar making it simpler to deploy if you don't have Groovy available.



# Creating the Jar

The Jar used to be manually generated using groovyc however we have found a Groovy script that will complete the entire process for us, ensuring that that the dependencies are bundled into the jar as well. This is all curtesy of Dmitrijs Artjomenko (dmitart) through his GitHub repository at https://github.com/dmitart/scriptjar.  The tool documentation is at https://github.com/dmitart/scriptjar/blob/master/README.md

Adapting his instructions for our specific  The translation of this becomes:

1. Check that _GROOVY_HOME_ is correctly set to reference the Groovy installation location.
2. Download and Copy scriptjar.groovy to the root folder for the LogGenerator (same folder that has LogSimulator.groovy)
3. run the command _groovy scriptjar.groovy LogSimulator.groovy LogSimulator.jar_
4. run the command _java -jar LogSimulator.jar -h_ to confirm the tool responds correctly with the help information

