-- Q2. Refunds!

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q2 CASCADE;

CREATE TABLE q2 (
    airline CHAR(2),
    name VARCHAR(50),
    year CHAR(4),
    seat_class seat_class,
    refund REAL
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.

DROP VIEW IF EXISTS depInfo CASCADE;
DROP VIEW IF EXISTS flightInfo CASCADE;
DROP VIEW IF EXISTS delayedDep CASCADE;
DROP VIEW IF EXISTS domestic CASCADE;
DROP VIEW IF EXISTS intl CASCADE;
DROP VIEW IF EXISTS domesticPass CASCADE;
DROP VIEW IF EXISTS refundDomestic35 CASCADE;
DROP VIEW IF EXISTS refundDomestic50 CASCADE;
DROP VIEW IF EXISTS intlPass CASCADE;
DROP VIEW IF EXISTS refundIntl35 CASCADE;
DROP VIEW IF EXISTS refundIntl50 CASCADE;
DROP VIEW IF EXISTS joinedRefunds CASCADE;
DROP VIEW IF EXISTS allRefunds CASCADE;

-- Define views for your intermediate steps here:
create view depInfo as 
select id as flight_id,airline, s_dep, datetime as dep,outbound, inbound,s_arv
from flight join departure on departure.flight_id=flight.id; 

create view flightInfo as 
select arrival.flight_id,airline, s_dep,dep,outbound,inbound,s_arv,datetime as arrival
from depInfo join arrival on arrival.flight_id=depInfo.flight_id;

 -- taking into consideration arrival time of earlier than scheduled
create view delayedDep as 
select flight_id,airline, (dep-s_dep) as delayTime,outbound, inbound, EXTRACT(year FROM s_dep) as year, (arrival-s_arv) as arvDelay
from flightInfo
where (dep>s_dep) and (arrival-s_arv)>0.5*(dep-s_dep) and arrival>s_arv;


create view domestic as 
select flight_id,airline,a2.country as dest,a1.country as origin,delayTime,year
from delayedDep join airport as a1 on a1.code=outbound join airport as a2 on a2.code=inbound
where a1.country=a2.country;

create view intl as 
select flight_id,airline,a2.country as dest,a1.country as origin,delayTime,year
from delayedDep join airport as a1 on a1.code=outbound join airport as a2 on a2.code=inbound
where a1.country<>a2.country;

create view domesticPass as 
select domestic.flight_id,airline,delayTime,year,seat_class,price
from domestic join booking on domestic.flight_id=booking.flight_id ;

create view refundDomestic35 as 
select airline,year,seat_class, sum(price)*35/100 as refund 
from domesticPass
where delayTime>='5:00:00' and delayTime<'10:00:00'
group by airline,seat_class,year;

create view refundDomestic50 as 
select airline,year,seat_class, sum(price)*50/100 as refund 
from domesticPass
where delayTime>='10:00:00'
group by airline,seat_class,year;

create view intlPass as 
select intl.flight_id,airline,delayTime,year,seat_class,price
from intl join booking on intl.flight_id=booking.flight_id ;

create view refundIntl35 as 
select airline,year,seat_class, sum(price)*35/100 as refund 
from intlPass
where delayTime>='8:00:00' and delayTime<'12:00:00'
group by airline,seat_class,year;

create view refundIntl50 as 
select airline,year,seat_class, sum(price)*50/100 as refund 
from intlPass
where delayTime>='12:00:00'
group by airline,seat_class,year;

create view joinedRefunds as select * from refundDomestic35 union all select * from refundDomestic50 union all select * from refundIntl35 union all select * from refundIntl50;

create view allRefunds as 
select airline,name,year,seat_class, round(sum(refund),1) as refund
from joinedRefunds join airline on code=airline
group by airline,name,seat_class,year;



-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q2
select * from allRefunds;
