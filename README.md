# MemTracePIN
Simple memory tracing tool that uses Intel PIN to intercepts every memory access and records timestamp and memory address accessed, as well as access type.

## Output Format

The output is a CSV file with the following columns:
- Timestamp: Time since the program started
- Address: Memory address accessed
- Access Type: Read or Write

## Requirements

- Intel PIN
- C++ compiler

## Installation

For Intel PIN, make sure the user has r/w access to the PIN installation and to ease the next steps define PIN_ROOT:

```
wget https://software.intel.com/sites/landingpage/pintool/downloads/pin-3.30-98830-g1d7b601b3-gcc-linux.tar.gz
tar xzf pin-3.30-98830-g1d7b601b3-gcc-linux.tar.gz
mv pin-3.30-98830-g1d7b601b3-gcc-linux /opt
export PIN_ROOT=/opt/pin-3.30-98830-g1d7b601b3-gcc-linux
echo -e "\nexport PIN_ROOT=/opt/pin-3.30-98830-g1d7b601b3-gcc-linux" >> ~/.bashrc
```

Now you're ready to compile TracerPIN and install it:

```
cd MyPinTool;
make clean && mkdir -p obj-intel64 && make TARGET=intel64 obj-intel64/main.so
```

## Usage

Run:
```
setarch x86_64 -R $PIN_ROOT/pin -ifeellucky -t ./obj-intel64/main.so -- executable_name
```