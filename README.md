# MemTracePIN

MemTracePIN is a comprehensive toolkit designed for detailed analysis of memory access patterns in applications.
It utilizes [Intel® PIN](https://software.intel.com/content/www/us/en/develop/articles/pin-a-dynamic-binary-instrumentation-tool.html) for dynamic binary instrumentation to trace memory accesses and simulates various page-ranking mechanisms in heterogeneous memory systems.
This toolkit is particularly useful for researchers and students working on memory hierarchy simulations, memory tiering systems, and performance optimization in modern computing environments.

## Project Structure

The repository is organized as follows:

```
MemTracePIN/
├── MyPinTool/
│   ├── README.md
│   ├── [Source code and scripts for the tracer]
├── TraceAnalyzer/
│   ├── README.md
│   ├── [Source code and scripts for the analyzer]
└── README.md
```

- **MyPinTool/**: Contains the Intel® PIN tool for tracing memory accesses.
- **TraceAnalyzer/**: Contains the simulator for analyzing the memory traces.
- **README.md**: This file, providing an overview of the project.

## Overview

### MyPinTool

An Intel® PIN tool that traces every memory access made by an application, logging the timestamp, memory address, and access type (read or write).
It supports multi-threaded applications and outputs per-thread trace files for detailed analysis.

- **Functionality**:
  - Intercepts all memory accesses during application execution.
  - Records high-resolution timestamps.
  - Outputs data in a CSV-like format for easy processing.
- **Usage**: Detailed instructions are provided in [MyPinTool/README.md](MyPinTool/README.md).

### TraceAnalyzer

A Java-based simulator that processes the generated memory traces to simulate various page-ranking mechanisms over discrete intervals.
It calculates metrics such as DRAM hit ratios and provides detailed analysis to facilitate performance optimization in memory systems.

- **Simulated Page-Ranking Mechanisms**:
  - **Total Number of Accesses**: Ranks pages based on how frequently they are accessed.
  - **First Access Time**: Ranks pages based on when they are first accessed.
  - **Page Table Scan (PTS) Scoring**: Ranks pages using an approximation of PTS scoring over sub-intervals.
  - **MicroChronos**: Approximates the ranking based on an approximation of the MicroChronos algorithm, which approximates first access time using variable sub-intervals.

- **Output**:
  - Generates CSV files with metrics for each interval.
  - Provides overall DRAM hit ratios and variance calculations.
- **Usage**: Detailed instructions are provided in [TraceAnalyzer/README.md](TraceAnalyzer/README.md).

## Getting Started

To use MemTracePIN:

1. **Set Up Intel® PIN**: Install Intel® PIN, which is required by MyPinTool.
2. **Compile the Tools**: Follow the instructions in the dedicated READMEs to compile MyPinTool and TraceAnalyzer.
3. **Trace an Application**:
   - Use **MyPinTool** to collect memory access traces from your target application.
4. **Analyze the Traces**:
   - Use **TraceAnalyzer** to process the traces and simulate page-ranking mechanisms.
5. **Visualize Results**:
   - Optionally, use the provided Python scripts to generate graphs from the simulation results.

## Documentation

For detailed installation steps, usage examples, and troubleshooting, please refer to the dedicated READMEs:

- [MyPinTool/README.md](MyPinTool/README.md)
- [TraceAnalyzer/README.md](TraceAnalyzer/README.md)

## Contributing

Contributions are welcome!
If you have suggestions for improvements or encounter any issues, please open an issue or submit a pull request.

## Acknowledgments

MemTracePIN was developed as part of a master's thesis project at [Instituto Superior Técnico, Universidade de Lisboa](https://tecnico.ulisboa.pt/en/). It aims to assist future students and researchers in the field of memory systems and performance analysis.

- **Author**: André Gonçalves
- **Contact**: [andreafonsogoncalves2001@gmail.com](mailto:andreafonsogoncalves2001@gmail.com)

We acknowledge the use of Intel® PIN for dynamic binary instrumentation.