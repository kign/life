// c4wa-compile -P -Xmodule.memoryStatus=import:mem -Xmodule.importName=js -Xmodule.dataOffset=0 -Xmodule.dataLength=0 life-wasm.c -o life-wasm.wat
// wat2wasm --enable-bulk-memory  life-wasm.wat

#define cell_t int

const unsigned int hash_rand = 179424673;

static int g_count = 0;
static unsigned int g_hash = 0;

void life_prepare (
   cell_t               * cells,
   int                  X,
   int                  Y ) {
    int cnt = 0;
    unsigned int hash = 0;

    for(int y = 0; y < Y; y ++)
        for(int x = 0; x < X; x ++) {
            int idx = X * y + x;
            if (cells[idx] == 1) {
                cnt ++;
                hash ^= (unsigned int)idx * hash_rand;
                for (int dx = -1; dx <= 1; dx ++)
                    for (int dy = -1; dy <= 1; dy ++) {
                        int didx = X * ((y + dy + Y) % Y) + ((x + dx + X) % X);
                        if (cells[didx] == 0)
                            cells[didx] = 2;
                    }
            }
        }
    g_count = cnt;
    g_hash = hash;
}

void life_step (
    cell_t                 * cells,
    cell_t                 * cellsnew,
    int                  X,
    int                  Y
) {
    int                  x, y,  n, newv;
    int                  n00, n01, n02, n10, n12, n20, n21, n22;
    int                  v00, v01, v02, v10, v11, v12, v20, v21, v22;
    int                  cnt = 0;
    unsigned int         hash = 0;
    cell_t               * p = cells - 1;
    int                  ind = -1;

    memset ( (char *)cellsnew, (char)0, X * Y * sizeof(cell_t) );

    /* Assuming there could be many empty cells, optimize looping the
     * best we can */

    do {
        do {
            p ++;
            ind ++;
            if (ind == X * Y) {
                    g_count = cnt;
                    g_hash = hash;
                    return;
            }
        }
        while (*p == (char)0);

        y = ind / X; x = ind - y * X;

        if ( x > 0 & x < X - 1 & y > 0 & y < Y - 1 ) {
            n00 = X * (y - 1) + (x - 1);
            n01 = n00 + 1;
            n02 = n01 + 1;
            n10 = ind - 1;
            n12 = ind + 1;
            n20 = n10 + X;
            n21 = n20 + 1;
            n22 = n21 + 1;
        }
        else {
#define N(dy,dx)  (X * ((y + dy + Y) % Y) + ((x + dx + X) % X))
            n00 = N(-1,-1);
            n01 = N(-1,0);
            n02 = N(-1,1);
            n10 = N(0,-1);
            n12 = N(0,1);
            n20 = N(1,-1);
            n21 = N(1,0);
            n22 = N(1,1);
#undef N
        }
        v00 = (1 == cells[n00]);
        v01 = (1 == cells[n01]);
        v02 = (1 == cells[n02]);
        v10 = (1 == cells[n10]);
        v11 = (1 == *p);
        v12 = (1 == cells[n12]);
        v20 = (1 == cells[n20]);
        v21 = (1 == cells[n21]);
        v22 = (1 == cells[n22]);

        n = v00 + v01 + v02 + v10 + v12 + v20 + v21 + v22;

        newv = (n == 3) | ((n == 2) & v11);

        if ( newv ) {
            cnt ++;

            hash ^= (unsigned int)ind * hash_rand;

            cellsnew[ind] = (cell_t)newv;
            if (cellsnew[n00] != 1) cellsnew[n00] = 2;
            if (cellsnew[n01] != 1) cellsnew[n01] = 2;
            if (cellsnew[n02] != 1) cellsnew[n02] = 2;
            if (cellsnew[n10] != 1) cellsnew[n10] = 2;
            if (cellsnew[n12] != 1) cellsnew[n12] = 2;
            if (cellsnew[n20] != 1) cellsnew[n20] = 2;
            if (cellsnew[n21] != 1) cellsnew[n21] = 2;
            if (cellsnew[n22] != 1) cellsnew[n22] = 2;
        }
    }
    while(1);
}

void log(int, int); // compatibility

int callback(int /* X */, int /* Y */, int /* iteration */,
        int /* count */, unsigned int /* hash */);

extern int run(int X, int Y, int N) {
    cell_t * pos_0 = alloc(0, 2*X*Y, cell_t);
    cell_t * pos_1 = pos_0 + X*Y;
    unsigned int hash_1, hash_2, hash_3, hash_4;
    hash_1 = hash_2 = hash_3 = hash_4 = (unsigned int) 0;

    life_prepare(pos_0, X, Y);

    for (int i = 0; i < N; i ++) {
        if (i % 2 == 0)
            life_step(pos_0, pos_1, X, Y);
        else
            life_step(pos_1, pos_0, X, Y);

        if(1 == callback(X, Y, i + 1, g_count, g_hash))
            return i + 1;

        if (g_hash == hash_1 ||
            g_hash == hash_2 ||
            g_hash == hash_3 ||
            g_hash == hash_4)

            return i + 1;

        hash_1 = hash_2;
        hash_2 = hash_3;
        hash_3 = hash_4;
        hash_4 = g_hash;
    }

    return N;
}
