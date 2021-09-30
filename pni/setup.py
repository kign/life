from distutils.core import setup, Extension
from os.path import exists

prefix = '..' if exists('../lib/liferun.c') else '.'

mod_life = Extension('conway_life',
      include_dirs = [prefix + '/lib'],
      sources = ['python_life.c', prefix + '/lib/liferun.c', prefix + '/lib/lifestep.c'])

setup(ext_modules=[mod_life])
