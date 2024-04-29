# MemTracePIN
Simple memory tracing tool that uses Intel PIN to intercepts every memory access and records timestamp since the program started and memory address accessed, as well as access type.

Relevant commands:

Compile:
```
cd MyPinTool && make clean && mkdir -p obj-intel64 && make TARGET=intel64 obj-intel64/main.so
```

Run:
```
setarch x86_64 -R $PIN_ROOT/pin -ifeellucky -t ./obj-intel64/main.so -- executable_name
```