#!/bin/bash
FILES="data/models/set1/*.arff"

for f in $FILES; do
    bname=$(basename "$f")
    filename="${bname%.*}"
    echo "Running generation for $filename"
    echo "@relation $filename\\n\\n" | cat - $f > /tmp/out && mv /tmp/out $f
done
