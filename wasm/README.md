# Life implementation in native Web Assembly

## Live demo

[https://kign.github.io/life/life-web.html](https://kign.github.io/life/life-web.html)

## Development

To compile the source file, you'll need `wat2wasm` (on Mac, it is part of Homebrew package `wabt`, [list of utilities included](https://github.com/WebAssembly/wabt))

```bash
wat2wasm life.wat --enable-bulk-memory -o ../docs/life.wasm
```

Note that for security reasons browsers won't load Web Assembly locally (that is, with `file:///` protocol).
Thus, to test locally, you need to serve generated file `life.wasm` via a local web server.
Script [run_local.py](https://github.com/kign/life/blob/master/wasm/run_local.py) can do it for you with a Flask server

```bash
python3 -m pip install --upgrade flask inetlab
FLASK_ENV=development wasm/run_local.py
```

## Performance comparison

We compare computational performance of native C code and three Web Assembly runtimes:
`node.js` (also used in Chrome browser and its derivatives),
[`wasmtime`](https://bytecodealliance.github.io/wasmtime-py/) and
[`wismer`](https://wasmerio.github.io/wasmer-python/api/wasmer/).
The test is done with 1000 iterations on 1200x1600 board, using
Node script [perf_test](https://github.com/kign/life/blob/master/wasm/perf_test)
for `node.js` runtime and
[perf_test.py](https://github.com/kign/life/blob/master/python/perf_test.py)
for everything else:

```bash
python/perf_test.py -s 12345 -i 1000 native 1200x1600
python/perf_test.py -s 12345 -i 1000 wasmtime 1200x1600
python/perf_test.py -s 12345 -i 1000 wasmer 1200x1600
wasm/perf_test -s 12345 -i 1000 1200x1600
```

(See notes below on `wasmer` testing)

The unit is "frames per second" (larger is better); all tests were
run on almost identical code with nearly identical setup<sup>1</sup>.

 Runtime    | FPS | Notes
 ---------- | --- | -------------
 Native     | 120 | Included for completeness; algorithm is basically the same
 `node.js`  | 90  | Appears to be very sensitive to tiny changes in the code. Could be made to run even faster (almost at native speed!) by removing cell counter and using `memory.fill`
 `wasmtime` | 55  | Slowest, but 100% compatible with `node.js`
 `wasmer`<sup>2</sup> | 76  | Necessitates some workarounds to run<sup>3</sup>

![Performance comparison chart](https://github.com/kign/life/blob/master/etc/wasm_runtimes.png?raw=true "Performance comparison chart" )


**NOTES**

  1. Random field generation is different between Python and Node;
  `wasmer` was tested with exported memory (should have no impact on performance).
  2. We are using `wasmer` in `LLVM` mode; there are also much slower `JIT` and
  `cranelift` options;
  3. `wasmer` doesn't support bulk memory operations (we are using `Memory.fill`
  for small optimization), therefore direct comparison of the same compatible
  version puts other runtimes at a small disadvantage. Additionally,
  current version of `wasmer` fails to handle memory import correctly;
  this has no bearing on performance per se, but necessitates a special
  version to use on `wasmer`.


**CONCLUSIONS**

  * `node.js` runtime is amazingly fast; with proper fine tuning, it could be made
to run at about 80% of native code speed; unfortunately, it doesn't have bindings
to other programming languages, such as Python, and thus can only be used from
JavaScript code via `node.js` or in browser;

  * `wasmtime` is the slowest, but well documented with all modern features
fully supported; generally, consistently running at about half the native speed isn't bad.

  * `wasmer` is clearly faster than `wasmtime` and not that far behind `node.js`,
but quality might be questionable; some features are not supported, some not working,
documentation on occasions is not up to date.

  See also: [Benchmark of WebAssembly runtimes - 2021 Q1](https://00f.net/2021/02/22/webassembly-runtimes-benchmarks/).

## Implementation notes

(in progress)
