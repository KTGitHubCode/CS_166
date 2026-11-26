SELECT s.stid,
       s.fname || ' ' || s.lname AS analyst_name,
       s.focus AS analyst_focus,
       'Team Player' AS target_type,
       tp.pid AS player_id,
       tp.fname || ' ' || tp.lname AS player_name,
       'Franchise Roster' AS team,
       tp.positions,
       at.date AS analysis_date,
       at.analysis
FROM Statistical_Team s
JOIN Analyze_Team at ON s.stid = at.stid
JOIN Team_Players tp ON at.pid = tp.pid

UNION ALL

SELECT s.stid,
       s.fname || ' ' || s.lname AS analyst_name,
       s.focus AS analyst_focus,
       'Opposing Player' AS target_type,
       op.opid AS player_id,
       op.fname || ' ' || op.lname AS player_name,
       op.team,
       op.positions,
       ao.date AS analysis_date,
       ao.analysis
FROM Statistical_Team s
JOIN Analyze_Opposing ao ON s.stid = ao.stid
JOIN Opposing_Players op ON ao.opid = op.opid

ORDER BY analysis_date DESC, analyst_name, player_name;