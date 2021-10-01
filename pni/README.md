# Python interface for Game of Life

Python wrapper for C implementation of Conway's Game of Life algorithm.

This only provides low-level utilities.

## Installation

Use PyPi package [conway-life](https://pypi.org/project/conway-life/)

```bash
python3 -m pip install --upgrade conway-life
```

## Design notes

None of the methods allocate any Python objects; output objects must be created by caller.
This both simplifies memory management and makes it possible to avoid unneeded
garbage collection for large objects.

## API

#### run(width, height, n_threads, n_iters, pos_start, pos_end, calllback)

`width`:       Width of the board; *integer*<br>
`height`:     Height of the board; *integer*<br>
`n_threads`:  Number of worker threads (not yet supported); *integer*<br>
`n_iters`:    Number of iterations to run; *integer*<br>
`pos_start`:  Initial position; boolean *list* of size `width` x `height`<br>
`pos_end`:    (Output) Final position; boolean *list* of size `width` x `height`. Must be pre-allocated.<br>
`callback`:   Iteration calllback, or `None` (see below)<br>

return value: number of actually executed iterations
          (never more than `n_iters`, but could be less, see below)<br>

**callback(n_iter, count, bhash, pos_ptr, fin)**

`n_iter`:     current iteration (see below); *integer*<br>
`count`:      count of cells; *integer*<br>
`bhash`:      hash of current position; *integer*<br>
`pos_ptr`:    internal memory pointer to the current position; *integer*.
                Method `read_ptr` can be used to extract the position<br>
`fin`:        1 if this is final iteration, 0 if not<br>

return value: `None` or *integer*; value 1 will trigger iterations to stop immediately.<br>

Notes

  1. `callback` (if defined) is called *before* first iteration, and then
again *after* every iterations, including the last (where fin=1). Therefore,
*normally* callback method is called `1 + n_iters` times.

  1. However this method also detects loops and if your sequence deteriorates
to a loop, it will cut it short; `fin` would still be set to 1 on the last
iteration only regardless. Hash values used in loop detection is passed
to the callback.

#### read_ptr(width, height, pos_ptr, position)

`width`:      Width of the board; *integer*<br>
`height`:     Height of the board; *integer*<br>
`pos_ptr`:    Internal memory pointer returned in a callback (see method `run`); *integer*<br>
`position`:   (Output) Intermediary position; boolean *list* of size `width` x `height`.
                Must be pre-allocated.<br>

return value: 1 on success
