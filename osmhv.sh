dir="$(dirname "$(readlink -m "$(which "$0")")")"

if [ -f "$dir/osmhv" ]; then
	LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$dir/dist:$dir/lib/cmdargs/dist:$dir/lib/sqlite-jdbc" "$dir/osmhv" "$@"
else
	java -Xmx256M -classpath "$dir/lib/sqlite-jdbc/sqlite-jdbc-3.6.14.1.jar:$dir/lib/cmdargs/dist/cmdargs.jar:$dir/dist/osmrmhvlib.jar:$dir/dist/osmhv.jar" de.cdauth.osm.historyviewer.Main "$@"
fi
