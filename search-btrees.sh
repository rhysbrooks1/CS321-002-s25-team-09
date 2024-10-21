#!/bin/sh

for value in accepted-ip accepted-time invalid-ip invalid-time failed-ip failed-time reverseaddress-ip reverseaddress-time user-ip
do
   time java -jar build/libs/SSHSearchBTree.jar --cache=1 --degree=0 --btree-file=output/btrees/SSH_log.txt.ssh.btree.$value.0 --query-file=QUERY-$value.txt --topfrequency=25 --cache-size=10000 --debug=0 > output/btree-search/"QUERY-"$value".0.txt"
done
