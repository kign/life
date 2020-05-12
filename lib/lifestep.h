typedef int cell_t;

typedef struct _lstepstat {
	int count;
	unsigned hash;
} lstepstat_t;

void life_prepare (
   cell_t               * cells,
   int                  X,
   int                  Y,
   lstepstat_t			* stat);

void life_step (
   cell_t               * cells,
   cell_t               * cellsnew,
   int                  X,
   int                  Y,
   lstepstat_t			* stat);



