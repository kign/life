# Ideally we should use -d ../lifefx/out/production/lifefx/
# but this might conflict with IntelliJ compilation
# if Java versions conflict
javac -h . -d out ../lifefx/src/net/inet_lab/life/ui/NativeWrapper.java
gcc -c -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin net_inet_lab_life_ui_NativeWrapper.c -o net_inet_lab_life_ui_NativeWrapper.o
gcc -L../lib -dynamiclib -o liblifenative.dylib net_inet_lab_life_ui_NativeWrapper.o -lc -lliferun

javap -s -cp ../lifefx/out/production/lifefx net.inet_lab.life.ui.NativeWrapper.runCallback

ls -l liblifenative.dylib
