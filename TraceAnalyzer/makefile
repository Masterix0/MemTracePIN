# Makefile for TraceAnalyzer

# Directories
SRC_DIR = src
BIN_DIR = bin

# Compiler
JC = javac

# Source files
SOURCES = $(wildcard $(SRC_DIR)/*.java)
# Compiled files
CLASSES = $(SOURCES:$(SRC_DIR)/%.java=$(BIN_DIR)/%.class)

output: $(CLASSES)
	mkdir -p output

# Targets
all: $(CLASSES) output

$(BIN_DIR)/%.class: $(SRC_DIR)/%.java | $(BIN_DIR)
	$(JC) -d $(BIN_DIR) $(SOURCES)

$(BIN_DIR):
	mkdir -p $(BIN_DIR)

clean:
	rm -rf $(BIN_DIR)
	rm -rf output

.PHONY: all clean
