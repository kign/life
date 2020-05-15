from distutils.core import setup, Extension

def main():
    setup(name="life",
          version="1.0.0",
          description="Python interface for game of life",
          author="Konstantin Ignatiev",
          author_email="kostya@gmail.com",
          ext_modules=[Extension("life", ["python_life.c"])])

if __name__ == "__main__":
    main()
