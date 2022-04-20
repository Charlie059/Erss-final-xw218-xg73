#!/bin/bash
for i in {1..10}
do
   python3 manage.py makemigrations
   python3 manage.py migrate
done
res="$?"
while [ "$res" != "0" ]
do
    sleep 3;
    python3 manage.py migrate
    res="$?"
done
