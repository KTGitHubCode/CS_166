#! /bin/bash
export PGDATA=/data/backup/$USER/myDB/data
export PGPORT=1126
echo "creating db named ... nfl_team"
createuser -h localhost -p $PGPORT --createdb $USER
createdb -h localhost -p $PGPORT "nfl_team"
pg_ctl status