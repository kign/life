/* This file is autogenerated with make_pydoc_h */

#define M_RUN "run"
#define RUN_DOC "run(width, height, n_threads, n_iters, pos_start, pos_end, calllback)\n" \
"\n" \
"    width:      Width of the board; integer\n" \
"    height:     Height of the board; integer\n" \
"    n_threads:  Number of worker threads (not yet supported); integer\n" \
"    n_iters:    Number of iterations to run; integer\n" \
"    pos_start:  Initial position; boolean list of size width*height\n" \
"    pos_end:    (Output) Final position; boolean list of size width*height. Must be pre-allocated.\n" \
"    callback:   Iteration calllback, or None (see below)\n" \
"\n" \
"    return value: number of actually executed iterations\n" \
"              (never more than n_iters, but could be less, see below)\n" \
"\n" \
"callback(n_iter, count, bhash, pos_ptr, fin)\n" \
"\n" \
"    n_iter:     current iteration (see below); integer\n" \
"    count:      count of cells; integer\n" \
"    bhash:      hash of current position; integer\n" \
"    pos_ptr:    (Output) Internal memory pointer to the current position; integer\n" \
"                    method `read_ptr` can be used to extract the position\n" \
"    fin:        1 if this is final iteration, 0 if not\n" \
"\n" \
"    return value: None or integer; value 1 will trigger iterations to stop immediately\n" \
"\n" \
"Notes: (1) callback (if defined) is called *before* first iteration, and then\n" \
"again *after* every iterations, including the last (where fin=1). Therefore,\n" \
"*normally* callback method is called (1 + n_iters) times.\n" \
"\n" \
"(2) However this method also detects loops and if your sequence deteriorates\n" \
"to a loop, it will cut it short; `fin` would still be set to 1 on the last\n" \
"iteration only regardless. Hash values used in loop detection is passed\n" \
"to the callback.\n"

#define M_READ_PTR "read_ptr"
#define READ_PTR_DOC "read_ptr(width, height, pos_ptr, position)\n" \
"\n" \
"    width:      Width of the board; integer\n" \
"    height:     Height of the board; integer\n" \
"    pos_ptr:    Internal memory pointer returned in a callback (see method `run`); integer\n" \
"    position:   (Output) Intermediary position; boolean list of size width*height.\n" \
"                    Must be pre-allocated.\n" \
"    return value: 1 on success\n"

