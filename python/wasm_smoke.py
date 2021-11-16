#! /usr/bin/env python3
import sys, os.path
from datetime import datetime
from wasmtime import Store, Module, Instance, Func, FuncType, ValType, \
        Memory, MemoryType, Limits
import lifeutils

def test () :
    store = Store()

    if len(sys.argv) >= 2 :
        life_wasm = sys.argv[1]
    else :
        life_wasm = os.path.realpath(os.path.join(os.path.dirname(os.path.realpath(__file__)), '..', 'docs', 'life.wasm'))

    print("WASM file :", life_wasm)
    print("Last modi :", datetime.fromtimestamp(os.path.getmtime('lifeutils.py')).strftime("%Y-%m-%d %H:%M:%S"))
    module = Module.from_file(store.engine, life_wasm)
    mem = Memory(store, MemoryType(Limits(5, 5)))

    def log(param1, param2) :
        print(f"[{param1}] {param2}")

    p_cnt = [1]  # unlike conway-life, we don't invoke callback before 1-st iteration
    def callback(X, Y, liter, cnt, hash) :
        assert p_cnt[0] == liter, f"cnt = {p_cnt[0]}, liter = {liter}"
        p_cnt[0] += 1

        cur = [0] * (X * Y)
        for idx in range(X * Y) :
            cur[idx] = 1 if mem.data_ptr(store)[4 * idx] == 1 else 0

        if liter == 50 :
            s_cur = lifeutils.savet(X, Y, cur)
            if s_cur != """\
.x........
..........
.xxx......
xxx.x....x
.....x...x
x.xx.x....
x.........
..x......x
..x.......
x.x.......
""" :
                print("FAILED, intermediary value\n", s_cur, file=sys.stderr, sep='', end='')
                exit(1)

        return 0

    log_obj = Func(store, FuncType([ValType.i32(), ValType.i32()], []), log)
    callback_obj = Func(store,
        FuncType([ValType.i32(), ValType.i32(), ValType.i32(), ValType.i32(), ValType.i32()],
            [ValType.i32()]), callback)

    instance = Instance(store, module, [log_obj, callback_obj, mem])
    life_run = instance.exports(store)["run"]

    X, Y, start = lifeutils.readt("""\
..........
......x...
....xxx...
.....x....
..........
..........
..........
..........
..........
..........
""")

    for idx in range(X * Y) :
        mem.data_ptr(store)[4 * idx] = start[idx]

    res = life_run(store, X, Y, 100)
    assert res == 100

    end = [0] * (X * Y)
    for idx in range(X * Y) :
        end[idx] = 1 if mem.data_ptr(store)[4 * idx] == 1 else 0

    s_end = lifeutils.savet(X, Y, end)
    print(s_end, end='')

    assert s_end == """\
..........
..x.......
.x.x......
.x..x.x...
.xxxxx.x..
.xx..xx...
....x.....
....x.....
..........
..........
"""

    print("Passed")


if __name__ == "__main__" :
    test()
