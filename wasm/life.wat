(module
	(import "js" "log" (func $log (param i32) (param i32)))
	;; callback(X, Y, iteration, count, hash); return 0 = continue, 1 = stop
	(import "js" "callback" (func $callback (param i32) (param i32) (param i32) (param i32) (param i32) (result i32)))
    (memory (import "js" "mem") 1)
    ;; width, height, n_iters -> [actual number of iterations]
    (func (export "run") (param $X i32) (param $Y i32) (param $reqiter i32) (result i32)
    	(local $ii i32)
    	(local $jj i32)
    	(local $XY i32)
    	(local $x i32)
    	(local $y i32)
    	(local $dx0 i32)
    	(local $dx1 i32)
    	(local $dy0 i32)
    	(local $dy1 i32)

    	(local $iter i32)
    	(local $src i32)
    	(local $dst i32)
    	(local $cnt i32)
    	(local $tcnt i32)
    	(local $cell i32)
    	(local $isrc i32)
    	(local $idst i32)

    	(local $hash_1 i32)
    	(local $hash_2 i32)
    	(local $hash_3 i32)
    	(local $hash_4 i32)
    	(local $hash i32)
    	(set_local $hash_1 (i32.const 0))
    	(set_local $hash_2 (i32.const 0))
    	(set_local $hash_3 (i32.const 0))
    	(set_local $hash_4 (i32.const 0))

    	(set_local $XY (i32.mul (get_local $X) (get_local $Y))) ;; $XY := $X * $Y

    	;;
    	;; Initialization
    	;;

    	(set_local $ii (i32.const -1)) ;; ii := -1
    	(block $break (loop $cont
    		(set_local $ii (i32.add (get_local $ii) (i32.const 1))) ;; $ii += 1
    		(br_if $break (i32.ge_s (get_local $ii) (get_local $XY))) ;; break if $ii>=$XY

    		;; continue if memory[4*$ii] != 1
    		(br_if $cont (i32.ne (i32.const 1) (i32.load (i32.mul (get_local $ii) (i32.const 4)))))

    		;; $hash_4 = $hash_4 ^ ($ii * 179424673)
    		(set_local $hash_4 (i32.xor (get_local $hash_4) (i32.mul (get_local $ii) (i32.const 179424673))))

    		;; $x := $X + $ii % $X, $y := $Y + $ii / $X
    		(set_local $x (i32.rem_s (get_local $ii) (get_local $X)))
    		(set_local $y (i32.div_s (get_local $ii) (get_local $X)))

    		;; $dx0 := -1; if ($x == 0) then $dx0 += $X
    		(set_local $dx0 (i32.const -1))
			(if (i32.eqz (get_local $x)) (then (set_local $dx0 (i32.add (get_local $dx0) (get_local $X)))))

    		;; $dx1 := 1; if ($x == $X - 1) then $dx1 -= $X
    		(set_local $dx1 (i32.const 1))
			(if (i32.eq (get_local $x) (i32.sub (get_local $X) (i32.const 1))) (then (set_local $dx1 (i32.sub (get_local $dx1) (get_local $X)))))

    		;; $dy0 := -$X; if ($y == 0) then $dy0 += $XY
    		(set_local $dy0 (i32.sub (i32.const 0) (get_local $X)))
			(if (i32.eqz (get_local $y)) (then (set_local $dy0 (i32.add (get_local $dy0) (get_local $XY)))))

    		;; $dy1 := $X; if ($y == $Y - 1) then $dy1 -= $XY
    		(set_local $dy1 (get_local $X))
			(if (i32.eq (get_local $y) (i32.sub (get_local $Y) (i32.const 1))) (then (set_local $dy1 (i32.sub (get_local $dy1) (get_local $XY)))))

			;; push $ii+$dx0+$dy0, $ii+$dx0+$dy1, $ii+$dx1+$dy0, $ii+$dx1+$dy1
			(i32.add (get_local $ii) (i32.add (get_local $dx0) (get_local $dy0)))
			(i32.add (get_local $ii) (i32.add (get_local $dx0) (get_local $dy1)))
			(i32.add (get_local $ii) (i32.add (get_local $dx1) (get_local $dy0)))
			(i32.add (get_local $ii) (i32.add (get_local $dx1) (get_local $dy1)))

			;; push $ii+$dx0, $ii+$dx1, $ii+$dy0, $ii+$dy1
			(i32.add (get_local $ii) (get_local $dx0))
			(i32.add (get_local $ii) (get_local $dx1))
			(i32.add (get_local $ii) (get_local $dy0))
			(i32.add (get_local $ii) (get_local $dy1))

			;; $jj = 4 * [pop]; if memory[$jj]==0 then memory[$jj]:=2
			(i32.mul (i32.const 4))
			(tee_local $jj)
			(if (i32.eqz (i32.load)) (then  (i32.store (get_local $jj) (i32.const 2))))

			;; repeat 7 more
			(i32.mul (i32.const 4))
			(tee_local $jj)
			(if (i32.eqz (i32.load)) (then  (i32.store (get_local $jj) (i32.const 2))))

			(i32.mul (i32.const 4))
			(tee_local $jj)
			(if (i32.eqz (i32.load)) (then  (i32.store (get_local $jj) (i32.const 2))))

			(i32.mul (i32.const 4))
			(tee_local $jj)
			(if (i32.eqz (i32.load)) (then  (i32.store (get_local $jj) (i32.const 2))))

			(i32.mul (i32.const 4))
			(tee_local $jj)
			(if (i32.eqz (i32.load)) (then  (i32.store (get_local $jj) (i32.const 2))))

			(i32.mul (i32.const 4))
			(tee_local $jj)
			(if (i32.eqz (i32.load)) (then  (i32.store (get_local $jj) (i32.const 2))))

			(i32.mul (i32.const 4))
			(tee_local $jj)
			(if (i32.eqz (i32.load)) (then  (i32.store (get_local $jj) (i32.const 2))))

			(i32.mul (i32.const 4))
			(tee_local $jj)
			(if (i32.eqz (i32.load)) (then  (i32.store (get_local $jj) (i32.const 2))))

    		(br $cont)  ;; continue
    	))

    	;;
    	;; Main iteration loop
    	;;

    	(set_local $iter (i32.const 0))  ;; $iter := 0
    	(block $return (loop $main
    		;; (call $log (i32.const 130) (get_local $iter))

    		(br_if $return (i32.ge_s (get_local $iter) (get_local $reqiter))) ;; break if $iter>=$reqiter
    		;; (call $log (i32.const 131) (get_local $iter))

    		(set_local $iter (i32.add (get_local $iter) (i32.const 1))) ;; $iter += 1

    		(if (i32.eqz (i32.rem_s (get_local $iter) (i32.const 2)))
    			(then (set_local $src (get_local $XY)) (set_local $dst (i32.const 0)))
    			(else (set_local $dst (get_local $XY)) (set_local $src (i32.const 0))))

    		(memory.fill (i32.mul (i32.const 4) (get_local $dst)) (i32.const 0) (i32.mul (i32.const 4) (get_local $XY)))

	    	(set_local $tcnt (i32.const 0))  ;; $tcnt := 0
	    	(set_local $hash (i32.const 0))
	    	(set_local $ii (i32.const -1)) ;; ii := -1
	    	(block $break (loop $cont
				;; (call $log (i32.const 80) (i32.load (i32.const 128)))
	    		;; (call $log (i32.const 130) (get_local $iter))

	    		(set_local $ii (i32.add (get_local $ii) (i32.const 1))) ;; $ii += 1
	    		(br_if $break (i32.ge_s (get_local $ii) (get_local $XY))) ;; break if $ii>=$XY

	    		;; $isrc := $ii + $src, $idst := $ii + $dst
	    		(set_local $isrc (i32.add (get_local $ii) (get_local $src)))
	    		(set_local $idst (i32.add (get_local $ii) (get_local $dst)))

	    		;; memory[4 * $idst] := 0
	    		;; (i32.store (i32.mul (i32.const 4) (get_local $idst)) (i32.const 0))

	    		;; $cell = memory[4 * $isrc]
	    		(set_local $cell (i32.load (i32.mul (get_local $isrc) (i32.const 4))))

	    		;; continue if $cell == 0
	    		(br_if $cont (i32.eqz (get_local $cell)))

	    		;; $x := $X + $ii % $X, $y := $Y + $ii / $X
	    		(set_local $x (i32.rem_s (get_local $ii) (get_local $X)))
	    		(set_local $y (i32.div_s (get_local $ii) (get_local $X)))

	    		;; $dx0 := -1; if ($x == 0) then $dx0 += $X
	    		(set_local $dx0 (i32.const -1))
				(if (i32.eqz (get_local $x)) (then (set_local $dx0 (i32.add (get_local $dx0) (get_local $X)))))

	    		;; $dx1 := 1; if ($x == $X - 1) then $dx1 -= $X
	    		(set_local $dx1 (i32.const 1))
				(if (i32.eq (get_local $x) (i32.sub (get_local $X) (i32.const 1))) (then (set_local $dx1 (i32.sub (get_local $dx1) (get_local $X)))))

	    		;; $dy0 := -$X; if ($y == 0) then $dy0 += $XY
	    		(set_local $dy0 (i32.sub (i32.const 0) (get_local $X)))
				(if (i32.eqz (get_local $y)) (then (set_local $dy0 (i32.add (get_local $dy0) (get_local $XY)))))

	    		;; $dy1 := $X; if ($y == $Y - 1) then $dy1 -= $XY
	    		(set_local $dy1 (get_local $X))
				(if (i32.eq (get_local $y) (i32.sub (get_local $Y) (i32.const 1))) (then (set_local $dy1 (i32.sub (get_local $dy1) (get_local $XY)))))

				;; push $isrc+$dx0+$dy0, $isrc+$dx0+$dy1, $isrc+$dx1+$dy0, $isrc+$dx1+$dy1
				(i32.add (get_local $isrc) (i32.add (get_local $dx0) (get_local $dy0)))
				(i32.add (get_local $isrc) (i32.add (get_local $dx0) (get_local $dy1)))
				(i32.add (get_local $isrc) (i32.add (get_local $dx1) (get_local $dy0)))
				(i32.add (get_local $isrc) (i32.add (get_local $dx1) (get_local $dy1)))

				;; push $isrc+$dx0, $isrc+$dx1, $isrc+$dy0, $isrc+$dy1
				(i32.add (get_local $isrc) (get_local $dx0))
				(i32.add (get_local $isrc) (get_local $dx1))
				(i32.add (get_local $isrc) (get_local $dy0))
				(i32.add (get_local $isrc) (get_local $dy1))

				(set_local $cnt (i32.const 0)) ;; $cnt := 0
				(set_local $cnt (i32.add (i32.eq (i32.load (i32.mul (i32.const 4))) (i32.const 1)) (get_local $cnt)))
				(set_local $cnt (i32.add (i32.eq (i32.load (i32.mul (i32.const 4))) (i32.const 1)) (get_local $cnt)))
				(set_local $cnt (i32.add (i32.eq (i32.load (i32.mul (i32.const 4))) (i32.const 1)) (get_local $cnt)))
				(set_local $cnt (i32.add (i32.eq (i32.load (i32.mul (i32.const 4))) (i32.const 1)) (get_local $cnt)))
				(set_local $cnt (i32.add (i32.eq (i32.load (i32.mul (i32.const 4))) (i32.const 1)) (get_local $cnt)))
				(set_local $cnt (i32.add (i32.eq (i32.load (i32.mul (i32.const 4))) (i32.const 1)) (get_local $cnt)))
				(set_local $cnt (i32.add (i32.eq (i32.load (i32.mul (i32.const 4))) (i32.const 1)) (get_local $cnt)))
				(set_local $cnt (i32.add (i32.eq (i32.load (i32.mul (i32.const 4))) (i32.const 1)) (get_local $cnt)))


				(;
				(call $log (i32.const 1) (get_local $x))
				(call $log (i32.const 2) (get_local $y))
				(call $log (i32.const 3) (get_local $cnt))
				;)

				(;
				;; if memory[[pop]] == 1 then $cnt += 1 [repeat 8 times]
				(if (i32.eq (i32.const 1) (i32.load)) (then (set_local $cnt (i32.add (i32.const 1) (get_local $cnt)))))
				(if (i32.eq (i32.const 1) (i32.load)) (then (set_local $cnt (i32.add (i32.const 1) (get_local $cnt)))))
				(if (i32.eq (i32.const 1) (i32.load)) (then (set_local $cnt (i32.add (i32.const 1) (get_local $cnt)))))
				(if (i32.eq (i32.const 1) (i32.load)) (then (set_local $cnt (i32.add (i32.const 1) (get_local $cnt)))))
				(if (i32.eq (i32.const 1) (i32.load)) (then (set_local $cnt (i32.add (i32.const 1) (get_local $cnt)))))
				(if (i32.eq (i32.const 1) (i32.load)) (then (set_local $cnt (i32.add (i32.const 1) (get_local $cnt)))))
				(if (i32.eq (i32.const 1) (i32.load)) (then (set_local $cnt (i32.add (i32.const 1) (get_local $cnt)))))
				(if (i32.eq (i32.const 1) (i32.load)) (then (set_local $cnt (i32.add (i32.const 1) (get_local $cnt)))))
				;)

				;; main Conway formula for next-gen of cells
				;; (exactly 3 neighnours OR exactly 2 neighbours + old cell)
				(if (i32.or (i32.eq (i32.const 3) (get_local $cnt))
					(i32.and (i32.eq (i32.const 2) (get_local $cnt))
						(i32.eq (i32.const 1) (get_local $cell))))
					(then
						;; (call $log (i32.const 100) (get_local $idst))

						;; $tcnt += 1
						;; *** WARNING *** WARNING *** WARNING *** WARNING ***
						;; For some reason, the next line triggers HUGE performance
						;; degradation in node.js (up to 10%); тhere in no impact in wasmtime
						;; It is safe to comment it out, the only result would be
						;; callback function will always get '0' as 'count'
						(set_local $tcnt (i32.add (get_local $tcnt) (i32.const 1)))

			    		;; memory[4 * $idst] := 1
			    		(i32.store (i32.mul (i32.const 4) (get_local $idst)) (i32.const 1))

    		    		;; $hash = $hash ^ ($ii * 179424673)
    					(set_local $hash (i32.xor (get_local $hash) (i32.mul (get_local $ii) (i32.const 179424673))))
    					;; (call $log (i32.const 1) (get_local $ii))
    					;; (call $log (i32.const 2) (get_local $hash))

						;; push $idst+$dx0+$dy0, $idst+$dx0+$dy1, $idst+$dx1+$dy0, $idst+$dx1+$dy1
						(i32.add (get_local $idst) (i32.add (get_local $dx0) (get_local $dy0)))
						(i32.add (get_local $idst) (i32.add (get_local $dx0) (get_local $dy1)))
						(i32.add (get_local $idst) (i32.add (get_local $dx1) (get_local $dy0)))
						(i32.add (get_local $idst) (i32.add (get_local $dx1) (get_local $dy1)))

						;; push $idst+$dx0, $idst+$dx1, $idst+$dy0, $idst+$dy1
						(i32.add (get_local $idst) (get_local $dx0))
						(i32.add (get_local $idst) (get_local $dx1))
						(i32.add (get_local $idst) (get_local $dy0))
						(i32.add (get_local $idst) (get_local $dy1))

						(i32.mul (i32.const 4))
						(tee_local $jj)
						(if (i32.ne (i32.load) (i32.const 1)) (then (i32.store (get_local $jj) (i32.const 2))))
						;; (call $log (i32.const 10) (i32.div_s (get_local $jj) (i32.const 4)))
						;; (call $log (i32.const 20) (i32.load (get_local $jj)))

						(i32.mul (i32.const 4))
						(tee_local $jj)
						(if (i32.ne (i32.load) (i32.const 1)) (then (i32.store (get_local $jj) (i32.const 2))))
						;; (call $log (i32.const 10) (i32.div_s (get_local $jj) (i32.const 4)))
						;; (call $log (i32.const 20) (i32.load (get_local $jj)))

						(i32.mul (i32.const 4))
						(tee_local $jj)
						(if (i32.ne (i32.load) (i32.const 1)) (then (i32.store (get_local $jj) (i32.const 2))))
						;; (call $log (i32.const 10) (i32.div_s (get_local $jj) (i32.const 4)))
						;; (call $log (i32.const 20) (i32.load (get_local $jj)))

						(i32.mul (i32.const 4))
						(tee_local $jj)
						(if (i32.ne (i32.load) (i32.const 1)) (then (i32.store (get_local $jj) (i32.const 2))))
						;; (call $log (i32.const 10) (i32.div_s (get_local $jj) (i32.const 4)))
						;; (call $log (i32.const 20) (i32.load (get_local $jj)))

						(i32.mul (i32.const 4))
						(tee_local $jj)
						(if (i32.ne (i32.load) (i32.const 1)) (then (i32.store (get_local $jj) (i32.const 2))))
						;; (call $log (i32.const 10) (i32.div_s (get_local $jj) (i32.const 4)))
						;; (call $log (i32.const 20) (i32.load (get_local $jj)))

						(i32.mul (i32.const 4))
						(tee_local $jj)
						(if (i32.ne (i32.load) (i32.const 1)) (then (i32.store (get_local $jj) (i32.const 2))))
						;; (call $log (i32.const 10) (i32.div_s (get_local $jj) (i32.const 4)))
						;; (call $log (i32.const 20) (i32.load (get_local $jj)))

						(i32.mul (i32.const 4))
						(tee_local $jj)
						(if (i32.ne (i32.load) (i32.const 1)) (then (i32.store (get_local $jj) (i32.const 2))))
						;; (call $log (i32.const 10) (i32.div_s (get_local $jj) (i32.const 4)))
						;; (call $log (i32.const 20) (i32.load (get_local $jj)))

						(i32.mul (i32.const 4))
						(tee_local $jj)
						(if (i32.ne (i32.load) (i32.const 1)) (then (i32.store (get_local $jj) (i32.const 2))))
						;; (call $log (i32.const 10) (i32.div_s (get_local $jj) (i32.const 4)))
						;; (call $log (i32.const 20) (i32.load (get_local $jj)))
						))

	    		(br $cont)  ;; continue
	    	))

			;; (call $log (i32.const 0) (get_local $hash))
    		;; (call $log (i32.const 132) (get_local $iter))

			(br_if $return (call $callback (get_local $X) (get_local $Y) (get_local $iter) (get_local $tcnt) (get_local $hash)))
    		;; (call $log (i32.const 133) (get_local $iter))

			(br_if $return
				(i32.or (i32.eq (get_local $hash_1) (get_local $hash))
				(i32.or (i32.eq (get_local $hash_2) (get_local $hash))
				(i32.or (i32.eq (get_local $hash_3) (get_local $hash))
				(i32.eq (get_local $hash_4) (get_local $hash))))))
    		;; (call $log (i32.const 134) (get_local $iter))

			(set_local $hash_1 (get_local $hash_2))
			(set_local $hash_2 (get_local $hash_3))
			(set_local $hash_3 (get_local $hash_4))
			(set_local $hash_4 (get_local $hash))

    		(br $main)  ;; continue [main loop]

    	))

    	;; returning number of actually executed iterations
    	(get_local $iter)
))
