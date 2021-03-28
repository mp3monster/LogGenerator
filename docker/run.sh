if test -f ./LogSimulator.groovy; then
  echo "got simulator"
else
  echo "getting log simulator"
  wget https://raw.githubusercontent.com/mp3monster/LogGenerator/master/LogSimulator.groovy
fi

if test -f /vol/conf; then 
  echo "copying /vol/conf for use"
  cp /vol/conf/*.conf .
  # need to find the conf file and rename
  cp -r ./*.conf ./default.conf
else
  rm -f default.properties source.txt
  wget https://raw.githubusercontent.com/mp3monster/LogGenerator/master/docker/default.properties
  wget https://raw.githubusercontent.com/mp3monster/LogGenerator/master/docker/source.txt
  echo "retrieved defaults"
fi
groovy --version
groovy ./LogSimulator.groovy default.properties
