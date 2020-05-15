#include <Python.h>

static PyObject * method_run(PyObject *self, PyObject *args) {
	int X, Y;
	PyObject *_Fin, *Fin;

    /* Parse arguments */
    if(!PyArg_ParseTuple(args, "iiO", &X, &Y, &_Fin)) {
        return NULL;
    }

    Fin = PySequence_Fast(_Fin, "argument must be iterable");

    if(!Fin)
        return NULL;

    fprintf(stderr, "X = %d, Y = %d\n", X, Y);

    for (int i = 0; i < X * Y; i ++) {
    	PyObject *b = PySequence_Fast_GET_ITEM(Fin, i);

    	fprintf(stderr, "%d: %d\n", i, b == Py_True);
    }

    Py_DECREF(Fin);
    return NULL;
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
