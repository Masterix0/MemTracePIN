#include <iostream>
#include <fstream>
#include <cstdlib>
#include <cstddef>
#include <unistd.h>
#include "pin.H"
#include <chrono>

using std::cerr;
using std::endl;
using std::hex;
using std::ofstream;
using std::string;

// Define the number of buffer pages (4KB buffer)
#define NUM_BUF_PAGES 1024

/*
 * Name of the output file
 */
KNOB<string> KnobOutputFile(KNOB_MODE_WRITEONCE, "pintool", "o", "buffer.out", "output file");

/*
 * The ID of the buffer
 */
BUFFER_ID bufId;

/*
 * Record of memory references.  Rather than having two separate
 * buffers for reads and writes, we just use one struct that includes a
 * flag for type.
 */
struct MEMREF
{
    ADDRINT pc;      // The address of the instruction
    ADDRINT ea;      // The effective address
    char accessType; // 'R' for read, 'W' for write
    UINT64 timestmp; // Timestamp
};

/*
 * MLOG - thread specific data that is not handled by the buffering API.
 */
class MLOG
{
public:
    MLOG(THREADID tid);
    ~MLOG();

    VOID DumpBufferToFile(struct MEMREF *reference, UINT64 numElements, THREADID tid);

private:
    ofstream _ofile;
};

MLOG::MLOG(THREADID tid)
{
    const string filename = KnobOutputFile.Value() + "." + decstr(getpid()) + "." + decstr(tid);

    _ofile.open(filename.c_str());

    if (!_ofile)
    {
        cerr << "Error: could not open output file." << endl;
        exit(1);
    }

    _ofile << hex;
}

MLOG::~MLOG() { _ofile.close(); }

VOID MLOG::DumpBufferToFile(struct MEMREF *reference, UINT64 numElements, THREADID tid)
{
    for (UINT64 i = 0; i < numElements; i++, reference++)
    {
        if (reference->ea != 0)
        {
            _ofile << reference->pc << "," << reference->timestmp << "," << reference->accessType << "," << reference->ea << endl;
        }
    }
}

// Define the thread specific data key
TLS_KEY mlog_key;

/**************************************************************************
 *
 *  Instrumentation routines
 *
 **************************************************************************/

/*
 * Insert code to write data to a thread-specific buffer for instructions
 * that access memory.
 */
VOID Trace(TRACE trace, VOID *v)
{
    for (BBL bbl = TRACE_BblHead(trace); BBL_Valid(bbl); bbl = BBL_Next(bbl))
    {
        for (INS ins = BBL_InsHead(bbl); INS_Valid(ins); ins = INS_Next(ins))
        {
            if (!INS_IsStandardMemop(ins) && !INS_HasMemoryVector(ins))
            {
                // We don't know how to treat these instructions
                continue;
            }

            UINT32 memoryOperands = INS_MemoryOperandCount(ins);

            for (UINT32 memOp = 0; memOp < memoryOperands; memOp++)
            {
                UINT64 timestamp = std::chrono::high_resolution_clock::now().time_since_epoch().count(); // Get current timestamp using std::chrono

                /* Note that if the operand is both read and written we log it once
                 * for each.
                 * INS_InsertFillBuffer() allows us to insert multiple fields into the buffer
                 * in one call.  This is more efficient than calling INS_InsertFillBuffer()
                 * once for each field.
                 * Syntax is:
                 * INS_InsertFillBuffer(instruction being instrumented, action filled before or after instruction,
                 * bufferId, argument type, [optional IARG parameters like values],
                 * offset in bytes from the start of the trace record to this field,
                 * ..., IARG_END);
                 */
                if (INS_MemoryOperandIsRead(ins, memOp))
                {
                    INS_InsertFillBuffer(ins, IPOINT_BEFORE, bufId, IARG_INST_PTR, offsetof(struct MEMREF, pc),
                                         IARG_MEMORYOP_EA, memOp, offsetof(struct MEMREF, ea), IARG_UINT64, 'R', offsetof(struct MEMREF, accessType),
                                         IARG_UINT64, timestamp, offsetof(struct MEMREF, timestmp), IARG_END);
                }

                if (INS_MemoryOperandIsWritten(ins, memOp))
                {
                    INS_InsertFillBuffer(ins, IPOINT_BEFORE, bufId, IARG_INST_PTR, offsetof(struct MEMREF, pc),
                                         IARG_MEMORYOP_EA, memOp, offsetof(struct MEMREF, ea), IARG_UINT64, 'W', offsetof(struct MEMREF, accessType),
                                         IARG_UINT64, timestamp, offsetof(struct MEMREF, timestmp), IARG_END);
                }
            }
        }
    }
}

/**************************************************************************
 *
 *  Callback Routines
 *
 **************************************************************************/

VOID *BufferFull(BUFFER_ID id, THREADID tid, const CONTEXT *ctxt, VOID *buf, UINT64 numElements, VOID *v)
{
    struct MEMREF *reference = (struct MEMREF *)buf;

    MLOG *mlog = static_cast<MLOG *>(PIN_GetThreadData(mlog_key, tid));

    mlog->DumpBufferToFile(reference, numElements, tid);

    return buf;
}

VOID ThreadStart(THREADID tid, CONTEXT *ctxt, INT32 flags, VOID *v)
{
    MLOG *mlog = new MLOG(tid);

    PIN_SetThreadData(mlog_key, mlog, tid);
}

VOID ThreadFini(THREADID tid, const CONTEXT *ctxt, INT32 code, VOID *v)
{
    MLOG *mlog = static_cast<MLOG *>(PIN_GetThreadData(mlog_key, tid));

    delete mlog;

    PIN_SetThreadData(mlog_key, 0, tid);
}

/* ===================================================================== */
/* Print Help Message                                                    */
/* ===================================================================== */

INT32 Usage()
{
    cerr << "This tool demonstrates the basic use of the buffering API." << endl;
    cerr << endl
         << KNOB_BASE::StringKnobSummary() << endl;
    return -1;
}

/* ===================================================================== */
/* Main                                                                  */
/* ===================================================================== */
int main(int argc, char *argv[])
{
    if (PIN_Init(argc, argv))
    {
        return Usage();
    }

    bufId = PIN_DefineTraceBuffer(sizeof(struct MEMREF), NUM_BUF_PAGES, BufferFull, 0);

    if (bufId == BUFFER_ID_INVALID)
    {
        cerr << "Error: could not allocate initial buffer" << endl;
        return 1;
    }

    mlog_key = PIN_CreateThreadDataKey(0);

    TRACE_AddInstrumentFunction(Trace, 0);

    PIN_AddThreadStartFunction(ThreadStart, 0);
    PIN_AddThreadFiniFunction(ThreadFini, 0);

    PIN_StartProgram();

    return 0;
}
