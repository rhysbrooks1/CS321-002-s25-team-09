#!/bin/sh

echo
echo "Checking your dump files against sample dump files..."
echo

for value in accepted-ip accepted-time invalid-ip invalid-time failed-ip failed-time reverseaddress-ip reverseaddress-time user-ip
do
	echo -n "Test-$value: Comparing /dumps/dump $value 0"
	diff -w output/dump-files/"dump-"$value".0.txt" results/dump-files/"dump-"$value".0.txt"
	if test "$?" = "0"
	then
		echo "----> Test-$value PASSED!"
	else
		echo "----> Test-$value FAILED :( @#$%!"
	fi
done
echo



