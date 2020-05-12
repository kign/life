#include <stdlib.h>
#include <stdio.h>

#include "lifestep.h"

#define OCCUPIED(x)       ((x) == 1)
#define EMPTY(x)          ((x) != 1)

int life_run (
	unsigned char * cells,
	unsigned int X,
	unsigned int Y,
	unsigned int n_steps,
   void (* cnt_callback)()
) {
   const int N_htrail = 10;

   cell_t *wf1 = calloc (X * Y + 4, sizeof(cell_t) );
   cell_t *wf2 = calloc (X * Y + 4, sizeof(cell_t) );
   cell_t *f1 = wf1, *f2 = wf2;
   lstepstat_t lstat;

   unsigned * htrail = calloc(N_htrail, sizeof(unsigned));

   if (0) {
      fprintf(stderr, "start of life_run (pics)");
      for (int i = 0; i < X * Y; i ++) {
         if (i % X == 0)
            fprintf(stderr, "\n");
         fprintf(stderr, "%c", cells[i]?'x':'.');
      }
      fprintf(stderr, "\n");
   }


   for (int i = 0; i < X * Y; i ++)
      f1[i] = cells[i];

   life_prepare (f1, X, Y, &lstat);

   if (cnt_callback != NULL)
      cnt_callback(0, lstat.count);

   htrail[0] = lstat.hash;

   int iter = 0;
   while (iter < n_steps) {
      life_step(f1, f2, X, Y, &lstat);

      cell_t * t = f1;
      f1 = f2;
      f2 = t;

      iter ++;

      if (cnt_callback != NULL)
         cnt_callback(iter, lstat.count);

      int i = 0;
      for (; i < N_htrail && htrail[i] != lstat.hash; i ++);
      if (i < N_htrail)
         break;
      htrail[iter % N_htrail] = lstat.hash;
   }

   for (int i = 0; i < X * Y; i ++)
      cells[i] = OCCUPIED(f1[i]);

   free(htrail);

   return iter;
}
