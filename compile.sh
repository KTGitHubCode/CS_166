#!/bin/sh

# Set the port to match startDB.sh and createDB.sh
export PGPORT=1126

# Export classpath with the PostgreSQL driver
export CLASSPATH=$CLASSPATH:$PWD/pg73jdbc3.jar

# Compile the Java program
javac EmbeddedSQL.java

# Run the Java program using the "nfl_team" database name, port, and username
java EmbeddedSQL "nfl_team" $PGPORT $USER