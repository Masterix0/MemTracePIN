#!/bin/bash

path=$(dirname $0)

# Check if at least one argument is provided
if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <executable_path> [executable_args...]"
    exit 1
fi

# Get the executable path and shift the arguments
executable_path=$1
shift
executable_args="$@"

# Define directories
input_dir="output"
sorted_dir="sorted"

# Create sorted directory if it doesn't exist
mkdir -p "$sorted_dir"

# Run the tracer and measure the time it takes
echo "Running tracer on $executable_path $executable_args"
time setarch x86_64 -R $PIN_ROOT/pin -ifeellucky -t $path/obj-intel64/MyPinTool.so -- "$executable_path" $executable_args

# Sort and delete files
echo "Sorting and deleting files in the output directory"

for file in "$input_dir"/*; do
    # Get the base name of the file
    base_name=$(basename "$file")
    
    # Define the sorted file path
    sorted_file="$sorted_dir/$base_name"
    
    # Sort the file and output to the sorted directory
    sort "$file" -o "$sorted_file"
    
    # Delete the original file
    rm "$file"
    
    echo "Sorted $file -> $sorted_file"
done

echo "All files sorted and originals deleted."
