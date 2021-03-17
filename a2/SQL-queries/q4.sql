-- Q4. Plane Capacity Histogram

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q4 CASCADE;

CREATE TABLE q4 (
	airline CHAR(2),
	tail_number CHAR(5),
	very_low INT,
	low INT,
	fair INT,
	normal INT,
	high INT
);


DROP VIEW IF EXISTS  FlightBookings  CASCADE;
CREATE VIEW FlightBookings as 
SELECT d.flight_id, count(b.id) as bookings, max(capacity_economy+capacity_business+capacity_first) as cap,
       (count(b.id)::numeric(7,2)/ max(capacity_economy+capacity_business+capacity_first)::numeric(7,2)) as percent
FROM  (flight as f LEFT JOIN  booking as b on b.flight_id=f.id), plane as p, departure as d
WHERE d.flight_id=f.id
AND f.airline=p.airline
AND f.plane=p.tail_number
GROUP BY d.flight_id;

DROP VIEW IF EXISTS  FlightCapacity  CASCADE;
CREATE VIEW FlightCapacity as 
SELECT flight_id, bookings, cap,
       CASE WHEN percent>=0   AND percent<0.2 THEN 'very_low'
       	    WHEN percent>=0.2 AND percent<0.4 THEN 'low'
	    WHEN percent>=0.4 AND percent<0.6 THEN 'fair'
	    WHEN percent>=0.6 AND percent<0.8 THEN 'normal'
	    WHEN percent>=0.8 THEN 'high'
       END as flightType
FROM FlightBookings;

DROP VIEW IF EXISTS  FlightCapType  CASCADE;
CREATE VIEW FlightCapType as 
SELECT p.airline, p.tail_number, count(fc.flight_id) as count, flighttype
FROM (plane as p LEFT JOIN flight as f ON f.airline=p.airline AND f.plane=p.tail_number) 
     LEFT JOIN FlightCapacity as fc  ON  fc.flight_id=f.id
GROUP BY p.airline, p.tail_number, flighttype;


DROP VIEW IF EXISTS  FlightFinal  CASCADE;
CREATE VIEW FlightFinal as 
SELECT airline, tail_number, 
       CASE WHEN flighttype = 'very_low'  THEN count ELSE 0
       END as very_low,
       CASE WHEN flighttype = 'low'  THEN count ELSE 0
       END as low,
       CASE WHEN flighttype = 'fair'  THEN count ELSE 0
       END as fair,
       CASE WHEN flighttype = 'normal'  THEN count ELSE 0
       END as normal,
       CASE WHEN flighttype = 'high'  THEN count ELSE 0
       END as high
FROM FlightCapType;


INSERT INTO q4
SELECT airline, tail_number, max(very_low) very_low, max(low) low, max(fair) fair, max(normal) normal, max(high) high
FROM FlightFinal
GROUP BY airline, tail_number;
