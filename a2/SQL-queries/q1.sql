-- Q1. Airlines

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q1 CASCADE;

CREATE TABLE q1 (
    pass_id INT,
    name VARCHAR(100),
    airlines INT
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.
DROP VIEW IF EXISTS passID CASCADE;
DROP VIEW IF EXISTS arrived CASCADE;
DROP VIEW IF EXISTS airlineMatch CASCADE;


-- Define views for your intermediate steps here:
create view passID as 
Select passenger.id as pass_id,firstname,surname,flight_id
from passenger join booking on passenger.id=booking.pass_id;

create view arrived as
select pass_id,CONCAT(firstname, ' ', surname) AS name,arrival.flight_id 
from passID join arrival on arrival.flight_id=passID.flight_id;

create view airlineMatch as
select pass_id, name,count(distinct airline) as airlines
from arrived join flight on arrived.flight_id=flight.id
group by pass_id,name;



-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q1
select * from airlineMatch;
