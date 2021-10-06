# Web UI for Conway Game of Life

This package is a Web interface for package [conway-life](https://pypi.org/project/conway-life/).

![Web UI Screenshot](https://github.com/kign/life/blob/master/etc/Life_Web_UI.png?raw=true "Web UI Screenshot" )

## Installation

Use PyPi package [life-web-ui](https://pypi.org/project/life-web-ui/)

```bash
python3 -m pip install --upgrade life-web-ui
```

## Usage

Package installs an executable script `life-start` (via common `entry_points` configuration option,
so depending on your local settings it may or may not be in your `$PATH`)

Running this command will start a local server and open it in the default browser. To exit,
press `Ctrl-C` to quit the server and close the browser tab.

```
life-start [-h] [--log {debug,info,warning,error,critical}] [-p PORT]
           [--cell PIXELS] [--space PIXELS] [--padding PIXELS] [-d]

Run Web UI for Game of Life

optional arguments:
  -h, --help            show this help message and exit
  --log {debug,info,warning,error,critical}, --log_level {debug,info,warning,error,critical}
                        Logging level (default = debug)
  -p PORT, --port PORT  Port (default = 13882)
  --cell PIXELS, --size PIXELS
                        Size of cell in pixels (default = 20)
  --space PIXELS        Spacing between cells in pixels
  --padding PIXELS      Canvas padding in pixels
```

## Implementation notes

`life-start` will run a local Flask server. All computations are done in the server
(not in the browser), synchronously on the `Step` command and asynchronously on `Walk` or `Run`.
Asynchronous execution is done with a background Python thread with results polled by the browser.
(`Walk` is different from `Run` in the background thread being suspended between polls).

There is also a short-lived background thread on startup to open Web browser (after a short delay
to make sure server has had time to start). This won't happen if `-d/--dev` option is used (see below).

## Development

For testing purposes, start the server with this command:

```bash
FLASK_ENV=development <VENV_ROOT>/bin/python3 <GIT_ROOT>/py-web/src/life_web_ui/__init__.py -d --log debug
```

Virtual environment installed under `VENV_ROOT` must have all dependencies listed in
[setup.cfg](https://github.com/kign/life/blob/master/py-web/setup.cfg) configuration file for the package (`install_requires`).


