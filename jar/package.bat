groovyc -d classes LogSimulator.groovy
dir
jar cfe .\\jar\\logSimulator.jar LogSimulator -C .\\classes .

echo Cleaning up
cd classes
del /f *.class
cd ..
REM del logGenerator.jar

REM now run with java -- java -jar logGenerator.jar
