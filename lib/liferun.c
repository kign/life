#include <stdlib.h>
#include <stdio.h>

#include "lifestep.h"
#include "liferun.h"

#define OCCUPIED(x)       ((x) == 1)
#define EMPTY(x)          ((x) != 1)

int life_run (
   const unsigned char * cells_in,
   unsigned char * cells_out,
	unsigned int X,
	unsigned int Y,
	unsigned int n_steps,
   liferun_cb_t * cb
) {
   const int N_htrail = 10;

   cell_t *wf1 = calloc (X * Y + 4, sizeof(cell_t) );
   cell_t *wf2 = calloc (X * Y + 4, sizeof(cell_t) );
   cell_t *f1 = wf1, *f2 = wf2;
   lstepstat_t lstat;

   unsigned * htrail = calloc(N_htrail, sizeof(unsigned));

   /*
   if (0) {
      fprintf(stderr, "start of life_run (pics)");
      for (int i = 0; i < X * Y; i ++) {
         if (i % X == 0)
            fprintf(stderr, "\n");
         fprintf(stderr, "%c", cells_in[i]?'x':'.');
      }
      fprintf(stderr, "\n");
   }
   */

   for (unsigned  i = 0; i < X * Y; i ++)
      f1[i] = cells_in[i];

   life_prepare (f1, X, Y, &lstat);

   unsigned iter = 0;
   int stop = 0;
   if (cb != NULL)
      (*cb->cb_ptr)(cb->cb_data, iter, lstat.count, lstat.hash, f1, 0, &stop);

   htrail[0] = lstat.hash;
   //fprintf(stderr, "hash %d: 0x%08X\n", iter, lstat.hash);

   while (n_steps <= 0 || iter < n_steps) {
      life_step(f1, f2, X, Y, &lstat);

      cell_t * t = f1;
      f1 = f2;
      f2 = t;

      iter ++;

      int i = 0;
      for (; i < N_htrail && htrail[i] != lstat.hash; i ++);

      if (cb != NULL)
         (*cb->cb_ptr)(cb->cb_data, iter, lstat.count, lstat.hash, f1,
                        iter == n_steps || i < N_htrail, &stop);
      if (stop || i < N_htrail)
         break;
      htrail[iter % N_htrail] = lstat.hash;
   }

   if (cells_out != NULL) {
      for (unsigned i = 0; i < X * Y; i ++)
         cells_out[i] = OCCUPIED(f1[i]);
   }

   free(htrail);

   return iter;
}

void life_extract_cells (void * f, unsigned char * cells) {
   cell_t * p;
   unsigned char * c;

   for (p = (cell_t *) f, c = cells; *p != 3; p ++, c++)
      *c = OCCUPIED(*p);
}
