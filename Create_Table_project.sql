-- ============================================================================
-- NFL Franchise Operations & Analytics Database (3NF Schema)
-- ============================================================================

-- ============================================================================
-- 0. CLEAN TEARDOWN (Allows re-running this script multiple times cleanly)
-- ============================================================================
DROP VIEW IF EXISTS vw_franchise_payroll CASCADE;

-- Drop associative & event log tables first
DROP TABLE IF EXISTS Works_on CASCADE;
DROP TABLE IF EXISTS Inspects CASCADE;
DROP TABLE IF EXISTS Manages_reg_staff CASCADE;
DROP TABLE IF EXISTS Practices_In CASCADE;
DROP TABLE IF EXISTS Analyze_Opposing CASCADE;
DROP TABLE IF EXISTS Analyze_Team CASCADE;
DROP TABLE IF EXISTS Opposing_Plays CASCADE;
DROP TABLE IF EXISTS Team_Plays CASCADE;
DROP TABLE IF EXISTS Coach_In_Game CASCADE;
DROP TABLE IF EXISTS Who_Coaches CASCADE;

-- Drop 1:1 weak-entity cumulative statistics tables
DROP TABLE IF EXISTS Special_Teams_Stats_Away CASCADE;
DROP TABLE IF EXISTS Special_Teams_Stats_Home CASCADE;
DROP TABLE IF EXISTS Defensive_Stats_Away CASCADE;
DROP TABLE IF EXISTS Defensive_Stats_Home CASCADE;
DROP TABLE IF EXISTS Offensive_Stats_Away CASCADE;
DROP TABLE IF EXISTS Offensive_Stats_Home CASCADE;

-- Drop core entity tables
DROP TABLE IF EXISTS Practices CASCADE;
DROP TABLE IF EXISTS Games CASCADE;
DROP TABLE IF EXISTS Regular_Staff CASCADE;
DROP TABLE IF EXISTS Trainers CASCADE;
DROP TABLE IF EXISTS Team_Doctors CASCADE;
DROP TABLE IF EXISTS Statistical_Team CASCADE;
DROP TABLE IF EXISTS Coaches CASCADE;
DROP TABLE IF EXISTS Opposing_Players CASCADE;
DROP TABLE IF EXISTS Team_Players CASCADE;
DROP TABLE IF EXISTS Managers CASCADE;

-- Drop trigger functions
DROP FUNCTION IF EXISTS fn_enforce_roster_caps() CASCADE;
DROP FUNCTION IF EXISTS fn_validate_mentorship() CASCADE;
DROP FUNCTION IF EXISTS fn_validate_coach_hierarchy() CASCADE;
DROP FUNCTION IF EXISTS fn_block_inactive_team_plays() CASCADE;
DROP FUNCTION IF EXISTS fn_require_regular_staff_manager() CASCADE;

-- ============================================================================
-- 1. CORE ENTITY TABLES
-- ============================================================================

-- Managers(mid, fname, lname, age, salary, rank, hire_date, experience)
CREATE TABLE Managers (
    mid INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    age INT CHECK (age >= 18),
    salary NUMERIC(12, 2) CHECK (salary >= 0),
    rank VARCHAR(100),
    hire_date DATE,
    experience INT CHECK (experience >= 0)
);

-- Team_Players(pid, fname, lname, height, weight, age, salary, hire_date,
--              experience, health_status, regular_practice_squad,
--              jersey_number, positions, mid, mentor_pid)
CREATE TABLE Team_Players (
    pid INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    height FLOAT CHECK (height > 0),
    weight FLOAT CHECK (weight > 0),
    age INT CHECK (age >= 18),
    salary NUMERIC(12, 2) CHECK (salary >= 0),
    hire_date DATE,
    experience INT CHECK (experience >= 0),
    health_status VARCHAR(50) NOT NULL DEFAULT 'Healthy',
    regular_practice_squad VARCHAR(30) NOT NULL
        CHECK (regular_practice_squad IN ('Regular', 'Practice Squad')),
    jersey_number INT CHECK (jersey_number BETWEEN 0 AND 99),
    positions VARCHAR(100) NOT NULL,
    mid INT NOT NULL REFERENCES Managers(mid),
    mentor_pid INT REFERENCES Team_Players(pid),
    CONSTRAINT chk_no_self_mentorship CHECK (mentor_pid IS NULL OR mentor_pid <> pid),
    CONSTRAINT chk_team_players_valid_positions CHECK (
        positions ~ '^(QB|RB|HB|TB|FB|WR|TE|OL|OT|LT|RT|T|OG|LG|RG|G|C|DL|DE|DT|NT|EDGE|LB|ILB|MLB|OLB|DB|CB|S|FS|SS|K|PK|P|LS|KR|PR)(\s*,\s*(QB|RB|HB|TB|FB|WR|TE|OL|OT|LT|RT|T|OG|LG|RG|G|C|DL|DE|DT|NT|EDGE|LB|ILB|MLB|OLB|DB|CB|S|FS|SS|K|PK|P|LS|KR|PR))*$'
    )
);

-- Opposing_Players(opid, fname, lname, height, weight, age, experience, team, positions)
CREATE TABLE Opposing_Players (
    opid INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    height FLOAT CHECK (height > 0),
    weight FLOAT CHECK (weight > 0),
    age INT CHECK (age >= 18),
    experience INT CHECK (experience >= 0),
    team VARCHAR(100) NOT NULL,
    positions VARCHAR(100) NOT NULL,
    CONSTRAINT chk_opposing_players_valid_positions CHECK (
        positions ~ '^(QB|RB|HB|TB|FB|WR|TE|OL|OT|LT|RT|T|OG|LG|RG|G|C|DL|DE|DT|NT|EDGE|LB|ILB|MLB|OLB|DB|CB|S|FS|SS|K|PK|P|LS|KR|PR)(\s*,\s*(QB|RB|HB|TB|FB|WR|TE|OL|OT|LT|RT|T|OG|LG|RG|G|C|DL|DE|DT|NT|EDGE|LB|ILB|MLB|OLB|DB|CB|S|FS|SS|K|PK|P|LS|KR|PR))*$'
    )
);

-- Coaches(cid, fname, lname, salary, age, gender, hire_date, experience,
--         position, defensive_offensive, mid, reports_to_cid)
CREATE TABLE Coaches (
    cid INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    salary NUMERIC(12, 2) CHECK (salary >= 0),
    age INT CHECK (age >= 18),
    gender VARCHAR(30),
    hire_date DATE,
    experience INT CHECK (experience >= 0),
    position VARCHAR(200) NOT NULL,
    defensive_offensive VARCHAR(30)
        CHECK (defensive_offensive IN ('Offensive', 'Defensive', 'Special Teams', 'Both')),
    mid INT NOT NULL REFERENCES Managers(mid),
    reports_to_cid INT REFERENCES Coaches(cid),
    CONSTRAINT chk_no_self_reporting CHECK (reports_to_cid IS NULL OR reports_to_cid <> cid)
);

-- Statistical_Team(stid, fname, lname, salary, hire_date, experience, focus)
CREATE TABLE Statistical_Team (
    stid INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    salary NUMERIC(12, 2) CHECK (salary >= 0),
    hire_date DATE,
    experience INT CHECK (experience >= 0),
    focus VARCHAR(30) NOT NULL
        CHECK (focus IN ('Offensive', 'Defensive', 'Both'))
);

-- Team_Doctors(did, fname, lname, salary, age, gender, hire_date, experience, speciality)
CREATE TABLE Team_Doctors (
    did INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    salary NUMERIC(12, 2) CHECK (salary >= 0),
    age INT CHECK (age >= 18),
    gender VARCHAR(30),
    hire_date DATE,
    experience INT CHECK (experience >= 0),
    speciality VARCHAR(200)
);

-- Trainers(tid, fname, lname, salary, age, gender, hire_date, experience)
CREATE TABLE Trainers (
    tid INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    salary NUMERIC(12, 2) CHECK (salary >= 0),
    age INT CHECK (age >= 18),
    gender VARCHAR(30),
    hire_date DATE,
    experience INT CHECK (experience >= 0)
);

-- Regular_Staff(jib, fname, lname, salary, age, experience, job_title, gender)
CREATE TABLE Regular_Staff (
    jib INT PRIMARY KEY,
    fname VARCHAR(50) NOT NULL,
    lname VARCHAR(100) NOT NULL,
    salary NUMERIC(12, 2) CHECK (salary >= 0),
    age INT CHECK (age >= 16),
    experience INT CHECK (experience >= 0),
    job_title VARCHAR(100) NOT NULL,
    gender VARCHAR(30)
);

-- Games(gid, date_of_game, home_team, away_team, play_by_play, home_team_plays, away_team_plays)
CREATE TABLE Games (
    gid INT PRIMARY KEY,
    date_of_game DATE NOT NULL UNIQUE,
    home_team VARCHAR(100) NOT NULL,
    away_team VARCHAR(100) NOT NULL,
    play_by_play TEXT,
    home_team_plays TEXT,
    away_team_plays TEXT
);

-- Practices(date_of_practice, morning_afternoon)
CREATE TABLE Practices (
    date_of_practice DATE NOT NULL,
    morning_afternoon VARCHAR(15) NOT NULL
        CHECK (morning_afternoon IN ('Morning', 'Afternoon')),
    PRIMARY KEY (date_of_practice, morning_afternoon)
);

-- ============================================================================
-- 2. 1:1 WEAK-ENTITY CUMULATIVE STATISTICS TABLES
-- ============================================================================

-- Offensive_Stats_Home(pid, ...)
-- Signed yardage columns (passing_yards, rushing_yards, receiving_yards, yards_gained_behind)
-- intentionally omit >= 0 constraints to allow negative total yards.
CREATE TABLE Offensive_Stats_Home (
    pid INT PRIMARY KEY REFERENCES Team_Players(pid) ON DELETE CASCADE,
    snaps_played INT DEFAULT 0 CHECK (snaps_played >= 0),
    passing_attempts INT DEFAULT 0 CHECK (passing_attempts >= 0),
    completions INT DEFAULT 0 CHECK (completions >= 0),
    passing_yards INT DEFAULT 0,          -- Allows negative total passing yards
    rushing_attempts INT DEFAULT 0 CHECK (rushing_attempts >= 0),
    rushing_yards INT DEFAULT 0,          -- Allows negative total rushing yards
    targets INT DEFAULT 0 CHECK (targets >= 0),
    catches INT DEFAULT 0 CHECK (catches >= 0),
    receiving_yards INT DEFAULT 0,        -- Allows negative total receiving yards
    blocking_attempts INT DEFAULT 0 CHECK (blocking_attempts >= 0),
    sacks_allowed INT DEFAULT 0 CHECK (sacks_allowed >= 0),
    pressures_allowed INT DEFAULT 0 CHECK (pressures_allowed >= 0),
    yards_gained_behind FLOAT DEFAULT 0,
    fumbles INT DEFAULT 0 CHECK (fumbles >= 0),
    penalties INT DEFAULT 0 CHECK (penalties >= 0)
);

-- Offensive_Stats_Away(opid, ...)
CREATE TABLE Offensive_Stats_Away (
    opid INT PRIMARY KEY REFERENCES Opposing_Players(opid) ON DELETE CASCADE,
    snaps_played INT DEFAULT 0 CHECK (snaps_played >= 0),
    passing_attempts INT DEFAULT 0 CHECK (passing_attempts >= 0),
    completions INT DEFAULT 0 CHECK (completions >= 0),
    passing_yards INT DEFAULT 0,          -- Allows negative total passing yards
    rushing_attempts INT DEFAULT 0 CHECK (rushing_attempts >= 0),
    rushing_yards INT DEFAULT 0,          -- Allows negative total rushing yards
    targets INT DEFAULT 0 CHECK (targets >= 0),
    catches INT DEFAULT 0 CHECK (catches >= 0),
    receiving_yards INT DEFAULT 0,        -- Allows negative total receiving yards
    blocking_attempts INT DEFAULT 0 CHECK (blocking_attempts >= 0),
    sacks_allowed INT DEFAULT 0 CHECK (sacks_allowed >= 0),
    pressures_allowed INT DEFAULT 0 CHECK (pressures_allowed >= 0),
    yards_gained_behind FLOAT DEFAULT 0,
    fumbles INT DEFAULT 0 CHECK (fumbles >= 0),
    penalties INT DEFAULT 0 CHECK (penalties >= 0)
);

-- Defensive_Stats_Home(pid, ...)
CREATE TABLE Defensive_Stats_Home (
    pid INT PRIMARY KEY REFERENCES Team_Players(pid) ON DELETE CASCADE,
    snaps_played INT DEFAULT 0 CHECK (snaps_played >= 0),
    tackles INT DEFAULT 0 CHECK (tackles >= 0),
    sacks NUMERIC(5, 1) DEFAULT 0 CHECK (sacks >= 0),
    pressures INT DEFAULT 0 CHECK (pressures >= 0),
    pass_deflections INT DEFAULT 0 CHECK (pass_deflections >= 0),
    interceptions INT DEFAULT 0 CHECK (interceptions >= 0),
    fumbles_forced INT DEFAULT 0 CHECK (fumbles_forced >= 0),
    fumbles_recovered INT DEFAULT 0 CHECK (fumbles_recovered >= 0),
    penalties INT DEFAULT 0 CHECK (penalties >= 0)
);

-- Defensive_Stats_Away(opid, ...)
CREATE TABLE Defensive_Stats_Away (
    opid INT PRIMARY KEY REFERENCES Opposing_Players(opid) ON DELETE CASCADE,
    snaps_played INT DEFAULT 0 CHECK (snaps_played >= 0),
    tackles INT DEFAULT 0 CHECK (tackles >= 0),
    sacks NUMERIC(5, 1) DEFAULT 0 CHECK (sacks >= 0),
    pressures INT DEFAULT 0 CHECK (pressures >= 0),
    pass_deflections INT DEFAULT 0 CHECK (pass_deflections >= 0),
    interceptions INT DEFAULT 0 CHECK (interceptions >= 0),
    fumbles_forced INT DEFAULT 0 CHECK (fumbles_forced >= 0),
    fumbles_recovered INT DEFAULT 0 CHECK (fumbles_recovered >= 0),
    penalties INT DEFAULT 0 CHECK (penalties >= 0)
);

-- Special_Teams_Stats_Home(pid, ...)
-- Signed yardage columns (punt_yards, kick_return_yards, punt_return_yards)
-- intentionally omit >= 0 constraints to allow negative total yards.
CREATE TABLE Special_Teams_Stats_Home (
    pid INT PRIMARY KEY REFERENCES Team_Players(pid) ON DELETE CASCADE,
    snaps_played INT DEFAULT 0 CHECK (snaps_played >= 0),
    punt_attempts INT DEFAULT 0 CHECK (punt_attempts >= 0),
    punt_yards INT DEFAULT 0,             -- Allows negative total punting yards
    field_goal_attempts INT DEFAULT 0 CHECK (field_goal_attempts >= 0),
    field_goals_made INT DEFAULT 0 CHECK (field_goals_made >= 0),
    field_goal_kick_lengths INT[],
    extra_point_attempts INT DEFAULT 0 CHECK (extra_point_attempts >= 0),
    extra_points_made INT DEFAULT 0 CHECK (extra_points_made >= 0),
    punts_blocked INT DEFAULT 0 CHECK (punts_blocked >= 0),
    kick_return_yards INT DEFAULT 0,      -- Allows negative total kick return yards
    punt_return_yards INT DEFAULT 0,      -- Allows negative total punt return yards
    penalties INT DEFAULT 0 CHECK (penalties >= 0)
);

-- Special_Teams_Stats_Away(opid, ...)
CREATE TABLE Special_Teams_Stats_Away (
    opid INT PRIMARY KEY REFERENCES Opposing_Players(opid) ON DELETE CASCADE,
    snaps_played INT DEFAULT 0 CHECK (snaps_played >= 0),
    punt_attempts INT DEFAULT 0 CHECK (punt_attempts >= 0),
    punt_yards INT DEFAULT 0,             -- Allows negative total punting yards
    field_goal_attempts INT DEFAULT 0 CHECK (field_goal_attempts >= 0),
    field_goals_made INT DEFAULT 0 CHECK (field_goals_made >= 0),
    field_goal_kick_lengths INT[],
    extra_point_attempts INT DEFAULT 0 CHECK (extra_point_attempts >= 0),
    extra_points_made INT DEFAULT 0 CHECK (extra_points_made >= 0),
    punts_blocked INT DEFAULT 0 CHECK (punts_blocked >= 0),
    kick_return_yards INT DEFAULT 0,      -- Allows negative total kick return yards
    punt_return_yards INT DEFAULT 0,      -- Allows negative total punt return yards
    penalties INT DEFAULT 0 CHECK (penalties >= 0)
);

-- ============================================================================
-- 3. MANY-TO-MANY ASSOCIATIVE & EVENT LOG TABLES
-- ============================================================================

-- Who_Coaches(cid, pid, affiliation, game_plan, date)
CREATE TABLE Who_Coaches (
    cid INT NOT NULL REFERENCES Coaches(cid),
    pid INT NOT NULL REFERENCES Team_Players(pid),
    affiliation VARCHAR(100),
    game_plan TEXT,
    date DATE NOT NULL,
    PRIMARY KEY (cid, pid, date)
);

-- Coach_In_Game(cid, gid)
CREATE TABLE Coach_In_Game (
    cid INT NOT NULL REFERENCES Coaches(cid),
    gid INT NOT NULL REFERENCES Games(gid),
    PRIMARY KEY (cid, gid)
);

-- Team_Plays(pid, gid, list_of_plays, list_of_stats)
CREATE TABLE Team_Plays (
    pid INT NOT NULL REFERENCES Team_Players(pid),
    gid INT NOT NULL REFERENCES Games(gid),
    list_of_plays TEXT[],
    list_of_stats TEXT[],
    PRIMARY KEY (pid, gid)
);

-- Opposing_Plays(opid, gid, list_of_plays, list_of_stats)
CREATE TABLE Opposing_Plays (
    opid INT NOT NULL REFERENCES Opposing_Players(opid),
    gid INT NOT NULL REFERENCES Games(gid),
    list_of_plays TEXT[],
    list_of_stats TEXT[],
    PRIMARY KEY (opid, gid)
);

-- Analyze_Team(stid, pid, analysis, date)
CREATE TABLE Analyze_Team (
    stid INT NOT NULL REFERENCES Statistical_Team(stid),
    pid INT NOT NULL REFERENCES Team_Players(pid),
    analysis TEXT,
    date DATE NOT NULL,
    PRIMARY KEY (stid, pid, date)
);

-- Analyze_Opposing(stid, opid, analysis, date)
CREATE TABLE Analyze_Opposing (
    stid INT NOT NULL REFERENCES Statistical_Team(stid),
    opid INT NOT NULL REFERENCES Opposing_Players(opid),
    analysis TEXT,
    date DATE NOT NULL,
    PRIMARY KEY (stid, opid, date)
);

-- Practices_In(pid, date_of_practice, morning_afternoon, did_practice, excused_unexcused)
CREATE TABLE Practices_In (
    pid INT NOT NULL REFERENCES Team_Players(pid),
    date_of_practice DATE NOT NULL,
    morning_afternoon VARCHAR(15) NOT NULL,
    did_practice BOOLEAN NOT NULL,
    excused_unexcused VARCHAR(15)
        CHECK (excused_unexcused IN ('Excused', 'Unexcused') OR excused_unexcused IS NULL),
    FOREIGN KEY (date_of_practice, morning_afternoon)
        REFERENCES Practices(date_of_practice, morning_afternoon),
    PRIMARY KEY (pid, date_of_practice, morning_afternoon)
);

-- Manages_reg_staff(mid, jib, days, schedule)
CREATE TABLE Manages_reg_staff (
    mid INT NOT NULL REFERENCES Managers(mid),
    jib INT NOT NULL REFERENCES Regular_Staff(jib),
    days INT CHECK (days BETWEEN 1 AND 7),
    schedule VARCHAR(100)[],
    PRIMARY KEY (mid, jib)
);

-- Inspects(did, pid, diagnosis, treatment_length, date)
CREATE TABLE Inspects (
    did INT NOT NULL REFERENCES Team_Doctors(did),
    pid INT NOT NULL REFERENCES Team_Players(pid),
    diagnosis VARCHAR(300),
    treatment_length INT CHECK (treatment_length >= 0),
    date DATE NOT NULL,
    PRIMARY KEY (did, pid, date)
);

-- Works_on(tid, pid, condition_of_player, recommends, date)
CREATE TABLE Works_on (
    tid INT NOT NULL REFERENCES Trainers(tid),
    pid INT NOT NULL REFERENCES Team_Players(pid),
    condition_of_player VARCHAR(200),
    recommends BOOLEAN,
    date DATE NOT NULL,
    PRIMARY KEY (tid, pid, date)
);

-- ============================================================================
-- 4. PARTIAL UNIQUE INDEXES, VIEWS & PROCEDURAL CONSTRAINTS (SECTION 5)
-- ============================================================================

-- Active regular-roster players must hold unique jersey numbers within the franchise
CREATE UNIQUE INDEX idx_active_roster_jersey_unique
    ON Team_Players (jersey_number)
    WHERE regular_practice_squad = 'Regular' AND jersey_number IS NOT NULL;

-- At most one Head Coach in the franchise
CREATE UNIQUE INDEX idx_single_head_coach
    ON Coaches (position)
    WHERE position = 'Head Coach';

-- Consolidated franchise payroll view across all 7 employee tables
CREATE OR REPLACE VIEW vw_franchise_payroll AS
SELECT 'Players' AS dept, COUNT(*) AS headcount, COALESCE(SUM(salary), 0) AS payroll FROM Team_Players
UNION ALL SELECT 'Coaches', COUNT(*), COALESCE(SUM(salary), 0) FROM Coaches
UNION ALL SELECT 'Managers', COUNT(*), COALESCE(SUM(salary), 0) FROM Managers
UNION ALL SELECT 'Team Doctors', COUNT(*), COALESCE(SUM(salary), 0) FROM Team_Doctors
UNION ALL SELECT 'Trainers', COUNT(*), COALESCE(SUM(salary), 0) FROM Trainers
UNION ALL SELECT 'Statistical Team', COUNT(*), COALESCE(SUM(salary), 0) FROM Statistical_Team
UNION ALL SELECT 'Regular Staff', COUNT(*), COALESCE(SUM(salary), 0) FROM Regular_Staff;

-- Enforce 53-man active roster and 17-man practice squad caps (excluding IR players)
CREATE OR REPLACE FUNCTION fn_enforce_roster_caps()
RETURNS TRIGGER AS $$
DECLARE
    v_count INT;
BEGIN
    -- Players on Injured Reserve ('IR') do not count toward active or practice squad caps
    IF NEW.health_status = 'IR' THEN
        RETURN NEW;
    END IF;

    IF NEW.regular_practice_squad = 'Regular' THEN
        SELECT COUNT(*) INTO v_count
        FROM Team_Players
        WHERE regular_practice_squad = 'Regular'
          AND health_status <> 'IR'
          AND pid <> NEW.pid;
        IF v_count >= 53 THEN
            RAISE EXCEPTION 'Active Regular roster cap (53, excluding IR) exceeded.';
        END IF;
    ELSIF NEW.regular_practice_squad = 'Practice Squad' THEN
        SELECT COUNT(*) INTO v_count
        FROM Team_Players
        WHERE regular_practice_squad = 'Practice Squad'
          AND health_status <> 'IR'
          AND pid <> NEW.pid;
        IF v_count >= 17 THEN
            RAISE EXCEPTION 'Practice Squad roster cap (17, excluding IR) exceeded.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_enforce_roster_caps ON Team_Players;

CREATE TRIGGER trg_enforce_roster_caps
BEFORE INSERT OR UPDATE OF regular_practice_squad, health_status ON Team_Players
FOR EACH ROW
EXECUTE PROCEDURE fn_enforce_roster_caps();

-- Prevent mentorship graph cycles and require mentor.experience > mentee.experience
CREATE OR REPLACE FUNCTION fn_validate_mentorship()
RETURNS TRIGGER AS $$
DECLARE
    v_mentor_exp INT;
    v_cycle_found BOOLEAN;
BEGIN
    IF NEW.mentor_pid IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT experience INTO v_mentor_exp
    FROM Team_Players
    WHERE pid = NEW.mentor_pid;

    IF v_mentor_exp IS NOT NULL AND NEW.experience IS NOT NULL AND v_mentor_exp <= NEW.experience THEN
        RAISE EXCEPTION 'Mentor experience (%) must exceed mentee experience (%).', v_mentor_exp, NEW.experience;
    END IF;

    WITH RECURSIVE mentor_chain AS (
        SELECT mentor_pid AS ancestor_pid
        FROM Team_Players
        WHERE pid = NEW.mentor_pid
        UNION ALL
        SELECT tp.mentor_pid
        FROM Team_Players tp
        JOIN mentor_chain mc ON tp.pid = mc.ancestor_pid
        WHERE tp.mentor_pid IS NOT NULL
    )
    SELECT EXISTS (
        SELECT 1 FROM mentor_chain WHERE ancestor_pid = NEW.pid
    ) INTO v_cycle_found;

    IF v_cycle_found THEN
        RAISE EXCEPTION 'Cycle detected in Team_Players mentorship hierarchy for pid %.', NEW.pid;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_mentorship
BEFORE INSERT OR UPDATE OF mentor_pid, experience ON Team_Players
FOR EACH ROW
EXECUTE PROCEDURE fn_validate_mentorship();

-- Prevent multi-step reporting cycles in Coaches.reports_to_cid
CREATE OR REPLACE FUNCTION fn_validate_coach_hierarchy()
RETURNS TRIGGER AS $$
DECLARE
    v_cycle_found BOOLEAN;
BEGIN
    IF NEW.reports_to_cid IS NULL THEN
        RETURN NEW;
    END IF;

    WITH RECURSIVE reporting_chain AS (
        SELECT reports_to_cid AS supervisor_cid
        FROM Coaches
        WHERE cid = NEW.reports_to_cid
        UNION ALL
        SELECT c.reports_to_cid
        FROM Coaches c
        JOIN reporting_chain rc ON c.cid = rc.supervisor_cid
        WHERE c.reports_to_cid IS NOT NULL
    )
    SELECT EXISTS (
        SELECT 1 FROM reporting_chain WHERE supervisor_cid = NEW.cid
    ) INTO v_cycle_found;

    IF v_cycle_found THEN
        RAISE EXCEPTION 'Cycle detected in Coaches reporting hierarchy for cid %.', NEW.cid;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_coach_hierarchy
BEFORE INSERT OR UPDATE OF reports_to_cid ON Coaches
FOR EACH ROW
EXECUTE PROCEDURE fn_validate_coach_hierarchy();

-- Block 'Out' or 'IR' players from logging game participation in Team_Plays
CREATE OR REPLACE FUNCTION fn_block_inactive_team_plays()
RETURNS TRIGGER AS $$
DECLARE
    v_status VARCHAR(50);
BEGIN
    SELECT health_status INTO v_status
    FROM Team_Players
    WHERE pid = NEW.pid;

    IF v_status IN ('Out', 'IR') THEN
        RAISE EXCEPTION 'Player % cannot play in a game while health_status is %.', NEW.pid, v_status;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_block_inactive_team_plays
BEFORE INSERT OR UPDATE OF pid ON Team_Plays
FOR EACH ROW
EXECUTE PROCEDURE fn_block_inactive_team_plays();

-- Require mandatory managerial assignment in Manages_reg_staff for every Regular_Staff hire
CREATE OR REPLACE FUNCTION fn_require_regular_staff_manager()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM Manages_reg_staff WHERE jib = NEW.jib
    ) THEN
        RAISE EXCEPTION 'Regular_Staff member % must have at least one manager in Manages_reg_staff.', NEW.jib;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_require_regular_staff_manager
AFTER INSERT ON Regular_Staff
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE PROCEDURE fn_require_regular_staff_manager();


-- ============================================================================
-- LOAD DATA FROM PIPE-DELIMITED TEXT FILES (each \copy MUST be on a single line)
-- ============================================================================

-- 1. Core Entity Tables
\copy Managers (mid, fname, lname, age, salary, rank, hire_date, experience) FROM './all_current_data/Managers.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Team_Players (pid, fname, lname, height, weight, age, salary, hire_date, experience, health_status, regular_practice_squad, jersey_number, positions, mid, mentor_pid) FROM './all_current_data/Team_Players.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Opposing_Players (opid, fname, lname, height, weight, age, experience, team, positions) FROM './all_current_data/Opposing_Players.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Coaches (cid, fname, lname, salary, age, gender, hire_date, experience, position, defensive_offensive, mid, reports_to_cid) FROM './all_current_data/Coaches.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Statistical_Team (stid, fname, lname, salary, hire_date, experience, focus) FROM './all_current_data/Statistical_Team.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Team_Doctors (did, fname, lname, salary, age, gender, hire_date, experience, speciality) FROM './all_current_data/Team_Doctors.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Trainers (tid, fname, lname, salary, age, gender, hire_date, experience) FROM './all_current_data/Trainers.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

BEGIN;
\copy Regular_Staff (jib, fname, lname, salary, age, experience, job_title, gender) FROM './all_current_data/Regular_Staff.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);
\copy Manages_reg_staff (mid, jib, days, schedule) FROM './all_current_data/Manages_reg_staff.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);
COMMIT;

\copy Games (gid, date_of_game, home_team, away_team, play_by_play, home_team_plays, away_team_plays) FROM './all_current_data/Games.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Practices (date_of_practice, morning_afternoon) FROM './all_current_data/Practices.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

-- 2. 1:1 Cumulative Statistics Tables
\copy Offensive_Stats_Home (pid, snaps_played, passing_attempts, completions, passing_yards, rushing_attempts, rushing_yards, targets, catches, receiving_yards, blocking_attempts, sacks_allowed, pressures_allowed, yards_gained_behind, fumbles, penalties) FROM './all_current_data/Offensive_Stats_Home.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Offensive_Stats_Away (opid, snaps_played, passing_attempts, completions, passing_yards, rushing_attempts, rushing_yards, targets, catches, receiving_yards, blocking_attempts, sacks_allowed, pressures_allowed, yards_gained_behind, fumbles, penalties) FROM './all_current_data/Offensive_Stats_Away.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Defensive_Stats_Home (pid, snaps_played, tackles, sacks, pressures, pass_deflections, interceptions, fumbles_forced, fumbles_recovered, penalties) FROM './all_current_data/Defensive_Stats_Home.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Defensive_Stats_Away (opid, snaps_played, tackles, sacks, pressures, pass_deflections, interceptions, fumbles_forced, fumbles_recovered, penalties) FROM './all_current_data/Defensive_Stats_Away.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Special_Teams_Stats_Home (pid, snaps_played, punt_attempts, punt_yards, field_goal_attempts, field_goals_made, field_goal_kick_lengths, extra_point_attempts, extra_points_made, punts_blocked, kick_return_yards, punt_return_yards, penalties) FROM './all_current_data/Special_Teams_Stats_Home.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Special_Teams_Stats_Away (opid, snaps_played, punt_attempts, punt_yards, field_goal_attempts, field_goals_made, field_goal_kick_lengths, extra_point_attempts, extra_points_made, punts_blocked, kick_return_yards, punt_return_yards, penalties) FROM './all_current_data/Special_Teams_Stats_Away.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

-- 3. Many-to-Many Associative & Event Log Tables
\copy Who_Coaches (cid, pid, affiliation, game_plan, date) FROM './all_current_data/Who_Coaches.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Coach_In_Game (cid, gid) FROM './all_current_data/Coach_In_Game.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Team_Plays (pid, gid, list_of_plays, list_of_stats) FROM './all_current_data/Team_Plays.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Opposing_Plays (opid, gid, list_of_plays, list_of_stats) FROM './all_current_data/Opposing_Plays.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Analyze_Team (stid, pid, analysis, date) FROM './all_current_data/Analyze_Team.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Analyze_Opposing (stid, opid, analysis, date) FROM './all_current_data/Analyze_Opposing.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Practices_In (pid, date_of_practice, morning_afternoon, did_practice, excused_unexcused) FROM './all_current_data/Practices_In.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Inspects (did, pid, diagnosis, treatment_length, date) FROM './all_current_data/Inspects.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

\copy Works_on (tid, pid, condition_of_player, recommends, date) FROM './all_current_data/Works_on.txt' WITH (FORMAT csv, DELIMITER '|', HEADER false);

