-- Q5. Flight Hopping

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q5 CASCADE;

CREATE TABLE q5 (
	destination CHAR(3),
	num_flights INT
);


DROP VIEW IF EXISTS FlightHopping  CASCADE;
CREATE VIEW FlightHopping AS
WITH RECURSIVE FlightHopping AS (
	SELECT
		f.outbound,
		f.inbound,
		f.s_arv,
		1 as num_flights
	FROM
		flight as f, q5_parameters as q5
	WHERE
		outbound = 'YYZ' AND date(f.s_dep) = date(q5.day) 
		
	UNION
		SELECT
			f1.outbound,
			f1.inbound,
			f1.s_arv,
			fh.num_flights +1
		FROM
		q5_parameters as q5, flight f1
		INNER JOIN FlightHopping fh
		      ON fh.inbound = f1.outbound
		      AND f1.s_dep < fh.s_arv + INTERVAL '1 DAY'
		      AND f1.s_dep >=  fh.s_arv
		WHERE fh.num_flights < q5.n
)
SELECT * FROM	FlightHopping;;

INSERT INTO q5
SELECT inbound as destination, num_flights FROM	FlightHopping;

















