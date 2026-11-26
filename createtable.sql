-- Managers(mid (primary key),  fname, lname, age, salary, rank, hire date, experience)

CREATE TABLE Managers (
    mid INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    age INT NOT NULL CHECK (age >= 18), 
    salary FLOAT NOT NULL CHECK (salary > 0), 
    ranks VARCHAR(100) NOT NULL,
    hire_date DATE NOT NULL,
    experience VARCHAR(500)
);

-- Team_Players(pid (primary key), fname, lname, height, weight, age, salary, hire date, experience, health status, 
-- regular/practice squad, (unique) jersey number, managed by (mid, foreign key), pid (foreign key))

CREATE TABLE Team_Players (
    pid INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    height FLOAT NOT NULL CHECK (height > 0),
    weight FLOAT NOT NULL CHECK (weight > 0),
    age INT NOT NULL CHECK (age >= 18),
    salary FLOAT NOT NULL CHECK (salary > 0),
    hire_date DATE NOT NULL,
    experience VARCHAR(500),
    health_status VARCHAR(300) NOT NULL,
    practice_squad VARCHAR(500) NOT NULL,
    jersey_num INT UNIQUE,
    manager_mid INT NOT NULL,
    mentor_pid INT,
    FOREIGN KEY (manager_mid) REFERENCES Managers(mid),
    FOREIGN KEY (mentor_pid) REFERENCES Team_Players(pid)
);

-- Opposing Players(opid (primary key), fname, lname, height, weight, age, experience, team)

CREATE TABLE Opposing_Players (
    opid INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    height FLOAT NOT NULL CHECK (height > 0),
    weight FLOAT NOT NULL CHECK (weight > 0),
    age INT NOT NULL CHECK (age >= 18),
    experience VARCHAR(500),
    team VARCHAR(100) NOT NULL
);

-- Special Team Positions(opid or pid (foreign key), position name, snaps played, punt attempts, punt yards, field goal attempts, 
-- field goals made, average kick length, extra point attempts, extra points made, punts blocked, kick return yards, 
-- punt return yards, penalties)

CREATE TABLE Special_Team_Positions (
    position_name VARCHAR(100) NOT NULL,
    snaps_played VARCHAR(100),
    punts_attempts INT,
    punt_yards FLOAT,
    field_goal_attempts INT,
    field_goals_made INT,
    average_kick_length FLOAT,
    extra_point_attempts INT,
    punts_blocked INT,
    kick_return_yards FLOAT,
    punt_return_yards FLOAT,
    penalties INT,
    team_players_pid INT,
    extra_points_made INT,
    FOREIGN KEY (team_players_pid) REFERENCES Team_Players(pid),
    PRIMARY KEY (team_players_pid, position_name)
);

-- Offensive Positions(opid or pid (foreign key), position name, snaps played, passing attempts, completions, passing yards, 
-- rushing attempts, rushing yards, targets, catches, receiving yards, blocking attempts, sacks allowed, pressures allowed, 
-- yards gained behind, fumbles, penalties)

CREATE TABLE Offensive_Positions (
    position_name VARCHAR(100) NOT NULL,
    snaps_played VARCHAR(100),
    passing_attempts INT,
    completions INT,
    passing_yards FLOAT,
    rushing_attempts INT,
    rushing_yards FLOAT,
    targets INT,
    catches INT,
    recieving_yards FLOAT,
    blocking_attempts INT,
    sacks_allowed INT,
    pressures_allowed INT,
    yards_gained_behind FLOAT,
    fumbles INT,
    penalties INT,
    team_players_pid INT,
    FOREIGN KEY (team_players_pid) REFERENCES Team_Players(pid),
    PRIMARY KEY (team_players_pid, position_name)
);

-- Defensive Positions(opid (foreign key), position name, snaps played, tackles, sacks, pressures, pass deflections, 
-- interceptions, fumbles forced, fumbles recovered, penalties)

CREATE TABLE Defensive_Positions (
    position_name VARCHAR(100) NOT NULL,
    snaps_played VARCHAR(100),
    tackles INT,
    sacks INT,
    pressures INT,
    pass_deflections INT,
    interceptions INT,
    fumbled_forced INT,
    fumbles_recovered INT,
    penalties INT,
    team_players_pid INT,
    FOREIGN KEY (team_players_pid) REFERENCES Team_Players(pid),
    PRIMARY KEY (team_players_pid, position_name)
);

-- Statistical Team(stid (primary key), fname, lname, salary, hire date, experience, focus) 
-- (focus is defensive, offensive, or both)

CREATE TABLE Statistical_Team (
    stid INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    salary FLOAT NOT NULL CHECK (salary > 0), 
    focus VARCHAR(100) NOT NULL
        CHECK (focus IN ('defensive', 'offensive', 'both')),
    hire_date DATE NOT NULL,
    experience VARCHAR(500)
);

-- Team_Doctors(did (primary key), fname, lname, salary, age, gender, hire date, experience, speciality)

CREATE TABLE Team_Doctors (
    did INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    salary FLOAT NOT NULL CHECK (salary > 0), 
    gender VARCHAR(30),
    speciality VARCHAR(200) NOT NULL,
    hire_date DATE NOT NULL,
    experience VARCHAR(500)
);

-- Coaches(cid (primary key), fname, lname, salary, age, gender, hire date, experience, position, defensive/offensive, 
-- hired by (mid,foreign key))

CREATE TABLE Coaches (
    cid INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    salary FLOAT NOT NULL CHECK (salary > 0), 
    gender VARCHAR(30),
    age INT NOT NULL CHECK (age >= 18),
    position VARCHAR(200) NOT NULL,
    hire_date DATE NOT NULL,
    experience VARCHAR(500),
    type_of_team VARCHAR(10) NOT NULL,
    hired_by INT REFERENCES Managers(mid)
);

-- Regular_Staff(jib (primary key), fname, lname, salary, age, experience, job title, gender)

CREATE TABLE Regular_Staff (
    jib INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    salary FLOAT NOT NULL CHECK (salary > 0), 
    job_title VARCHAR(100) NOT NULL,
    age INT NOT NULL CHECK (age >= 18),
    gender VARCHAR(30),
    experience VARCHAR(500)
);

-- Games(gid (primary key), date of game (unique), home team, away team, play by play, home team plays, away team plays)

CREATE TABLE Games (
    gid INT PRIMARY KEY,
    date_of_game DATE NOT NULL UNIQUE,
    home_team VARCHAR(100) NOT NULL,
    away_team VARCHAR(100) NOT NULL,
    play_by_play VARCHAR(100),
    home_team_plays VARCHAR(100),
    away_team_plays VARCHAR(100)
);

-- Practices(date of practice, morning/afternoon (previous 2 make the primary key))

CREATE TABLE Practices (
    date_of_practice DATE NOT NULL,
    time_of_day VARCHAR(10) NOT NULL,
    primary key(date_of_practice, time_of_day)
);

-- Trainers(tid (primary key), fname, lname, salary, age, gender, hire date, experience)

CREATE TABLE Trainers (
    tid INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    salary FLOAT NOT NULL CHECK (salary > 0), 
    age INT NOT NULL CHECK (age >= 18),
    gender VARCHAR(30),
    hire_date DATE NOT NULL,
    experience VARCHAR(500)
);

-- Who_Coaches(cid (foreign key), pid (foreign key), affiliation, game plan, date) Primary key is (cid,pid,date)
-- Relationship 2: Who_Coaches (coaches, team players)
-- Mandatory Many to Mandatory Many (coaches, team players)

CREATE TABLE Who_Coaches (
    coach_cid INT NOT NULL,
    FOREIGN KEY (coach_cid) REFERENCES Coaches(cid),
    team_players_pid INT NOT NULL,
    FOREIGN KEY (team_players_pid) REFERENCES Team_Players(pid),
    affiliation VARCHAR(50), 
    game_plan VARCHAR(300),
    coaches_date DATE NOT NULL,
    primary key(coach_cid, team_players_pid, coaches_date)
);

-- Coach_In_Game(cid (foreign key), gid (foreign key)) Primary key is (cid,gid)
-- Relationship 3: coaches in (coaches, games)
-- Mandatory Many to Mandatory Many (coaches, games)

CREATE TABLE Coach_In_Game (
    coach_cid INT NOT NULL,
    FOREIGN KEY (coach_cid) REFERENCES Coaches(cid),
    games_gid INT NOT NULL,
    FOREIGN KEY (games_gid) REFERENCES Games(gid),
    primary key(coach_cid, games_gid)
);

-- Relationship 4: reports to (coaches, coaches)
-- Mandatory many to optional one (coaches, coaches)

CREATE TABLE Coach_reports_to (
    reporter_cid INT NOT NULL,
    FOREIGN KEY (reporter_cid) REFERENCES Coaches(cid),
    coach_cid INT NOT NULL,
    FOREIGN KEY (coach_cid) REFERENCES Coaches(cid),
    primary key(coach_cid, reporter_cid)
);

-- Opposing_Plays(opid (foreign key), gid (foreign key), list of plays, list of stats) Primary key is (opid,gid)
-- Relationship 2: played game (away) (games, opposing players) 
-- Mandatory Many to Optional Many (games, opposing players)

CREATE TABLE Opposing_Plays (
    opposing_opid INT NOT NULL,
    FOREIGN KEY (opposing_opid) REFERENCES Opposing_Players(opid),
    games_gid INT NOT NULL,
    FOREIGN KEY (games_gid) REFERENCES Games(gid),
    list_of_plays VARCHAR(50)[],
    list_of_stats VARCHAR(50)[],
    primary key(opposing_opid, games_gid)
);

-- Team_Plays(pid (foreign key), gid (foreign key), list of plays, list of stats) Primary key is (pid,gid)
-- Relationship 3: played game (home) (games, team players)
-- Mandatory Many to Optional Many (games, team players)

CREATE TABLE Team_Plays (
    team_players_pid INT NOT NULL,
    FOREIGN KEY (team_players_pid) REFERENCES Team_Players(pid),
    games_gid INT NOT NULL,
    FOREIGN KEY (games_gid) REFERENCES Games(gid),
    list_of_plays VARCHAR(50)[],
    list_of_stats VARCHAR(50)[],
    primary key(team_players_pid, games_gid)
);

-- Analyze_Team(stid (foreign key), pid (foreign key), analysis, date) Primary key is (stid,pid,date)
-- Relationship 1: Analyze team (statistical team, team players)
-- Mandatory Many to Optional Many (statistical team, team players)

CREATE TABLE Analyze_Team (
    stats_stid INT NOT NULL,
    FOREIGN KEY (stats_stid) REFERENCES Statistical_Team(stid),
    team_players_pid INT NOT NULL,
    FOREIGN KEY (team_players_pid) REFERENCES Team_Players(pid),
    analyze_date DATE NOT NULL,
    anaylsis VARCHAR(200) NOT NULL,
    primary key(team_players_pid, stats_stid, analyze_date)
);

-- Analyze_Opposing(stid (foreign key), opid (foreign key), analysis, date) Primary key is (stid,opid,date)
-- Relationship 2: Analyze away (statistical team, opposing players)
-- Mandatory Many to Optional Many (statistical team, opposing players)

CREATE TABLE Analyze_Opposing (
    stats_stid INT NOT NULL,
    FOREIGN KEY (stats_stid) REFERENCES Statistical_Team(stid),
    opposing_opid INT NOT NULL,
    FOREIGN KEY (opposing_opid) REFERENCES Opposing_Players(opid),
    analyze_date DATE NOT NULL,
    anaylsis VARCHAR(200) NOT NULL,
    PRIMARY KEY (opposing_opid, stats_stid, analyze_date)
);

-- Practices_In(pid (foreign key), (date of practice, morning/afternoon), did practice) 
-- Primary key is (pid, date of practice, morning/afternoon)
-- Relationship 1: did practice (practice, team players)
-- Mandatory Many to Optional Many (practice, team players)

CREATE TABLE Practices_In (
    team_players_pid INT NOT NULL,
    FOREIGN KEY (team_players_pid) REFERENCES Team_Players(pid),
    did_practice BOOLEAN NOT NULL,
    practice_date DATE NOT NULL,
    practice_time VARCHAR(10) NOT NULL,
    FOREIGN KEY (practice_date, practice_time) REFERENCES Practices(date_of_practice, time_of_day),
    primary key(team_players_pid, practice_date, practice_time)
);

-- Manages_reg_staff(mid (foreign key), jib (foreign key), days, schedule) Primary key is (mid,jib)
-- Relationship 1: manages_reg_staff (regular staff, managers)
-- Mandatory Many to Optional Many (regular staff, managers)

CREATE TABLE Manages_reg_staff (
    managers_mid INT NOT NULL,
    FOREIGN KEY (managers_mid) REFERENCES Managers(mid),
    reg_jib INT NOT NULL,
    FOREIGN KEY (reg_jib) REFERENCES Regular_Staff(jib),
    days INT NOT NULL,
    schedule VARCHAR(100)[],
    primary key(managers_mid, reg_jib)
);

-- Inspects(did (foreign key), pid (foreign key), diagnosis, treatment length, date) Primary key is (did, pid, date)
-- Relationship 1: inspects (Team Doctors, team players)
-- Optional Many to Optional Many (Team Doctors, team players)

CREATE TABLE Inspects (
    doc_did INT NOT NULL,
    FOREIGN KEY (doc_did) REFERENCES Team_Doctors(did),
    team_players_pid INT NOT NULL,
    FOREIGN KEY (team_players_pid) REFERENCES Team_Players(pid),
    diagnosis VARCHAR(100) NOT NULL,
    treatment_length INT NOT NULL,
    doc_date DATE NOT NULL,
    primary key(doc_did, team_players_pid, doc_date)
);

-- Works_on(tid (foreign key), pid (foreign key), condition of player, recommends, date) Primary key is (tid, pid,date)
-- Relationship 1: works on (Trainers, team Players)
-- Optional Many to Optional Many (Trainers, team Players)

CREATE TABLE Works_on (
    trainer_tid INT NOT NULL,
    FOREIGN KEY (trainer_tid) REFERENCES Trainers(tid),
    team_players_pid INT NOT NULL,
    FOREIGN KEY (team_players_pid) REFERENCES Team_Players(pid),
    condition_of_player VARCHAR(100),
    recommends BOOLEAN NOT NULL,
    trainer_date DATE NOT NULL,
    primary key(trainer_tid, team_players_pid, trainer_date)
);

-- Relationship 3: plays at offense (away) (opposing players, offensive positions)
-- Optional Many to Mandatory Many (opposing players, offensive positions)

CREATE TABLE plays_at_offense (
    opposing_team_opid INT NOT NULL,
    team_players_offense_pid INT NOT NULL,
    offense_name VARCHAR(100) NOT NULL,
    FOREIGN KEY (team_players_offense_pid, offense_name) REFERENCES Offensive_Positions(team_players_pid, position_name),
    FOREIGN KEY (opposing_team_opid) REFERENCES Opposing_Players(opid),
    PRIMARY KEY (opposing_team_opid, team_players_offense_pid, offense_name)
);

-- Relationship 4: plays at defense (away) (opposing players, defensive positions)
-- Optional Many to Mandatory Many (opposing players, defensive positions)

CREATE TABLE plays_at_defense (
    opposing_team_opid INT NOT NULL,
    team_players_defense_pid INT NOT NULL,
    defense_name VARCHAR(100) NOT NULL,
    FOREIGN KEY (team_players_defense_pid, defense_name) REFERENCES Defensive_Positions(team_players_pid, position_name),
    FOREIGN KEY (opposing_team_opid) REFERENCES Opposing_Players(opid),
    PRIMARY KEY (opposing_team_opid, team_players_defense_pid, defense_name)
);

-- Relationship 5: plays at special (away) (opposing players, special team positions)
-- Optional Many to Mandatory Many (opposing players, special team positions)

CREATE TABLE plays_at_special (
    opposing_team_opid INT NOT NULL,
    team_players_special_pid INT NOT NULL,
    special_name VARCHAR(100) NOT NULL,
    FOREIGN KEY (team_players_special_pid, special_name) REFERENCES Special_Team_Positions(team_players_pid, position_name),
    FOREIGN KEY (opposing_team_opid) REFERENCES Opposing_Players(opid),
    PRIMARY KEY (opposing_team_opid, team_players_special_pid, special_name)
);
