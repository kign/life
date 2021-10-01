from distutils.core import setup, Extension
from os import listdir, mkdir
from shutil import copy

try :
    mkdir('lib')
    for f in listdir('../lib') :
        if f.endswith('.c') or f.endswith('.h') :
            copy('../lib/' + f, 'lib')
except (FileExistsError, FileNotFoundError) :
    pass

mod_life = Extension('conway_life',
      include_dirs = ['lib'],
      sources = ['python_life.c', 'lib/liferun.c', 'lib/lifestep.c'])

setup(ext_modules=[mod_life])
