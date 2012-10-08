cd "$(dirname "$(which "$0")")"

export JAVA_HOME=/usr/lib/jvm/java-6-openjdk
export PATH="$PATH:$(readlink -f ../apache-maven/bin)"

mvn clean package
