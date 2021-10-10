const LifeCanvas = (function () {
    'use strict';

    let ctx;  // 2D drawing context
    let canvas; // HTML canvas element
    let W, H; // canvas pixel size
    let X, Y; // canvas logical size

    let pad = 1;
    let box = 10;
    let spc = 1;

    const c_bg = 'Teal';
    const c_empty = 'rgb(0,120,120)';
    const c_cell = 'White';
    const c_val2 = 'coral';
    const dbg_val_2 = false;

    const flip = false;

    const F = [];
    let count = 0;

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
            if(g) {
                pad = g.pad;
                box = g.box;
                spc = g.spc;
            }
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
                    F[(y-1)*X + x-1] = f?1:0;
                    count += f?1:(-1);
                    fcell(f?c_cell:c_empty, x, y);
                    LifeControls.reset_gen ();
                    LifeControls.update_density(X, Y, count);
	      			// console.log("Click @", [x, y]);
	      		})
	      	}

            return [X, Y];
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
            count = 0;
            for (let x = 1; x <= X; x++)
                for (let y = 1; y <= Y; y++) {
                    const idx = (y-1)*X + x-1;
                    const f = pos[idx];
                    if (f === 2 && dbg_val_2) {
                        F[idx] = 0;
                        fcell(c_val2, x, y);
                    }
                    else if (f === 0 || f === 1 || f == 2) {
                        if (f === 1) count ++;
                        if (F[idx] !== (f === 1)) {
                            F[idx] = (f === 1);
                            fcell((f === 1)?c_cell:c_empty, x, y);
                        }
                    }
                    else {
                        console.error("Incorrect memory value", f);
                    }
                }
            LifeControls.update_density(X, Y, count);
        }
    }
}());

const LifeControls = (function () {
    let polling_timeout = 1;

    const ctrl = {};
    let gen = 0;
    let running = false;
    let walkID = null;
    let start_gen = 0;
    let last_hash = null;
    let last_iter = null;

    const randomize = (X, Y, density) => {
        const N = X * Y;
        const pos = [];
        pos.length = N;
        for (let n = 0; n < N; n ++)
            pos[n] = (Math.random() < density)? 1 : 0;
        LifeCanvas.setPosition(pos);
        update_gen (1);
    };

    const update_gen = new_gen => {
        gen = new_gen;
        ctrl.gen.innerHTML = gen.toString();
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

    const one_step = (memory, wasm_run) => {
        const F = LifeCanvas.getPosition();
        for (let ii = 0; ii < X * Y; ii ++)
            memory[ii] = F[ii]? 1 : 0;
        // console.log("Trying step...; position =", memory.subarray(0,2*X*Y));
        const res = wasm_run(X, Y, 1);
        // console.log("Received", res, "; position =", memory.subarray(0,2*X*Y));
        LifeCanvas.setPosition(memory.subarray(X*Y));
        update_gen(gen + 1);
    }

    const commence_run = (memory, wasm_run, walk) => {
        if (running) {
            make_stop ();
        }
        else {
            ctrl[walk?'walk':'run'].innerHTML = "Stop";
            ctrl[walk?'run':'walk'].disabled = true;
            ctrl.step.disabled = true;
            running = true;

            const wr_timeout = parseFloat(ctrl.int.value);
            if (isNaN(wr_timeout) || wr_timeout<=0 || wr_timeout>=10) {
                alert(`Invalid interval value ${ctrl.int.value}`);
                return;
            }

            if (walk) {
                const hashes = [];
                hashes.length = 10;
                let ihash = 0;
                let gen0 = gen;

                walkID = window.setInterval(() => {
                    one_step(memory, wasm_run);
                    if(hashes.includes(last_hash))
                        make_stop();
                    hashes[(ihash ++) % hashes.length] = last_hash;

                }, 1000 * wr_timeout)
            }
            else {
                const F = LifeCanvas.getPosition();
                for (let ii = 0; ii < X * Y; ii ++)
                    memory[ii] = F[ii]? 1 : 0;
                const res = wasm_run(X, Y, 1_000_000);
                console.log("wasm_run returned after", res, "iterations");
                update_gen(gen + res);
                LifeCanvas.setPosition(memory.subarray((res % 2 == 1)?(X*Y):0));
                make_stop();
            }
        }
    }

    const make_stop = () => {
        if (!running) {
            alert("Not running!");
            return;
        }

        running = false;
        if (walkID) {
            window.clearInterval(walkID);
            walkID = null;
        }

        ctrl.walk.innerHTML = "Walk";
        ctrl.run.innerHTML = "Run";
        ctrl.walk.disabled = false;
        ctrl.run.disabled = false;
        ctrl.step.disabled = false;
    }

    return {
        init: function (X, Y, wasm_run, memory, controls) {
            gen = 1;

            for (const c in controls)
                ctrl[c] = document.getElementById(controls[c]);

            ctrl.random_value.value = "0";
            ctrl.int.value = "0.5";

            ctrl.step.onclick = function () {
                one_step(memory, wasm_run);
            }

            ctrl.walk.onclick = function () {
                commence_run(memory, wasm_run, 1);
            }

            ctrl.run.onclick = function () {
                commence_run(memory, wasm_run, 0);
            }

            ctrl.random_button.onclick = function () {
                let density = parseFloat(ctrl.random_value.value);
                if (isNaN(density) || density<=0 || density>=1) {
                    const sden = window.prompt("Density (number bwtween 0 and 1)", "0.3");
                    if (!sden) return;
                    density = parseFloat(sden);
                    if (isNaN(density) || density<=0 || density>=1) {
                        alert(`Invalid density value ${sden}`);
                        return;
                    }
                }

                randomize(X, Y, density);
            }

            ctrl.reset.onclick = function () {
                randomize(X, Y, 0);
            }
        },

        update_density: function(X, Y, count) {
            ctrl.random_value.value = (count/(X*Y)).toFixed(4);
        },

        reset_gen: function () {
            update_gen(1);
        },

        callback: function(X, Y, iter, hash) {
            // console.log("Callback", X, Y, iter, hash);
            last_hash = hash;
            last_iter = iter;
            return 0;
        }
    }
}());
