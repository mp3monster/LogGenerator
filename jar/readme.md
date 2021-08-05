# Summary

This jar represents the process of taking a Groovy script and compiling it with any dependencies into the Jar making it simpler to deploy if you don't have Groovy available.

# Creation Process

The process by which this JAR is created as ....

1. clear out any lingering class and jar files in the ./jar and ./classes folders
4. Generate Java class files from the groovy code: *groovyc -d classes LogSimulator.groovy* 
5. Package up the new classes into a Jar file : *jar cfe ./jar/logSimulator.jar LogSimulator -C ./classes 



The process has been wrapped up in a bat  and .sh script called *package* in the folder LogEgenerator/jar

