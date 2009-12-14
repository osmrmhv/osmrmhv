cd "$(dirname "$(readlink -m "$(which "$0")")")"

if [ ! -e classgen ]; then
	darcs get http://darcs.brianweb.net/classgen/ 
else
	cd classgen
	darcs pull
	cd ..
fi

cd classgen
make
cd ..
IFS="
"
gcj -c sqlite-jdbc.jar $(find classgen/src -type f -name "*.java") -o sqlite-jdbc.o
ar rcs sqlite-jdbc.a sqlite-jdbc.o
gcj --shared -Wl,-soname,sqlite-jdbc.so -o sqlite-jdbc.so sqlite-jdbc.o
