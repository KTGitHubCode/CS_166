#! /bin/bash
echo "creating db named ... "$USER"_Project"
createdb -h localhost -p $PGPORT $USER"_Project"
pg_ctl status
