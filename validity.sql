/* It can't be empty: Use NOT NULL.
It must be unique: Use PRIMARY KEY or UNIQUE.
It must exist in another table: Use FOREIGN KEY.
It must follow a specific rule: Use CHECK (e.g., movie year must be positive). */


-- ==========================================
-- CS166 PART C: DATA VALIDITY TESTS
-- ==========================================

-- Each test uses a savepoint so that an invalid
-- INSERT can be rolled back without stopping
-- the rest of the tests.

BEGIN;

-- ------------------------------------------
-- TEST 1: Managers - age must be >= 18
-- ------------------------------------------

SAVEPOINT test1;

INSERT INTO Managers
VALUES (999, 'John', 'Smith', 15, 50000, 'A', '2026-01-01', NULL);

ROLLBACK TO SAVEPOINT test1;


-- ------------------------------------------
-- TEST 2: Managers - salary must be > 0
-- ------------------------------------------

SAVEPOINT test2;

INSERT INTO Managers
VALUES (998, 'John', 'Smith', 30, -5000, 'A', '2026-01-01', NULL);

ROLLBACK TO SAVEPOINT test2;


-- ------------------------------------------
-- TEST 3: Managers - first name cannot be NULL
-- ------------------------------------------

SAVEPOINT test3;

INSERT INTO Managers
VALUES (997, NULL, 'Smith', 30, 50000, 'A', '2026-01-01', NULL);

ROLLBACK TO SAVEPOINT test3;


-- ------------------------------------------
-- TEST 4: Managers - primary key must be unique
-- Requires manager ID 1 to already exist.
-- ------------------------------------------

SAVEPOINT test4;

INSERT INTO Managers
VALUES (1, 'Alex', 'Brown', 35, 60000, 'A', '2026-01-01', NULL);

ROLLBACK TO SAVEPOINT test4;


-- ------------------------------------------
-- TEST 5: Team_Players - height must be > 0
-- Requires manager ID 1 to exist.
-- ------------------------------------------

SAVEPOINT test5;

INSERT INTO Team_Players
VALUES (999, 'John', 'Smith', -1, 180, 25, 100000,
        '2026-01-01', NULL, 'Healthy', 'Regular',
        99, 1, NULL);

ROLLBACK TO SAVEPOINT test5;


-- ------------------------------------------
-- TEST 6: Team_Players - weight must be > 0
-- ------------------------------------------

SAVEPOINT test6;

INSERT INTO Team_Players
VALUES (998, 'John', 'Smith', 6.0, -180, 25, 100000,
        '2026-01-01', NULL, 'Healthy', 'Regular',
        98, 1, NULL);

ROLLBACK TO SAVEPOINT test6;


-- ------------------------------------------
-- TEST 7: Team_Players - age must be >= 18
-- ------------------------------------------

SAVEPOINT test7;

INSERT INTO Team_Players
VALUES (997, 'John', 'Smith', 6.0, 180, 15, 100000,
        '2026-01-01', NULL, 'Healthy', 'Regular',
        97, 1, NULL);

ROLLBACK TO SAVEPOINT test7;


-- ------------------------------------------
-- TEST 8: Team_Players - salary must be > 0
-- ------------------------------------------

SAVEPOINT test8;

INSERT INTO Team_Players
VALUES (996, 'John', 'Smith', 6.0, 180, 25, -100000,
        '2026-01-01', NULL, 'Healthy', 'Regular',
        96, 1, NULL);

ROLLBACK TO SAVEPOINT test8;


-- ------------------------------------------
-- TEST 9: Team_Players - jersey number UNIQUE
-- Requires another player to already have jersey 99.
-- ------------------------------------------

SAVEPOINT test9;

INSERT INTO Team_Players
VALUES (995, 'Jane', 'Smith', 6.0, 180, 25, 100000,
        '2026-01-01', NULL, 'Healthy', 'Regular',
        99, 1, NULL);

ROLLBACK TO SAVEPOINT test9;


-- ------------------------------------------
-- TEST 10: Team_Players - foreign key manager
-- Manager ID 99999 must not exist.
-- ------------------------------------------

SAVEPOINT test10;

INSERT INTO Team_Players
VALUES (994, 'Alex', 'Smith', 6.0, 180, 25, 100000,
        '2026-01-01', NULL, 'Healthy', 'Regular',
        94, 99999, NULL);

ROLLBACK TO SAVEPOINT test10;


-- ------------------------------------------
-- TEST 11: Statistical_Team - invalid focus
-- ------------------------------------------

SAVEPOINT test11;

INSERT INTO Statistical_Team
VALUES (999, 'Alex', 'Smith', 50000,
        'special', '2026-01-01', NULL);

ROLLBACK TO SAVEPOINT test11;


-- ------------------------------------------
-- TEST 12: Coaches - invalid salary
-- ------------------------------------------

SAVEPOINT test12;

INSERT INTO Coaches
VALUES (999, 'Alex', 'Smith', -5000, NULL, 30,
        'Head Coach', '2026-01-01', NULL,
        'offensive', NULL);

ROLLBACK TO SAVEPOINT test12;


-- ------------------------------------------
-- TEST 13: Games - duplicate game date
-- Requires another game on 2026-11-25.
-- ------------------------------------------

SAVEPOINT test13;

INSERT INTO Games
VALUES (999, '2026-11-25', 'Team A', 'Team B',
        NULL, NULL, NULL);

ROLLBACK TO SAVEPOINT test13;


-- ------------------------------------------
-- TEST 14: Practices - duplicate primary key
-- Requires this practice date/time to exist.
-- ------------------------------------------

SAVEPOINT test14;

INSERT INTO Practices
VALUES ('2026-11-25', 'morning');

ROLLBACK TO SAVEPOINT test14;


-- Undo any successful test inserts.
ROLLBACK;

-- ==========================================
-- END OF VALIDITY TESTS
-- ==========================================
