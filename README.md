Saving Da League

How to Run the Database Application

Follow these steps in order to start the PostgreSQL database server, load the schema, compile the Java application, and cleanly shut down when finished.

1. Start the Database Server

Initialize and start the PostgreSQL server instance:

source ./startDB.sh


2. Create the Database

Create the nfl_team database:

source ./createDB.sh


3. Load the Schema & Initial Data

Populate the database tables by importing the SQL script on port 1126:

cs166_psql -p 1126 nfl_team < Create_Table_project.sql


4. Compile and Run the Application

Compile the Java source files and launch the interactive terminal interface:

source ./compile.sh


(Once compiled, the application should run and display the main menu.)

5. Stop the Database Server

When you are done using the application, make sure to shut down the PostgreSQL server before logging out:

source ./stopDB.sh
