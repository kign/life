#include <stdlib.h>
#include <stdio.h>

#include "lifestep.h"

void life_run (
	unsigned char * cells,
	unsigned int X,
	unsigned int Y,
	unsigned int n_steps
) {
   cell_t *wf1 = calloc (X * Y + 4, sizeof(cell_t) );
   cell_t *wf2 = calloc (X * Y + 4, sizeof(cell_t) );
   cell_t *f1 = wf1, *f2 = wf2;

   if (0) {
      fprintf(stderr, "start of life_run (pics)");
      for (int i = 0; i < X * Y; i ++) {
         if (i % X == 0)
            fprintf(stderr, "\n");
         fprintf(stderr, "%c", cells[i]?'x':'.');
      }
      fprintf(stderr, "\n");
   }

   int iter;

   for (int i = 0; i < X * Y; i ++)
      f1[i] = cells[i];

   life_pre_step (f1, X, Y);

   for (iter = 0; iter < n_steps; iter ++) {
      life_step(f1, f2, X, Y);

      cell_t * t = f1;
      f1 = f2;
      f2 = t;
   }

   for (int i = 0; i < X * Y; i ++)
      cells[i] = f1[i] == 1;
}
