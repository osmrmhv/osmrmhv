dir="$(dirname "$(readlink -m "$(which "$0")")")"
java -Xmx256M -classpath "$dir/lib/sqlite-jdbc/sqlite-jdbc-3.6.14.1.jar:$dir/lib/cmdargs/dist_src/cmdargs.jar:$dir/dist/lib/osmrmhv.jar" de.cdauth.osm.historyviewer.Main "$@"
