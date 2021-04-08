cd ..
del ./classes/*
del logGenerator.jar
del ./jar/*.jar
groovyc -d classes LogSimulator.groovy
jar cfe ./jar/logSimulator.jar LogSimulator -C ./classes .

REM now run with java -- java -jar logGenerator.jar
