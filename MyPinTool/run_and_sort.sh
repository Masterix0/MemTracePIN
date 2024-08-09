#!/bin/bash

# Set the path to the directory containing the script
path=$(dirname "$0")

# Check if at least one argument is provided
if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <executable_path> [executable_args...]"
    exit 1
fi

# Get the executable path and shift the arguments
executable_path="$1"
shift
executable_args="$@"

# Define directories
input_dir="output"
sorted_dir="sorted"

# Create sorted directory if it doesn't exist
mkdir -p "$sorted_dir"

# Define the benchmark output file
benchmark_file="benchmark_time.txt"

# Function to run the tracer and measure time
run_tracer() {
    echo "Running tracer on $executable_path $executable_args"

    # Use the 'time' command with format specifiers
    # %e: Real elapsed time (in seconds)
    # %U: User CPU time used (in seconds)
    # %S: System CPU time used (in seconds)
    # Redirect the time output to both terminal and benchmark_file

    { time setarch x86_64 -R "$PIN_ROOT/pin" -ifeellucky -t "$path/obj-intel64/MyPinTool.so" -- "$executable_path" $executable_args; } 2> >(tee -a "$benchmark_file")
}

# Function to sort and delete files
sort_and_cleanup() {
    echo "Sorting and deleting files in the output directory"

    for file in "$input_dir"/*; do
        # Ensure that we are processing files only
        if [ -f "$file" ]; then
            # Get the base name of the file
            base_name=$(basename "$file")

            # Define the sorted file path
            sorted_file="$sorted_dir/$base_name"

            # Sort the file and output to the sorted directory
            sort "$file" -o "$sorted_file"

            # Delete the original file
            rm "$file"

            echo "Sorted $file -> $sorted_file"
        fi
    done

    echo "All files sorted and originals deleted."
}

# Main execution
run_tracer
sort_and_cleanup
