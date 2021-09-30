from distutils.core import setup, Extension

mod_life = Extension('conway_life',
      include_dirs = ['../lib'],
      sources = ['python_life.c', '../lib/liferun.c', '../lib/lifestep.c'])

setup(ext_modules=[mod_life])
