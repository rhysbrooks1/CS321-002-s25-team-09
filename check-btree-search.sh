#!/bin/sh

for value in accepted-ip accepted-time invalid-ip invalid-time failed-ip failed-time reverseaddress-ip reverseaddress-time user-ip
do
	echo "Checking search query on "QUERY-"$value".txt""
	dos2unix output/btree-search/"QUERY-"$value".0.txt"
  	diff -w output/btree-search/"QUERY-"$value".0.txt" "results/btree-search/"query-"$value".0.txt""
  	if test "$?" = "0"
  	then
    	echo "----> Test-$value PASSED!"
	else
    	echo "----> Test-$value FAILED!"
  	fi
done
echo

for value in accepted-ip accepted-time invalid-ip invalid-time failed-ip failed-time reverseaddress-ip reverseaddress-time user-ip
do
	echo "Checking search query for top 25 frequencies on "QUERY-"$value".txt""
	dos2unix output/btree-search/"QUERY-"$value"-top25.0.txt"
  	diff -w output/btree-search/"QUERY-"$value"-top25.0.txt" "results/db-search/"$value"-top25.txt""
  	if test "$?" = "0"
  	then
    	echo "----> Test-$value for top 25 PASSED!"
	else
    	echo "----> Test-$value for top 25 FAILED!"
  	fi
done
echo
