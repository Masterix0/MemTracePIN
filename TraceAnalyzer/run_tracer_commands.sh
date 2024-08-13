#!/bin/bash

# Input file with list of tracer commands
input_file="tracer_commands_list.txt"

# Output log file
output_log="tracer_output.log"

# Remove existing log file if it exists
if [ -f "$output_log" ]; then
    rm "$output_log"
fi

# Execute each command in the input file
while IFS= read -r cmd
do
    echo "Running command: $cmd" | tee -a "$output_log"
    eval "$cmd" 2>&1 | tee -a "$output_log"
    echo -e "\n" | tee -a "$output_log"
done < "$input_file"

echo "All commands executed. Logs written to $output_log."
