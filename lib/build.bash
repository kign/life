gcc -Wno-unused-result -Wsign-compare -Wunreachable-code -Wall -O2 -c liferun.c lifestep.c
rm -f libliferun.a
ar q libliferun.a liferun.o lifestep.o
