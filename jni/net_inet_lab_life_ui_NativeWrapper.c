#include <stdlib.h>
#include <stdio.h>
#include <sys/time.h>


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

   life_run(cells, cells, X, Y, 1, NULL);

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

struct _run_cb_data {
   int X;
   int Y;

   struct timeval tv_start;
   double tout;

   JNIEnv * env;
   jobject java_cb;
   jmethodID mid;
   jbooleanArray Fout;
};

static void run_cb(void * _cb_data, int iter, int count, void * f, int fin, int * p_stop) {
   *p_stop = 0;

   if (iter == 0)
      return;
   struct _run_cb_data * cb_data = (struct _run_cb_data *) _cb_data;

   struct timeval tv_now, tv_diff;
   gettimeofday(&tv_now, NULL);

   timersub(&tv_now, &cb_data->tv_start, &tv_diff);
   double d = tv_diff.tv_sec + 1.0e-6 * tv_diff.tv_usec;

   if (fin || d > cb_data->tout) {
      cb_data->tv_start = tv_now;
      unsigned char * cells = malloc (cb_data->X * cb_data->Y);
      life_extract_cells (f, cells);
      (*cb_data->env)->SetBooleanArrayRegion(cb_data->env, cb_data->Fout, 0,
         cb_data->X*cb_data->Y, cells);
      int res = (*cb_data->env)->CallIntMethod(cb_data->env, cb_data->java_cb,
         cb_data->mid, iter, count, fin, cb_data->Fout);
      *p_stop = res == 1;
      free(cells);
   }
}

JNIEXPORT void JNICALL Java_net_inet_1lab_life_ui_NativeWrapper__1run
  (JNIEnv * env, jclass _, jint X, jint Y, jdouble tout, jbooleanArray Fin, jbooleanArray Fout,jobject java_cb
) {
   jclass cls = (*env)->GetObjectClass(env, java_cb);
   jmethodID mid = (*env)->GetMethodID(env, cls, "report", "(III[Z)I");

   if (mid == 0) {
      fprintf(stderr,"Cannot find 'report'\n");
      return;
   }

   struct _run_cb_data cb_data;
   cb_data.X = X;
   cb_data.Y = Y;
   gettimeofday(&cb_data.tv_start, NULL);
   cb_data.tout = tout;
   cb_data.env = env;
   cb_data.java_cb = java_cb;
   cb_data.mid = mid;
   cb_data.Fout = Fout;

   liferun_cb_t liferun_cb;
   liferun_cb.cb_ptr = run_cb;
   liferun_cb.cb_data = &cb_data;

   jboolean * fin = (*env)->GetBooleanArrayElements(env, Fin, 0);

   life_run(fin, NULL, X, Y, -1, &liferun_cb);
}

