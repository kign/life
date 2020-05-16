#include <Python.h>

#include "liferun.h"

struct _run_cb_data {
	int foo;
};

static void run_cb_func (void * _cb_data, int iter, int count, void * f, int fin, int * p_stop) {
	struct _run_cb_data * cb_data = _cb_data;
}

static PyObject * method_run(PyObject *self, PyObject *args) {
	int X, Y, n_threads, n_steps, n_iter;
	struct _run_cb_data run_cb_data;
	liferun_cb_t  run_cb_struct;
	PyObject *Fin, *Fout;

    if(!PyArg_ParseTuple(args, "iiiiOO", &X, &Y, &n_threads, &n_steps, &Fin, &Fout)) {
    	fprintf(stderr, "method_run: cannot parse arguments\n");
        Py_RETURN_NONE;
    }

    if (!PyList_Check(Fin)) {
    	fprintf(stderr, "Third argument is not a List\n");
    	Py_RETURN_NONE;
    }

    if (PyList_Size(Fin) < X * Y) {
    	fprintf(stderr, "Incoming list size is %ld, expecting at least %d\n", PyList_Size(Fin), X*Y);
    	Py_RETURN_NONE;
    }

    if (PyList_Size(Fout) < X * Y) {
    	fprintf(stderr, "Incoming list size is %ld, expecting at least %d\n", PyList_Size(Fin), X*Y);
    	Py_RETURN_NONE;
    }

//    fprintf(stderr, "X = %d, Y = %d\n", X, Y);
    unsigned char * cells_in = malloc(X*Y);
    unsigned char * cells_out = malloc(X*Y);

    for (int i = 0; i < X * Y; i ++)
    	cells_in[i] = Py_True == PyList_GetItem(Fin, i);

    run_cb_struct.cb_ptr = run_cb_func;
    run_cb_struct.cb_data = &run_cb_data;

	n_iter = life_run (cells_in, cells_out, X, Y, n_steps, &run_cb_struct);

	for (int i = 0; i < X * Y; i ++)
		PyList_SetItem(Fout, i, PyBool_FromLong(cells_out[i]));

	free(cells_in);
	free(cells_out);

    return PyLong_FromLong(n_iter);
}

static PyMethodDef life_methods[] = {
    {"run", method_run, METH_VARARGS, "Run provided board"},
    {NULL, NULL, 0, NULL}
};

static struct PyModuleDef life_module = {
    PyModuleDef_HEAD_INIT,
    "life",
    "Python interface for game of life",
    -1,
    life_methods
};

PyMODINIT_FUNC PyInit_life(void) {
    return PyModule_Create(&life_module);
}
