typedef struct _liferun_cb {
	void (* cb_ptr)(void * cb_data, int iter, int count, unsigned hash, void * f, int fin, int * p_stop);
	void * cb_data;
} liferun_cb_t;

int life_run (
	const unsigned char * cells_in,
	unsigned char * cells_out,
	unsigned int X,
	unsigned int Y,
	unsigned int n_steps,
	liferun_cb_t * cb );

void life_extract_cells (void * f, unsigned char * cells);
