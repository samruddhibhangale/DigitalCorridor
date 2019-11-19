# DigitalCorridor
Private blockchain application using corda
# Pre-Requisites

23.101.25.46

See https://docs.corda.net/getting-set-up.html.
---------------------------------------------------------
####### Step 1#
# Go to the project location- cd/
   - Ubuntu
         ./gradlew.bat clean deployNodes
   - Windows
         ./gradlew clean deployNodes
--------------------------------------------------------
####### Step 2#
# Go to the cd/build/nodes folder
   - Ubuntu
         ./runnodes
   - Windows
         ./runnodes.bat
---------------------------------------------------------
####### Step 3#
Build the boot JAR- run the task in
  cd  client/ build.gradlew

run the BOOT JAR task which will make the jar file at client/build/libs/corda-spring-webserver-0.1
---------------------------------------------------------
###### Step 4#
Bring up the corda nodes.
cd build/nodes/./runnodes
----------------------------------------------------------
####### Step 5#
#### Interacting with the webserver
cd Client/build/libs - Java -jar corda-spring-webserver-0.1



