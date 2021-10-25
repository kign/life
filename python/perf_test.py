#! /usr/bin/env python3
import os.path, argparse, logging, random, re, time
import matplotlib.pyplot as plt
from inetlab.cli.colorterm import add_coloring_to_emit_ansi

# perf_test.py 1200x1600 --density-graph
# [1200x1600] Computed 1000 iterations in 9.020 seconds @ 110.864 fps

# perf_test.py -s 12345 -i 1000 c 1200x1600
# [1200x1600] Computed 1000 iterations in 8.552 seconds @ 116.932 fps
# perf_test.py -s 12345 -i 1000 wasm 1200x1600
# Computed 1000 iterations in 17.682 seconds @ 56.555 fps

expected_speed = 4.5e-9

def get_args () :
    default_log_level = "debug"
    default_density = 0.3
    default_iterations = 1_000

    parser = argparse.ArgumentParser(description="Performance/density testing of Life")
    parser.add_argument('--log', dest='log_level', help="Logging level (default = %s)" % default_log_level,
                        choices=['debug', 'info', 'warning', 'error', 'critical'],
                        default=default_log_level)

    parser.add_argument('-d', '--density', type=float,
        help=f"density for randomized board generation (default = {default_density:0.4f})",
        default=default_density)
    parser.add_argument('-s', '--seed',
        help="Seed for randomized board generation (if omitted results will be different every time)")
    parser.add_argument('-i', '--iterations', type=int,
        help=f"Number of iterations (default = {default_iterations})",
        default=default_iterations)
    parser.add_argument('--density-graph', action='store_true', help="Display density graph",
        dest='density_graph')

    parser.add_argument('runtime',
        help="Use 'c' for C-based conway-life package, or 'wasm' for Web Assembly based engine",
        choices=["c", "wasm"])
    parser.add_argument('size_a', nargs='+', help="board size, e.g. 1200x1600",
        metavar="WIDTHxHEIGHT")

    args = parser.parse_args ()
    logging.basicConfig(format="%(asctime)s.%(msecs)03d %(filename)s:%(lineno)d %(message)s",
                        level=getattr(logging, args.log_level.upper(), None))
    logging.getLogger('matplotlib.font_manager').setLevel(logging.INFO)

    logging.StreamHandler.emit = add_coloring_to_emit_ansi(logging.StreamHandler.emit)

    if not (0 < args.density < 1) :
        logging.error("Invalid density value %f, must be between 0 and 1", args.density)
        exit(1)

    random.seed(args.seed)

    if not (0 < args.iterations < 1_000_000_000) :
        logging.error("Invalid number of iterations %d", args.iterations)
        exit(1)

    return args

class Common :
    def __init__(self, X, Y, iters) :
        self.X = X
        self.Y = Y
        self.iters = iters
        self.dens_a = [None] * iters

class CRun(Common) :
    speed = 1.0

    def __init__(self, X, Y, iters, density) :
        import conway_life

        super().__init__(X, Y, iters)

        self.Fin = [False] * X * Y
        self.Fout = [False] * X * Y

        self.life_run = conway_life.run

        for idx in range(X * Y):
            self.Fin[idx] = random.random() < density

    def run(self, use_callback) :
        def callback(liter, count, lhash, f, fin) :
            if liter > 0 :
                self.dens_a[liter - 1] = count / self.X / self.Y
            return 0
        return self.life_run(self.X, self.Y, 1, self.iters, self.Fin, self.Fout,
                callback if use_callback else None)

class CWasm(Common) :
    speed = 2.0

    def __init__(self, X, Y, iters, density) :
        from wasmtime import Store, Module, Instance, Func, FuncType, ValType, \
                Memory, MemoryType, Limits
        super().__init__(X, Y, iters)

        self.store = Store()

        module = Module.from_file(self.store.engine,
                os.path.join(os.path.dirname(os.path.realpath(__file__)),
                    '..', 'docs', 'life.wasm'))
        pages = 2 * (X * Y // 16000 + 1)  # probbaly need to divide by 16384 but OK
        mem = Memory(self.store, MemoryType(Limits(pages, pages)))

        for idx in range(X * Y) :
            mem.data_ptr(self.store)[4 * idx] = 1 if random.random() < density else 0

        def log(param1, param2) :
            print(f"[{param1}] {param2}")

        def callback(X, Y, liter, hash) :
            # count not yet defined
            count = 1
            self.dens_a[liter - 1] = count / X / Y
            return 0

        log_obj = Func(self.store, FuncType([ValType.i32(), ValType.i32()], []), log)
        callback_obj = Func(self.store,
            FuncType([ValType.i32(), ValType.i32(), ValType.i32(), ValType.i32()],
                [ValType.i32()]), callback)

        instance = Instance(self.store, module, [log_obj, callback_obj, mem])
        self.life_run = instance.exports(self.store)["run"]

    def run(self, use_callback) :
        return self.life_run(self.store, self.X, self.Y, self.iters)

def main(args) :
    re_size = re.compile(r'^(\d+)[^0-9](\d+)$')
    for size in args.size_a :
        m = re_size.match(size)
        if not m :
            logging.error("Cannot parse size %s", size)
            exit(1)
        X = int(m.group(1))
        Y = int(m.group(2))
        if X < 2 or Y < 2 or X * Y > 1.0e9 :
            logging.error("Invalid values %s", size)
            exit(1)

        engine = {'c' : CRun, 'wasm' : CWasm}[args.runtime]
        runtime = engine(X, Y, args.iterations, args.density)

        estimate = expected_speed * args.iterations * X * Y * engine.speed
        if estimate > 5 :
            print(f"Starting {X}x{Y}, estimated completion in {estimate:.2f} seconds")

        t0 = time.time ()
        n = runtime.run(args.density_graph is not None)
        t1 = time.time ()
        print(f"[{X}x{Y}] Computed {n} iterations in {t1-t0:.3f} seconds @ {n/(t1-t0):.3f} fps")

        if args.density_graph :
            fig, ax = plt.subplots()
            ax.plot(range(args.iterations), runtime.dens_a)
            plt.show ()


main (get_args())
