import os, os.path, logging
from threading import Thread, Event
from flask import Flask, render_template, url_for, request, session as flsk_s, jsonify
import conway_life as life

app = Flask(__name__, template_folder='web/t', static_folder='web/static')
app.config['SECRET_KEY'] = '4W5rgr7TYNTQFIzhWFQ1-xDbGfSKNlcqbgHsg7DrE'
# This is to preserve order of keys in OrderedDict when passing through `tojson`
app.config['JSON_SORT_KEYS'] = False

class BackgroundProcess:
    def __init__(self) :
        self.h = None
        self.active = False
        self.gen = 0
        self.pos_ptr = None
        self.pos_en = None
        # Note that in a regular multi-threading paradigm, event is "set" (to True) == no waiting, could proceed
        self.abort = False
        self.count = None
        self.wait_callback = Event()
        self.wait_consume = Event()

    def start(self, pos_st, walk) :
        self.h = Thread(target=run_thread, args=(flsk_s['width'], flsk_s['height'], pos_st))
        self.active = True
        self.walk = walk
        self.gen = 0
        self.pos_ptr = None
        self.pos_en = None
        self.abort = False
        self.count = None
        self.wait_callback.set()
        self.wait_consume.clear()
        self.h.start ()

    def stop(self, gen, pos_en) :
        self.h = None
        self.active = False
        self.gen = gen
        self.pos_ptr = None
        self.pos_en = pos_en
        self.abort = False
        self.wait_callback.set()
        self.wait_consume.set()


bgProc = BackgroundProcess()

@app.route('/')
def home() :
    try :
        return render_template('home.html', geom=app.config['GEOM'])
    except Exception as err :
        logging.error("render_template failed: %s", err)
        logging.warn("Current WD is %s", os.getcwd())
        exit(1)

@app.route('/init', methods=['POST'])
def init() :
    obj = request.get_json()
    flsk_s['width'] = int(obj['x'])
    flsk_s['height'] = int(obj['y'])
    logging.info("Width = %d, Height = %d", flsk_s['width'], flsk_s['height'])

    return jsonify({'ok': 'ok'})

@app.route('/step', methods=['POST'])
def step() :
    pos_st = request.get_json()
    # print("STEP:", pos_st)
    pos_en = [None] * (flsk_s['width'] * flsk_s['height'])
    life.run(flsk_s['width'], flsk_s['height'], 1, 1, pos_st, pos_en, None)

    return jsonify(pos_en)

@app.route('/run/<int:walk>', methods=['POST'])
def run(walk) :
    if bgProc.active :
        return jsonify({'error': "active process"})

    pos_st = request.get_json()
    bgProc.start (pos_st, walk)

    return jsonify({'ok': 'ok'})

@app.route('/stop', methods=['POST'])
def stop() :
    if not bgProc.active :
        return jsonify({'error': "no active process"})

    bgProc.abort = True
    return jsonify({'ok': 'ok'})

@app.route('/poll')
def poll() :
    logging.info("/poll: enter")
    bgProc.wait_callback.clear ()
    bgProc.wait_consume.wait ()
    logging.info("/poll: consuming allowed, active = %s", bgProc.active)

    if bgProc.active :
        pos = [None] * (flsk_s['width'] * flsk_s['height'])
        life.read_ptr(flsk_s['width'], flsk_s['height'], bgProc.pos_ptr, pos)
        # print(f'[iter={bgProc.gen}] read_ptr retuned', pos)

        logging.info("/poll: allowing callback")
        bgProc.wait_callback.set ()

    else :
        pos = bgProc.pos_en

    logging.info("/poll: returning")
    return jsonify({'active': bgProc.active, 'gen' : bgProc.gen, 'pos' : pos, 'count': bgProc.count})

@app.before_request
def create_session():
    pass

@app.teardown_appcontext
def shutdown_session(response_or_exc):
    pass

@app.template_global()
def favicon () :
    return url_for('static', filename='Icon.png')

def run_thread(X, Y, pos_st) :
    pos_en = [None] * (X * Y)
    logging.info("Starting background thread")
    n = life.run(X, Y, 1, 1_000_000, pos_st, pos_en, run_tracker)

    logging.info("Returned from background thread after %d iterations", n)
    bgProc.stop(n, pos_en)

def run_tracker(n_iter, count, bhash, pos_ptr, fin) :
    bgProc.pos_ptr = pos_ptr
    bgProc.gen = n_iter
    bgProc.count = count

    bgProc.wait_consume.set ()

    will_wait = not bgProc.wait_callback.is_set ()
    if will_wait :
        logging.info("run_tracker: will wait for wait_callback to clear")

    bgProc.wait_callback.wait ()

    if will_wait :
        logging.info("run_tracker: wait is over")

    bgProc.wait_consume.clear ()

    if bgProc.abort :
        logging.warn("run_tracker: abort requested")
        return 1

    if bgProc.walk == 1 :
        logging.info("run_tracker: walk=%d, raising wait_callback", bgProc.walk)
        bgProc.wait_callback.clear()


def open_browser_thread(port) :
    import webbrowser, time

    time.sleep(0.2)

    url = f'http://localhost:{port}'
    logging.info("Opening %s", url)
    webbrowser.open(url)

def start() :
    import argparse
    from inetlab.cli.colorterm import add_coloring_to_emit_ansi

    os.chdir(os.path.dirname(__file__))

    default_log_level = "debug"
    default_port = 13882
    default_box = 20

    parser = argparse.ArgumentParser(description="Run Web UI for Game of Life")
    parser.add_argument('--log', '--log_level', dest='log_level',
                        help="Logging level (default = %s)" % default_log_level,
                        choices=['debug', 'info', 'warning', 'error', 'critical'],
                        default=default_log_level)
    parser.add_argument('-p', '--port', type=int, default=default_port, help="Port (default = %s)" % default_port)
    parser.add_argument('--cell', '--size', metavar='PIXELS', type=int, dest='g_box', default=default_box,
        help=f"Size of cell in pixels (default = {default_box})")
    parser.add_argument('--space', type=int, metavar='PIXELS', dest='g_spc', help="Spacing between cell in pixels")
    parser.add_argument('--padding', metavar='PIXELS', type=int, dest='g_pad', help="Padding in pixels")
    parser.add_argument('-d', '--dev', action='store_true', dest='dev_mode',
        help='Regular Flask mode with reloader')

    args = parser.parse_args()

    logging.basicConfig(format="%(asctime)s.%(msecs)03d %(filename)s:%(lineno)d %(message)s",
                        level=getattr(logging, args.log_level.upper(), None),
                        datefmt='%H:%M:%S')
    logging.StreamHandler.emit = add_coloring_to_emit_ansi(logging.StreamHandler.emit)

    if args.g_spc is None :
        args.g_spc = args.g_box // 5
    if args.g_pad is None :
        args.g_pad = args.g_box // 10

    app.config['GEOM'] = {'box' : args.g_box, 'spc' : args.g_spc, 'pad' : args.g_pad}

    if not args.dev_mode :
        Thread(target=open_browser_thread, args=(args.port,)).start ()

    logging.info("Starting in CLI debug mode")
    if args.dev_mode :
        print(f"DEV MODE, open manually http://localhost:{args.port}")
        app.run(host='0.0.0.0', debug=True, port=args.port)
    else :
        app.run(host='0.0.0.0', debug=True, port=args.port, use_reloader=False)


if __name__ == "__main__" :
    start ()
