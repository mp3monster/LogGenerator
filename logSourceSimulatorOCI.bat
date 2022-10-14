set CLASSPATH=./CustomOCIOutputter/CustomOCIOutputter.groovy;./lib/*;oci-java-sdk-full-2.46.0.jar
echo %CLASSPATH%
groovy  LogSimulator.groovy testConfigurations\tool-oci.properties

