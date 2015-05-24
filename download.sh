#!/bin/bash
echo ">> Downloading..."
wget --header="User-Agent: MonTransit" -i input_url -O input/gtfs.zip
echo ">> Downloading... DONE"