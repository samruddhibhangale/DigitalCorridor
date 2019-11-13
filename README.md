# DigitalCorridor
Private blockchain application using corda
# Pre-Requisites

23.101.25.46

See https://docs.corda.net/getting-set-up.html.
---------------------------------------------------------
####### Step 1#
# Go to the project location- cd/Kale-poc
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

API calls:-
Create Transaction
POST - http://23.101.25.46:8080/amsAirport/createTransaction?otherParty=O=BOMAirport,L=Mumbai,C=IN
attache XML payload

Get ALL Transactions by it a ID
GET  -  http://23.101.25.46:8080/amsAirport/getAllTransactionsById?airwayBillNumber=0004

Get a Transaction(UNCONSUMED) by ID
GET  -  http://23.101.25.46:8080/bomAirport/getTransactionById?airwayBillNumber=0004

Get a Transaction details with msg type
GET  -  http://23.101.25.46:8080/amsAirport/getPayloadByMessageTypeNum?uniqueMessageTypeNum=f66a6dce-cfe2-42e1-88b1-5dda200fba98&airwayBillNumber=0004


