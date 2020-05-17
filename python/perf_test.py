#! /usr/bin/env python3
import os, sys, argparse, logging, random, re, time
import matplotlib.pyplot as plt
import numpy as np

from pprint import pprint

sys.path.append(os.environ['HOME'] + "/git/inet-lab/shared/pylib3")
from colorterm import add_coloring_to_emit_ansi
from genformatter import GenericFormatter

def get_args () :
    default_log_level = "debug"
    default_density = 0.3
    default_iterations = 1_000

    parser = argparse.ArgumentParser(description="My utility template")
    parser.add_argument('--log', dest='log_level', help="Logging level (default = %s)" % default_log_level,
                        choices=['debug', 'info', 'warning', 'error', 'critical'],
                        default=default_log_level)

    parser.add_argument('-d', '--density', type=float, help=f"density for randomized board generation (default = {default_density:0.4f})", default=default_density)
    parser.add_argument('-s', '--seed', help="Seed for randomized board generation (if omitted results will be different every time)")
    parser.add_argument('-i', '--iterations', type=int, help=f"Number of iterations (default = {default_iterations})", default=default_iterations)
    parser.add_argument('--density-graph', action='store_true', help="Display density graph", dest='density_graph')

    parser.add_argument('size_a', nargs='+', help="board size", metavar="WIDTHxHEIGHT" )

    args = parser.parse_args ()
    logging.basicConfig(#format="%(asctime)s %(message)s",
                        level=getattr(logging, args.log_level.upper(), None))
    logging.getLogger('matplotlib.font_manager').setLevel(logging.INFO)

    logging.StreamHandler.emit = add_coloring_to_emit_ansi(logging.StreamHandler.emit)

    if not (0 < args.density < 1) :
    	logging.error("Invalid dencity value %f, must be between 0 and 1", args.density)
    	exit(1)

    random.seed(args.seed)

    if not (0 < args.iterations < 1_000_000_000) :
    	logging.error("Invalid number of iterations %d", args.iterations)
    	exit(1)

    return args

def main(args) :
	import life

	re_size = re.compile(r'^(\d+)[^0-9](\d+)$')
	for size in args.size_a :
		m = re_size.match(size)
		if not m :
			logging.error("Cannot parse size %s", size)
			exit(1)
		X = int(m.group(1))
		Y = int(m.group(2))
		if X < 2 or Y < 2 or X*Y > 1.0e9 :
			logging.error("Invalid values %s", size)
			exit(1)

		Fin = [False] * X*Y
		Fout = [False] * X*Y

		for idx in range(X*Y):
			Fin[idx] = random.random() < args.density

		dens_a = [None] * args.iterations
		def callback(liter, count, lhash, f, fin) :
			if liter > 0 :
				dens_a[liter - 1] = count/X/Y

		t0 = time.time ()
		n = life.run(X, Y, 1, args.iterations, Fin, Fout,
			             callback if args.density_graph else None)
		t1 = time.time ()
		print(f"[{X}x{Y}] Computed {n} iterations in {t1-t0:.3f} seconds @ {n/(t1-t0):.3f} fps")

		if args.density_graph :
			fig, ax = plt.subplots()
			ax.plot(range(args.iterations), dens_a)
			plt.show ()



main (get_args())
