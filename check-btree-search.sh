#!/bin/sh

for value in accepted-ip accepted-time invalid-ip invalid-time failed-ip failed-time reverseaddress-ip reverseaddress-time user-ip
do
	echo
	echo "Running search query on "QUERY-"$value".txt""
  	diff -w output/btree-search/"QUERY-"$value".0.txt" "results/btree-search/"query-"$value".0.txt""
  	if test "$?" = "0"
  	then
    	echo "----> Test-$value PASSED!"
	else
    	echo "----> Test-$value FAILED :( @#$%!"
  	fi
done
echo

