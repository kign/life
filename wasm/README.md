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

## Implementation notes

(in progress)
