#!/bin/sh

for value in accepted-ip accepted-time invalid-ip invalid-time failed-ip failed-time reverseaddress-ip reverseaddress-time user-ip
do
   time java -jar build/libs/SSHCreateBTree.jar --cache=1 --degree=0 --sshFile=data/SSH_Files/SSH_log.txt --type=$value --size=10000 --debug=1
done
