#!/bin/sh

for value in accepted-ip accepted-time invalid-ip invalid-time failed-ip failed-time reverseaddress-ip reverseaddress-time user-ip
do
	echo
	echo "Running Search query on "SQUERY:"$value"25.txt""
  diff -w data/searchQueries/"SQUERY:"$value"25.txt" "results/squery-results/"SQUERY:"$value"25.txt""
  if test "$?" = "0"
  then
    echo "----> Test-$value PASSED!"
  fi
done
echo

