#!/bin/sh

for value in accepted-ip accepted-time invalid-ip invalid-time failed-ip failed-time reverseaddress-ip reverseaddress-time user-ip
do
   time java -jar build/libs/SSHSearchBTree.jar --cache=0 --degree=0 --btreefile=SSH_log.txt.ssh.btree.$value.0 --queryfile=QUERY:$value.txt --topfrequency=25 --size=10000 --debug=0
done
