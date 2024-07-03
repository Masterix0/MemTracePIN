# MemTracePIN
MemTracePIN is a memory tracing tool that uses Intel PIN to intercept every memory access and records the timestamp, memory address accessed, and access type.

## Output Format

The output is a CSV file with the following columns:
- **Timestamp**: Time since the program started
- **Access Type**: 'R' for Read or 'W' for Write
- **Address**: Memory address accessed

## Requirements

- Intel PIN
- C++ compiler

## Installation

1. **Download and Install Intel PIN**:

    Make sure the user has read/write access to the PIN installation. Define `PIN_ROOT` to ease the next steps:

    ```bash
    wget https://software.intel.com/sites/landingpage/pintool/downloads/pin-3.30-98830-g1d7b601b3-gcc-linux.tar.gz
    tar xzf pin-3.30-98830-g1d7b601b3-gcc-linux.tar.gz
    sudo mv pin-3.30-98830-g1d7b601b3-gcc-linux /opt
    export PIN_ROOT=/opt/pin-3.30-98830-g1d7b601b3-gcc-linux
    echo -e "\nexport PIN_ROOT=/opt/pin-3.30-98830-g1d7b601b3-gcc-linux" >> ~/.bashrc
    source ~/.bashrc
    ```

2. **Compile MemTracePIN**:

    Navigate to the MyPinTool directory and compile the tool:

    ```bash
    cd MyPinTool
    make
    ```

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


## Cleaning Up

To clean the build artifacts, use:

```bash
make clean
```

This will remove the `obj-intel64` directory, the `output` directory and the `sorted` directory.