#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <time.h>
#include <assert.h>

// gcc -Wall -O2 -I../lib -L../lib -o old_unittests old_unittests.c -lliferun

#include "liferun.h"

/* We will need simple platfrom-independent random numbers generator.
 * Taking a hint from http://www.die.net/doc/linux/man/man3/rand.3.html
 */

#define MYRAND_MAX 32767
static unsigned long next = 1;

static int myrand(void) {
   next = next * 1103515245 + 12345;
   return (unsigned)(next/65536) % 32768;
}

static void mysrand(unsigned seed) {
   next = seed;
}

static void randomize (unsigned char * cells, int X, int Y, double density, unsigned seed);
static unsigned get_old_hash (unsigned char *f, int X, int Y);

void test (int x, int y, int N, unsigned _h0, unsigned _h1) {
   clock_t              c0, c1;
   time_t               t0, t1;
   unsigned             h0, h1;
   double               lapse;

   unsigned char * cells = malloc (x * y);
   randomize ( cells, x, y, 0.3, 1 );

   c0 = clock ();
   h0 = get_old_hash (cells, x, y);
   t0 = time (NULL);

   int n_iter = life_run(cells, x, y, N, NULL);

   c1 = clock ();
   h1 = get_old_hash (cells, x, y);
   t1 = time (NULL);

   lapse = (c1 - c0 + 0.0)/CLOCKS_PER_SEC;

   fprintf ( stderr, "%d steps %d_%d took %f (%d real) seconds (%f fps)\n",
             n_iter, x, y, lapse, (int)(t1 - t0), N/lapse );
   fprintf ( stderr, "Hashes: 0x%08X -> 0x%08X\n", h0, h1 );

   assert ( _h0 == h0 );
   assert ( _h1 == h1 );

   free(cells);
}

static void randomize (unsigned char * cells, int X, int Y, double density, unsigned seed) {
   if (seed)
      mysrand ( seed % MYRAND_MAX );

   for (int y = 0; y < Y; y ++ )
      for (int x = 0; x < X; x ++ )
         if (myrand() < density * MYRAND_MAX)
            cells[y * X + x] = 1;
         else
            cells[y * X + x] = 0;
}

static unsigned get_old_hash (unsigned char *f, int X, int Y) {
   /* quick-and-dirty way to generate 32-bit digest of the play field */
   int                  ind = 0;
   unsigned int         res = 0;

   for (int y = 0; y < Y; y ++ )
      for (int x = 0; x < X; x ++ )
         if (f[y * X + x] == 1) {
            ind = (ind + 1)%32;
            res ^= ((x*x + y + 1) << ind);
         }

   return res;
}

static void main_smoke (void) {
   test ( 100, 100, 2, 0xD7F4F066, 0x0F1B4001 );
}

static void main_benchmark (void) {
   test ( 100,  100,  5000, 0xD7F4F066, 0x3D484A18 );
   test ( 800,  600,  100,  0xC6A7471D, 0x29C07F40 );
   test ( 1600, 1200, 25,   0x22D82AFC, 0x7B0270DD );
   test ( 1000, 1000, 100,  0x980E00DD, 0x83A152D0 );
}

int main ( int argc, char * argv[] ) {
   if ( argc >= 2 && strcmp ( argv[1], "-smoke" ) == 0 )
      main_smoke ();
   else
      main_benchmark ();

   return 0;
}

/* new copernicus (MacBook Pro (13-inch, 2019, Four Thunderbolt 3 ports)
2028 steps 100_100 took 0.087666 (0 real) seconds (57034.654256 fps)
Hashes: 0xD7F4F066 -> 0x3D484A18
100 steps 800_600 took 0.473867 (1 real) seconds (211.029677 fps)
Hashes: 0xC6A7471D -> 0x29C07F40
25 steps 1600_1200 took 0.713694 (1 real) seconds (35.029018 fps)
Hashes: 0x22D82AFC -> 0x7B0270DD
100 steps 1000_1000 took 1.017586 (1 real) seconds (98.271792 fps)
Hashes: 0x980E00DD -> 0x83A152D0
*/
