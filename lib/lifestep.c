#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <assert.h>

#include "lifestep.h"

#define UPD_HASH(ind) hash ^= (ind) * 179424673

void life_prepare (
   cell_t               * cells,
   int                  X,
   int                  Y,
   lstepstat_t          * stat
) {
   int cnt = 0;
   unsigned hash = 0;

   for ( int y = 0; y < Y; y ++ )
      for ( int x = 0; x < X; x ++ ) {
         int idx = X * y + x;
         if (cells[idx] == 1) {
            cnt ++;
            UPD_HASH(idx);
            for (int dx = -1; dx <= 1; dx ++)
               for (int dy = -1; dy <= 1; dy ++) {
                  int didx = X * ((y + dy + Y) % Y) + ((x + dx + X) % X);
                  (cells[didx] == 0) && (cells[didx] = 2);
               }
            }
      }

   cells[X * Y] = 3; /* end-of-area sign */
   if (stat != NULL) {
      stat->count = cnt;
      stat->hash = hash;
   }
}

void life_step (
   cell_t               * cells,
   cell_t               * cellsnew,
   int                  X,
   int                  Y,
   lstepstat_t          * stat
) {
   int                  ind, x, y,  n, newv;
   int                  n00, n01, n02, n10, n12, n20, n21, n22;
   int                  v00, v01, v02, v10, v11, v12, v20, v21, v22;
   int                  cnt = 0;
   unsigned             hash = 0;
   cell_t               * p = cells - 1;


   memset ( cellsnew, 0, X * Y * sizeof(cell_t) );
   cellsnew[X * Y] = 3; /* end-of-area sign */

   /* Assuming there could be many empty cells, optimize looping the
    * best we can */


   while ( 1 ) {
      while (!*(++ p)); /* <-- does it look fast enough? */

      if ( *p == 3 )
         break;

      assert ( *p == 1 || *p == 2 );

      ind = p - cells;

      y = ind / X, x = ind - y * X;

      if ( x > 0 && x < X - 1 && y > 0 && y < Y - 1 ) {
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

      newv = n == 3 || (n == 2 && v11);

      if ( newv ) {
         cnt ++;
         UPD_HASH(ind);

         cellsnew[ind] = newv;
         (cellsnew[n00] == 1) || (cellsnew[n00] = 2);
         (cellsnew[n01] == 1) || (cellsnew[n01] = 2);
         (cellsnew[n02] == 1) || (cellsnew[n02] = 2);
         (cellsnew[n10] == 1) || (cellsnew[n10] = 2);
         (cellsnew[n12] == 1) || (cellsnew[n12] = 2);
         (cellsnew[n20] == 1) || (cellsnew[n20] = 2);
         (cellsnew[n21] == 1) || (cellsnew[n21] = 2);
         (cellsnew[n22] == 1) || (cellsnew[n22] = 2);
      }
   }
   if (stat != NULL) {
      stat->count = cnt;
      stat->hash = hash;
   }
}

