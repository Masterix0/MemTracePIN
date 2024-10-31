# MemTracePIN

MemTracePIN is a memory tracing tool that utilizes [Intel® PIN](https://software.intel.com/content/www/us/en/develop/articles/pin-a-dynamic-binary-instrumentation-tool.html) to intercept and record every memory access made by an application during its execution.
It logs the timestamp, memory address accessed, and access type (read or write), allowing for detailed analysis of memory usage patterns.

This tool was developed as part of a master's thesis project to collect memory access traces for simulation of various page ranking mechanisms in heterogeneous memory systems.
It can be used by researchers and students interested in memory access patterns and memory hierarchy simulations.

## Table of Contents

- [Features](#features)
- [Output Format](#output-format)
- [Requirements](#requirements)
- [Installation](#installation)
  - [1. Download and Install Intel® PIN](#1-download-and-install-intel-pin)
  - [2. Clone and Compile MemTracePIN](#2-clone-and-compile-memtracepin)
- [Usage](#usage)
  - [Running the Tracer](#running-the-tracer)
  - [Example](#example)
- [Cleaning Up](#cleaning-up)
- [Troubleshooting](#troubleshooting)
- [Acknowledgments](#acknowledgments)

## Features

- Intercepts all memory accesses (reads and writes) made by an application.
- Records high-resolution timestamps for each memory access.
- Outputs data in CSV-like format (CSV without headers) for easy processing and visualization.
- Supports multi-threaded applications with per-thread trace files.
- Provides a script for sorting and cleaning up trace files.

## Output Format

The tracer outputs CSV-like files (in essence, CSV without headers) with the following format:

- **Timestamp**: High-resolution timestamp of when the memory access occurred.
- **Access Type**: `R` for Read or `W` for Write.
- **Address**: Memory address accessed (in hexadecimal).

Example line from the output:

```
1e946f69b5e2c7,R,7ffff7ffcef8
```

## Requirements

- **Operating System**: Linux
- **Intel® PIN**: Version 3.30 or compatible
- **C++ Compiler**: GCC supporting C++11 or higher
- **Make**
- **Bash Shell**
- **Standard Unix Tools**: `sort`, `mkdir`, `wget`, etc.

## Installation

### 1. Download and Install Intel® PIN

Intel® PIN is a dynamic binary instrumentation tool that MemTracePIN uses to intercept memory accesses.

1. **Download Intel® PIN**:

   Download the latest version of Intel® PIN (version 3.30 as of writing):

   ```bash
   wget https://software.intel.com/sites/landingpage/pintool/downloads/pin-3.30-98830-g1d7b601b3-gcc-linux.tar.gz
   ```

2. **Extract the Archive**:

   ```bash
   tar xzf pin-3.30-98830-g1d7b601b3-gcc-linux.tar.gz
   ```

3. **Move to `/opt` Directory** (Optional):

   You can move the extracted folder to `/opt` or any preferred location:

   ```bash
   sudo mv pin-3.30-98830-g1d7b601b3-gcc-linux /opt/
   ```

4. **Set `PIN_ROOT` Environment Variable**:

   Define `PIN_ROOT` in your shell to point to the PIN installation directory:

   ```bash
   export PIN_ROOT=/opt/pin-3.30-98830-g1d7b601b3-gcc-linux
   ```

   To make this permanent, add it to your `~/.bashrc` or `~/.profile`:

   ```bash
   echo '\nexport PIN_ROOT=/opt/pin-3.30-98830-g1d7b601b3-gcc-linux' >> ~/.bashrc
   source ~/.bashrc
   ```

   **Note**: Adjust the path if you installed PIN in a different location.

5. **Verify PIN Installation**:

   Check that PIN is correctly installed:

   ```bash
   $PIN_ROOT/pin -version
   ```

   You should see output similar to:

   ```
   Pin: pin-3.30-98830-1d7b601b3
   Copyright 2002-2023 Intel Corporation.
   ```

### 2. Clone and Compile MemTracePIN

1. **Clone the Repository**:

   If you haven't already, clone the MemTracePIN repository:

   ```bash
   git clone https://github.com/Masterix0/MemTracePIN
   ```

2. **Navigate to the Tool Directory**:

   ```bash
   cd MemTracePIN/MyPinTool
   ```

3. **Compile the Tool**:

   Run `make` to compile the PIN tool:

   ```bash
   make
   ```

   This will create the `obj-intel64` directory containing the compiled shared object `MyPinTool.so`.

   **Note**: Ensure that `PIN_ROOT` is correctly set, as the `Makefile` uses it to find the PIN headers and libraries.

## Usage

To run the tool, use the following command:

```bash
./run_and_sort.sh <executable_and_arguments>
```

Replace `<executable_and_arguments>` with the executable and arguments you want to trace.

This will run the tracer and save the output to the `output` directory.
It will then the trace execution time to terminal.
Finally, the output will be sorted and saved to the `sorted` directory, with the original files being deleted.

The output files will be sorted by timestamp, using the gnu/unix `sort` command.

## Usage

### Running the Tracer

To trace an executable, use the provided `run_and_sort.sh` script.
This script runs the tracer on the specified executable and then sorts the output files.

**Syntax**:

```bash
./run_and_sort.sh <executable_path> [executable_args...]
```

- `<executable_path>`: Path to the executable you wish to trace.
- `[executable_args...]`: Optional arguments to pass to the executable.

**What the Script Does**:

1. Runs the tracer on the specified executable using Intel® PIN.
2. Outputs per-thread trace files to the `output` directory.
3. Measures and outputs the tracing execution time to the terminal and `benchmark_time.txt`.
4. Sorts the trace files by timestamp.
5. Saves sorted files to the `sorted` directory.
6. Deletes the original unsorted trace files.

**Example**:

Suppose you have an executable `./my_program` that you want to trace with arguments `arg1` and `arg2`.

```bash
./run_and_sort.sh ./my_program arg1 arg2
```

### Example

1. **Prepare the Executable**:

   Ensure that the executable you want to trace is built and has execute permissions.

2. **Run the Tracer**:

   ```bash
   ./run_and_sort.sh ./my_program arg1 arg2
   ```

3. **Check the Output**:

   After the script completes, the sorted trace files will be in the `sorted` directory.

   ```bash
   ls sorted/
   ```

   You should see files named like `buffer.out.<pid>.<tid>`, where `<pid>` is the process ID and `<tid>` is the thread ID.

4. **Inspect the Trace Files**:

   You can view the contents of a trace file:

   ```bash
   less sorted/buffer.out.12345.0
   ```

   This will display a part of the memory accesses recorded for thread 0.
   Using `cat` isn't recommended as output files can have several GBs or TBs.

5. **Review Benchmark Time**:

   Execution time information is stored in `benchmark_time.txt`.

   ```bash
   cat benchmark_time.txt
   ```

### Notes:

- The `run_and_sort.sh` script should have execute permissions. If not, make it executable:

  ```bash
  chmod +x run_and_sort.sh
  ```

- The script uses `setarch x86_64 -R` to disable address space randomization, which can help with consistency in memory addresses.

## Cleaning Up

To remove compiled objects and output directories, use:

```bash
make clean
```

This will delete:

- `obj-intel64` directory (compiled objects)
- `output` directory (unsorted trace files)
- `sorted` directory (sorted trace files)

## Troubleshooting

- **`PIN_ROOT` Not Set or Incorrect**:

  If you get errors related to PIN headers or libraries not found during compilation, ensure that `PIN_ROOT` is correctly set.

- **Permissions**:

  Make sure you have read/write permissions to the directories and files used by the tool, especially if using `sudo` during installation.

- **Executable Not Running**:

  If the target executable fails to run under PIN, check if it requires special privileges or environment variables. Some applications may not be compatible with PIN.

- **Output Directories Not Created**:

  The script should create the `output` and `sorted` directories if they do not exist. If not, create them manually:

  ```bash
  mkdir -p output sorted
  ```

- **High Overhead**:

  Tracing every memory access can significantly slow down execution (usually a slowdown of 10,000 to 1,000,000 times).
  This makes it so a 2-5 second application run can take two weeks or more to complete.
  For long-running applications, consider tracing a subset or using sampling techniques.

- **Time Command Not Found**:

  The script uses the `time` command.
  If you receive an error that `time` is not found, you might need to install it or modify the script to use `/usr/bin/time`.

- **`setarch` Command Not Found**:

  The script uses `setarch`. If it's not installed, install it using:

  ```bash
  sudo apt-get install util-linux
  ```

## Acknowledgments

MemTracePIN was developed as part of a master's thesis project at [Instituto Superior Técnico, Universidade de Lisboa](https://tecnico.ulisboa.pt/en/). It aims to assist future students and researchers in the field of memory systems and performance analysis.

- **Author**: André Gonçalves
- **Contact**: [andreafonsogoncalves2001@gmail.com](mailto:andreafonsogoncalves2001@gmail.com)

We acknowledge the use of Intel® PIN for dynamic binary instrumentation.