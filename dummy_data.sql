-- Fictional dummy data for the NFL-style database.
-- Run after successfully creating all tables.
-- Note: Coaches.hired_by should be INT and reference Managers(mid).
-- Note: plays_at_* currently reference home-team position tables in the supplied schema.

-- 1. Main entity tables

INSERT INTO Managers VALUES
(1, 'John', 'Smith', 48, 150000, 'Senior', '2010-03-15', '15 years'),
(2, 'David', 'Brown', 42, 120000, 'Junior', '2015-06-20', '10 years'),
(3, 'Linda', 'Wilson', 45, 135000, 'Senior', '2012-08-10', '12 years');

INSERT INTO Team_Players VALUES
(101, 'Alex', 'Johnson', 190, 95, 25, 500000, '2020-04-01', '5 years', 'Healthy', 'Regular', 10, 1, NULL),
(102, 'Michael', 'Davis', 185, 88, 23, 300000, '2022-05-10', '3 years', 'Healthy', 'Regular', 12, 2, 101),
(103, 'Daniel', 'Miller', 195, 100, 27, 600000, '2019-03-12', '7 years', 'Recovering', 'Regular', 15, 1, 101),
(104, 'James', 'Taylor', 180, 85, 24, 250000, '2023-04-15', '2 years', 'Healthy', 'Practice', 18, 3, 102);

INSERT INTO Opposing_Players VALUES
(201, 'Chris', 'Wilson', 188, 92, 26, '6 years', 'Tigers'),
(202, 'Robert', 'Anderson', 182, 87, 24, '4 years', 'Tigers'),
(203, 'Kevin', 'Thomas', 193, 98, 28, '8 years', 'Lions');

INSERT INTO Statistical_Team VALUES
(301, 'Emily', 'Clark', 90000, 'offensive', '2021-01-10', '5 years'),
(302, 'Ryan', 'Miller', 95000, 'defensive', '2019-02-15', '7 years'),
(303, 'Sophia', 'White', 85000, 'both', '2022-06-01', '4 years');

INSERT INTO Team_Doctors VALUES
(401, 'Sarah', 'Lee', 110000, 'Female', 'Sports Medicine', '2018-04-01', '8 years'),
(402, 'Thomas', 'King', 105000, 'Male', 'Orthopedics', '2020-05-12', '6 years');

INSERT INTO Coaches VALUES
(501, 'Robert', 'White', 130000, 'Male', 45, 'Head Coach', '2015-01-01', '15 years', 'offensive', '1'),
(502, 'Kevin', 'Green', 100000, 'Male', 38, 'Assistant Coach', '2020-02-01', '8 years', 'defensive', '2'),
(503, 'Mary', 'Adams', 95000, 'Female', 36, 'Assistant Coach', '2021-03-01', '6 years', 'offensive', '3');

INSERT INTO Regular_Staff VALUES
(601, 'Anna', 'Hall', 60000, 'Equipment Manager', 32, 'Female', '5 years'),
(602, 'Mark', 'Young', 55000, 'Team Assistant', 29, 'Male', '3 years'),
(603, 'Peter', 'Scott', 58000, 'Operations Coordinator', 34, 'Male', '7 years');

INSERT INTO Games VALUES
(701, '2026-09-10', 'Eagles', 'Tigers', 'Game 1', 'Home plays', 'Away plays'),
(702, '2026-09-17', 'Eagles', 'Lions', 'Game 2', 'Home plays', 'Away plays'),
(703, '2026-09-24', 'Eagles', 'Bears', 'Game 3', 'Home plays', 'Away plays');

INSERT INTO Practices VALUES
('2026-09-05', 'Morning'),
('2026-09-05', 'Afternoon'),
('2026-09-06', 'Morning');

INSERT INTO Trainers VALUES
(801, 'Peter', 'Adams', 75000, 35, 'Male', '2020-03-01', '6 years'),
(802, 'Laura', 'Baker', 80000, 40, 'Female', '2018-07-01', '9 years');

-- 2. Position tables

INSERT INTO Special_Team_Positions VALUES
('Kicker', 50, 0, 0, 5, 4, 45.5, 4, 0, 0, 0, 1, 101),
('Punter', 40, 20, 850, 0, 0, 42.5, 0, 1, 100, 200, 2, 104);

INSERT INTO Offensive_Positions VALUES
('Quarterback', 80, 30, 20, 250, 5, 20, 0, 0, 0, 0, 2, 3, 10, 1, 2, 101),
('Wide Receiver', 65, 0, 0, 0, 2, 15, 8, 6, 95, 1, 0, 1, 5, 0, 1, 102),
('Running Back', 70, 0, 0, 0, 15, 90, 3, 2, 15, 2, 0, 0, 8, 1, 1, 103);

INSERT INTO Defensive_Positions VALUES
('Linebacker', 70, 8, 2, 4, 1, 0, 1, 1, 2, 102),
('Cornerback', 65, 6, 0, 3, 4, 2, 0, 1, 1, 103);

-- 3. Relationship tables

INSERT INTO Who_Coaches VALUES
(501, 101, 'Head Coach', 'Passing strategy', '2026-09-01'),
(501, 102, 'Head Coach', 'Receiving strategy', '2026-09-01'),
(502, 103, 'Assistant', 'Defensive preparation', '2026-09-02'),
(503, 104, 'Assistant', 'Player development', '2026-09-02'),
(502, 101, 'Assistant', 'Defensive preparation', '2026-09-03');

INSERT INTO Coach_In_Game VALUES
(501, 701), (501, 702), (501, 703),
(502, 701), (502, 702), (502, 703),
(503, 701), (503, 702), (503, 703);

INSERT INTO Coach_reports_to VALUES
(502, 501),
(503, 501);

INSERT INTO Opposing_Plays VALUES
(201, 701, ARRAY['Run', 'Pass'], ARRAY['Yards', 'Touchdown']),
(202, 701, ARRAY['Pass'], ARRAY['Reception']),
(203, 702, ARRAY['Run'], ARRAY['Yards']);

INSERT INTO Team_Plays VALUES
(101, 701, ARRAY['Pass', 'Run'], ARRAY['Passing yards', 'Touchdown']),
(102, 701, ARRAY['Catch'], ARRAY['Receiving yards']),
(103, 702, ARRAY['Run'], ARRAY['Rushing yards']),
(104, 703, ARRAY['Kick'], ARRAY['Kick return yards']);

INSERT INTO Analyze_Team VALUES
(301, 101, '2026-09-11', 'Strong passing performance'),
(302, 102, '2026-09-12', 'Good defensive performance'),
(303, 103, '2026-09-13', 'Excellent overall performance');

INSERT INTO Analyze_Opposing VALUES
(301, 201, '2026-09-11', 'Strong offensive performance'),
(302, 202, '2026-09-12', 'Weak defensive performance'),
(303, 203, '2026-09-13', 'Good overall performance');

INSERT INTO Practices_In VALUES
(101, TRUE, '2026-09-05', 'Morning'),
(102, TRUE, '2026-09-05', 'Morning'),
(103, FALSE, '2026-09-05', 'Afternoon'),
(104, TRUE, '2026-09-06', 'Morning');

INSERT INTO Manages_reg_staff VALUES
(1, 601, 5, ARRAY['Monday', 'Tuesday']),
(2, 602, 3, ARRAY['Wednesday', 'Thursday']),
(3, 603, 4, ARRAY['Friday']);

INSERT INTO Inspects VALUES
(401, 101, 'Muscle strain', 5, '2026-09-10'),
(402, 103, 'Knee discomfort', 7, '2026-09-12');

INSERT INTO Works_on VALUES
(801, 101, 'Muscle strain', TRUE, '2026-09-10'),
(802, 103, 'Knee discomfort', TRUE, '2026-09-12'),
(801, 102, 'Routine assessment', FALSE, '2026-09-13');

-- 4. Away-player position relationships
-- WARNING: These currently reference home-team position rows in the supplied schema.

INSERT INTO plays_at_offense VALUES
(201, 101, 'Quarterback'),
(202, 102, 'Wide Receiver');

INSERT INTO plays_at_defense VALUES
(201, 102, 'Linebacker');

INSERT INTO plays_at_special VALUES
(202, 101, 'Kicker');
