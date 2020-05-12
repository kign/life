#include <stdlib.h>
#include <stdio.h>

#include "net_inet_lab_life_ui_NativeWrapper.h"
#include "../lib/liferun.h"

JNIEXPORT void JNICALL Java_net_inet_1lab_life_ui_NativeWrapper__1oneStep
	(JNIEnv * env, jclass _, jint X, jint Y, jbooleanArray Fin, jbooleanArray Fout)
{
   unsigned char * cells;
   cells = malloc (X * Y * sizeof cells[0] );

   // last argument could be jboolean*, called isCopy, not sure how to use it
   jboolean * fin = (*env)->GetBooleanArrayElements(env, Fin, 0);

   for (int i = 0; i < X * Y; i ++) {
      cells[i] = fin[i];
   }

   // I don't know if we need this to avoid memory leak from fin allocation
   (*env)->ReleaseBooleanArrayElements(env, Fin, fin, 0);

   life_run(cells, X, Y, 1);

   // not sure if this is kosher?
   // Perpahs copying every element in a loop would be safer
   (*env)->SetBooleanArrayRegion(env, Fout, 0, X*Y, cells);

   // safe version
   // for (int i = 0; i < X * Y; i ++) {
   //    jboolean c = cells[i];
   //    (*env)->SetBooleanArrayRegion(env, Fout, i, 1, &c);
   // }

   free(cells);
}
