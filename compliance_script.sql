-- 1. Confirm Roster Caps (Expected: Regular = 53 Compliant, Practice Squad = 17 Compliant)
SELECT regular_practice_squad,
       COUNT(pid) AS player_count,
       CASE
           WHEN regular_practice_squad = 'Regular' AND COUNT(pid) > 53 THEN 'OVER LIMIT'
           WHEN regular_practice_squad = 'Practice Squad' AND COUNT(pid) > 17 THEN 'OVER LIMIT'
           ELSE 'Compliant'
       END AS status
FROM Team_Players
GROUP BY regular_practice_squad;

-- 2. Confirm Shared Jersey Numbers Across Active vs. Practice Squad
SELECT jersey_number, pid, fname, lname, regular_practice_squad
FROM Team_Players
WHERE jersey_number IN (12, 28, 54, 81)
ORDER BY jersey_number, regular_practice_squad;

-- 3. Confirm Negative Yardage Totals Were Preserved
SELECT tp.fname, tp.lname, o.passing_yards, o.rushing_yards, o.receiving_yards
FROM Team_Players tp
JOIN Offensive_Stats_Home o ON tp.pid = o.pid
WHERE o.passing_yards < 0 OR o.rushing_yards < 0 OR o.receiving_yards < 0;

-- 4. Confirm Full Franchise Payroll Across All 7 Employee Tables
SELECT * FROM vw_franchise_payroll;

