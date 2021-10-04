#include <Python.h>

#include "lifestep.h"
#include "liferun.h"

#include "pydoc.h"

struct _run_cb_data {
	PyObject * py_cb;
};

static void run_cb_func (void * _cb_data, int iter, int count, unsigned hash, void * f, int fin, int * p_stop) {
	struct _run_cb_data * cb_data = _cb_data;

	/* i = "int", I = "unsigned int", k = "unsigned long", b = "unsigned char" */
	PyObject *py_args = Py_BuildValue("(iiIkb)", iter, count, hash, (unsigned long)f, (char)fin);

	if (py_args == NULL) {
		fprintf(stderr, "ERROR: Py_BuildValue(%d, %d, %d, 0x%0lx, %d) failed\n", iter, count, hash, (unsigned long)f, (char)fin);
		return;
	}

	PyObject *py_res = PyEval_CallObject(cb_data->py_cb, py_args);
	long res = 0;
	if (PyLong_CheckExact(py_res))
		res = PyLong_AsLong(py_res);
	else if (py_res != Py_None) {
		fprintf(stderr, "ERROR: Callback returned result which is not integer\n");
		PyObject_Print(py_res, stderr, 0);
		fprintf(stderr, "\n");
	}

	*p_stop = res == 1;

	/* Py_XDECREF has additional NULL check */
	Py_XDECREF(py_res);
    Py_DECREF(py_args);
}

static PyObject * method_run(PyObject *self, PyObject *args) {
	// Reference: https://docs.python.org/3/c-api/list.html
	int X, Y, n_threads, n_steps, n_iter;
	struct _run_cb_data run_cb_data;
	liferun_cb_t  run_cb_struct;
	PyObject *py_Fin, *py_Fout, *py_cb;

	/* i = "int", O = "object" */
    if(!PyArg_ParseTuple(args, "iiiiOOO", &X, &Y, &n_threads, &n_steps,
    	&py_Fin, &py_Fout, &py_cb)) {
    	fprintf(stderr, "method_run: cannot parse arguments\n");
        Py_RETURN_NONE;
    }

    if (!PyList_Check(py_Fin)) {
    	fprintf(stderr, "Argument %d is not a List\n", 5);
    	Py_RETURN_NONE;
    }

    if (!PyList_Check(py_Fout)) {
    	fprintf(stderr, "Argument %d is not a List\n", 6);
    	Py_RETURN_NONE;
    }

    if (py_cb != Py_None && !PyCallable_Check(py_cb)) {
    	fprintf(stderr, "Argument %d is not callable\n", 7);
    	Py_RETURN_NONE;
    }

    if (PyList_Size(py_Fin) < X * Y) {
    	fprintf(stderr, "Incoming list size is %ld, expecting at least %d\n", PyList_Size(py_Fin), X*Y);
    	Py_RETURN_NONE;
    }

    if (PyList_Size(py_Fout) < X * Y) {
    	fprintf(stderr, "Outgoing list size is %ld, expecting at least %d\n", PyList_Size(py_Fin), X*Y);
    	Py_RETURN_NONE;
    }

    unsigned char * cells_in = malloc(X*Y);
    unsigned char * cells_out = malloc(X*Y);

    for (int i = 0; i < X * Y; i ++)
    	cells_in[i] = Py_True == PyList_GetItem(py_Fin, i);

    run_cb_data.py_cb = py_cb;
    run_cb_struct.cb_ptr = run_cb_func;
    run_cb_struct.cb_data = &run_cb_data;

	n_iter = life_run (cells_in, cells_out, X, Y, n_steps,
		(py_cb == Py_None)? NULL : &run_cb_struct);

	for (int i = 0; i < X * Y; i ++)
		PyList_SetItem(py_Fout, i, PyBool_FromLong(cells_out[i]));

	free(cells_in);
	free(cells_out);

    return PyLong_FromLong(n_iter);
}

static PyObject * method_read_ptr(PyObject *self, PyObject *args) {
	int X, Y;
	PyObject *py_F;
	cell_t * cells;

	/* i = "int", k = "unsigned long", O = "object"  */
    if(!PyArg_ParseTuple(args, "iikO", &X, &Y, &cells, &py_F)) {
    	fprintf(stderr, "method_run: cannot parse arguments\n");
        Py_RETURN_NONE;
    }

	for (int i = 0; i < X * Y; i ++)
		PyList_SetItem(py_F, i, PyBool_FromLong(1 == cells[i]));

    return PyLong_FromLong(1);
}

static PyMethodDef life_methods[] = {
    {M_RUN, 	 method_run, 	  METH_VARARGS, RUN_DOC},
    {M_READ_PTR, method_read_ptr, METH_VARARGS, READ_PTR_DOC},
    {NULL, NULL, 0, NULL}
};

static struct PyModuleDef life_module = {
    PyModuleDef_HEAD_INIT,
    "life",
    /* description should probably match file setup.cfg */
    "Python interface for game of life",
    -1,
    life_methods
};

PyMODINIT_FUNC PyInit_conway_life(void) {
    return PyModule_Create(&life_module);
}
