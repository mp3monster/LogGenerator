cd ..
rm -rf ./classes/*
rm logGenerator.jar
rm -rf ./jar/*.jar
groovyc -d classes LogSimulator.groovy
jar cfe ./jar/logSimulator.jar LogSimulator -C ./classes .

# now run with java -- java -jar logGenerator.jar
