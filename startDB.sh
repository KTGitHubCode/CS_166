#! /bin/bash
folder=/data/backup/$USER
export PGDATA=$folder/myDB/data
export PGSOCKETS=$folder/myDB/sockets

echo $folder

#Clear folder
rm -rf $folder
#! /bin/bash
folder=/data/backup/$USER
export PGDATA=$folder/myDB/data
export PGSOCKETS=$folder/myDB/sockets

echo $folder

#Clear folder
rm -rf $folder

#Initialize folders
mkdir -p $folder/myDB/data
mkdir -p $folder/myDB/sockets
sleep 1

#Initialize DB
initdb

sleep 1
#Start folder
export PGPORT=1126
pg_ctl -o "-c unix_socket_directories=$PGSOCKETS -p $PGPORT" start