from distutils.core import setup, Extension

# Build with this command:
# python3 setup.py build_ext -I ../lib -L ../lib -lliferun && python3 setup.py install

def main():
    setup(name="life",
          version="1.0.0",
          description="Python interface for game of life",
          author="Konstantin Ignatiev",
          author_email="kostya@gmail.com",
          ext_modules=[Extension("life", ["python_life.c"])])

if __name__ == "__main__":
    main()
