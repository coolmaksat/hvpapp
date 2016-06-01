#!/bin/bash
FILES="data/models/set1/*.arff"

for i in {0..9}; do
    echo "Running for $i"
    gradle run -PappArgs="['data/pgp/', '$i']"
done
