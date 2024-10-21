#!/bin/sh

for value in accepted-ip accepted-time invalid-ip invalid-time failed-ip failed-time reverseaddress-ip reverseaddress-time user-ip
do
	echo
	echo "Checking db search query results "DB-QUERY-"$value".txt""
  	diff -w output/db-search/"DB-QUERY-"$value"-top25.txt" "results/db-search/"$value"-top25.txt""
  	if test "$?" = "0"
  	then
    	echo "----> Test-$value PASSED!"
	else
   		echo "----> Test-$value FAILED :( @#$%!"
  	fi
done
echo

