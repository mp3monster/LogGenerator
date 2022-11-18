set CLASSPATH=./oci/lib/*;./oci/third-party/lib/*;.
echo %CLASSPATH%
groovy  LogSimulator.groovy testConfigurations\tool-oci-queue.properties

