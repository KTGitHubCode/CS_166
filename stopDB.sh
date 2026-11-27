#! /bin/bash
folder=/data/backup/$USER
export PGDATA=$folder/myDB/data
export PGSOCKETS=$folder/myDB/sockets
export PGPORT=1126
pg_ctl -D $folder/myDB/data -o "-c unix_socket_directories=$PGSOCKETS -p $PGPORT" stop