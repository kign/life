const LifeCanvas = (function () {
    'use strict';

    let ctx;  // 2D drawing context
    let canvas; // HTML canvas element
    let W, H; // canvas pixel size
    let X, Y; // canvas logical size

    let pad = 1;
    let box = 20;
    let spc = 3;

    const c_bg = 'Teal';
    const c_empty = 'rgb(0,120,120)';
    const c_cell = 'White';

    const flip = false;

    const F = [];

    const init_board = function () {
        ctx.fillStyle = c_bg;
        ctx.fillRect(0, 0, W, H);
    }

    const fcell = function (color, x, y, ext=0) {
        ctx.fillStyle = color;
        ctx.fillRect(
            pad + (x - 1) * (spc + box) - ext,
            (flip?(H - box - pad - (y - 1) * (spc + box)) : (pad + (y - 1) * (spc + box))) - ext,
            box + 2 * ext,
            box + 2 * ext);
    }

  	const canvas_action = function(evt, cell_action) {
	    const ox = evt.offsetX * W/canvas.clientWidth;
	    const oy = evt.offsetY * H/canvas.clientHeight;
	    const rx = (ox - pad)/(spc + box);
	    const ry = (oy - pad)/(spc + box);
	    //console.log("ox =", ox, "oy =", oy, "rx =", rx, "ry = ", ry);
	    if (rx < 0 || ry < 0)
	    	console.log("Outside click!");
	    else {
	    	const x = Math.floor(rx);
	      	const y = Math.floor(ry);

	      	if (x >= X || y >= Y)
        		; //console.log("Outside click!");
	      	else {
	        	//console.log("ox - pad - x*(spc + box) =", ox - pad - x*(spc + box),
	        	//  "oy - pad - y*(spc + box) =", oy - pad - y*(spc + box));
	        	if (ox - pad - x*(spc + box) > box || oy - pad - y*(spc + box) > box)
	          	    ; //console.log("Click between cells!");
	        	else {
	          	    cell_action(1+x, flip?(Y-y):(y+1));
	        	}
	      	}
	    }
  	}


    return {
        init: function (id, g) {
            pad = g.pad;
            box = g.box;
            spc = g.spc;

            canvas = document.getElementById(id);

            W = canvas.clientWidth;
            H = canvas.clientHeight;

            canvas.width = W;
            canvas.height = H;

            ctx = canvas.getContext("2d");

            init_board();
            X = Math.floor((W - 2*pad + spc)/(box + spc));
            Y = Math.floor((H - 2*pad + spc)/(box + spc));

            console.log("W =", W, ", H =", H, ", X =", X, ", Y =", Y);

            for (let x = 1; x <= X; x++)
                for (let y = 1; y <= Y; y++)
                    fcell(c_empty, x, y);

            F.length = X * Y;
            for (let x = 0; x < X*Y; x ++)
                F[x] = false;

	      	canvas.onclick = function (evt) {
	      		canvas_action(evt, (x, y) => {
                    const f = !F[(y-1)*X + x-1];
                    F[(y-1)*X + x-1] = f;
                    fcell(f?c_cell:c_empty, x, y);
                    LifeControls.reset_gen ();
	      			// console.log("Click @", [x, y]);
	      		})
	      	}
        },

        dbgShowCells: function () {
            for (let x = 1; x <= X; x++)
                for (let y = 1; y <= Y; y++)
                    fcell(c_fg, x, y);
        },

        getWidth: function () {
            return X;
        },

        getHeight: function () {
            return Y;
        },

        getPosition: function () {
            return F;
        },

        setPosition: function (pos) {
            for (let x = 1; x <= X; x++)
                for (let y = 1; y <= Y; y++) {
                    const f = pos[(y-1)*X + x-1];
                    if (F[(y-1)*X + x-1] !== f) {
                        F[(y-1)*X + x-1] = f;
                        fcell(f?c_cell:c_empty, x, y);
                    }
                }
        }
    }
}());

const LifeControls = (function () {
    let polling_timeout = 1;

    const ctrl = {};
    let gen = 0;
    let running = false;
    let pollingID = null;
    let start_gen = 0;
    let endpoints = null;

    const send = (url, data, callback) => {
        const xhr = new XMLHttpRequest();
        xhr.open(data?"POST":"GET", url, true);
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.onreadystatechange = function () {
            if (xhr.readyState === 4) {
                if (xhr.status === 200) {
                    const res = JSON.parse(xhr.responseText);
                    callback(res);
                }
                else {
                    alert(`AJAX ${data?"POST":"GET"} request to ${url} failed: ${xhr.status}\n${xhr.responseText}`);
                }
            }
        };
        if (data)
            xhr.send(JSON.stringify(data));
        else
            xhr.send();
    };

    const randomize = density => {
        const N = LifeCanvas.getWidth() * LifeCanvas.getHeight();
        const pos = [];
        pos.length = N;
        for (let n = 0; n < N; n ++)
            pos[n] = Math.random() < density;
        LifeCanvas.setPosition(pos);
        reset_gen ();
    };

    const update_gen = () => {
        ctrl.gen.innerHTML = gen.toString();
    }

    const reset_gen = () => {
        gen = 1;
        update_gen ();
    }

    const update_density = count => {
        const N = LifeCanvas.getWidth() * LifeCanvas.getHeight();
        ctrl.random_value.value = (count/N).toFixed(4);
    }

    const polling_action = () => {
        send(endpoints.poll, null, (data) => {
            if (data.error) {
                alert(data.error);
                return;
            }

            gen = data.gen + start_gen;
            update_gen ();
            update_density(data.count);

            LifeCanvas.setPosition(data.pos);

            if (data.active)
                pollingID = setTimeout(polling_action, 1000 * polling_timeout);
            else {
                pollingID = null;
                running = false;
                ctrl.run.innerHTML = "Run";
                ctrl.run.disabled = false;
                ctrl.walk.innerHTML = "Walk";
                ctrl.walk.disabled = false;
            }
        });
    }

    const commence_run = walk => {
        if (running) {
            send(endpoints.stop, {'stop': true}, (data) => {
                if (data.error) {
                    alert(data.error);
                    return;
                }

                console.log("Stop requested");
            });
        }
        else {
            send(walk?endpoints.walk:endpoints.run, LifeCanvas.getPosition(), (data) => {
                if (data.error) {
                    alert(data.error);
                    return;
                }

                polling_timeout = parseFloat(ctrl.int.value);
                if (isNaN(polling_timeout) || polling_timeout<=0 || polling_timeout>=10) {
                    alert(`Invalid interval value ${ctrl.int.value}`);
                    return;
                }

                running = true;
                start_gen = gen;
                ctrl[walk?'walk':'run'].innerHTML = "Stop";
                ctrl[walk?'run':'walk'].disabled = true;

                pollingID = setTimeout(polling_action, 1000 * polling_timeout);
            });
        }

    }

    return {
        init: function (controls, _endpoints) {
            endpoints = _endpoints;

            gen = 1;

            for (const c in controls)
                ctrl[c] = document.getElementById(controls[c]);
            //console.log(ctrl);

            send(endpoints.init, {x: LifeCanvas.getWidth(), y: LifeCanvas.getHeight()}, (data) => {
                console.log("[init] Received", data);

                ctrl.random_value.value = "0.3";
                ctrl.int.value = "0.5";

                ctrl.step.onclick = function () {
                    send(endpoints.step, LifeCanvas.getPosition(), (data) => {
                        //console.log("Received", data);
                        gen ++;
                        update_gen ();
                        LifeCanvas.setPosition(data);

                    });
                }

                ctrl.walk.onclick = function () {
                    commence_run(1);
                }

                ctrl.run.onclick = function () {
                    commence_run(0);
                }

                ctrl.random_button.onclick = function () {
                    const density = parseFloat(ctrl.random_value.value);
                    if (isNaN(density) || density<=0 || density>=1) {
                        alert(`Invalid density value ${ctrl.random_value.value}`);
                        return;
                    }

                    randomize(density);
                }

                ctrl.reset.onclick = function () {
                    randomize(0);
                }

            });

        },

        reset_gen: function () {
            reset_gen();
        }
    }
}());
