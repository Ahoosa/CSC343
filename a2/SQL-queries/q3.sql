-- Q3. North and South Connections

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q3 CASCADE;

CREATE TABLE q3 (
    inbound VARCHAR(30),
    outbound VARCHAR(30),
    direct INT,
    one_con INT,
    two_con INT,
    earliest timestamp
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.
DROP VIEW IF EXISTS Canada CASCADE;
DROP VIEW IF EXISTS USA CASCADE;
DROP VIEW IF EXISTS CanUSA CASCADE;
DROP VIEW IF EXISTS direct_USA CASCADE;
DROP VIEW IF EXISTS direct_Canada CASCADE;
DROP VIEW IF EXISTS directFlights CASCADE;
DROP VIEW IF EXISTS airportCities CASCADE;
DROP VIEW IF EXISTS originCan CASCADE;
DROP VIEW IF EXISTS originUS CASCADE;
DROP VIEW IF EXISTS originCan2 CASCADE;
DROP VIEW IF EXISTS originUS2 CASCADE;
DROP VIEW IF EXISTS joinTables CASCADE;
DROP VIEW IF EXISTS oneCon CASCADE;
DROP VIEW IF EXISTS joinTables2 CASCADE;
DROP VIEW IF EXISTS twoCon CASCADE;
DROP VIEW IF EXISTS allpairs1 CASCADE;
DROP VIEW IF EXISTS allpairs2 CASCADE;
DROP VIEW IF EXISTS allpairs CASCADE;
DROP VIEW IF EXISTS allConnections CASCADE;

-- Define views for your intermediate steps here:
create view Canada as select city ,code from airport where country='Canada';
create view USA as select city,code from airport where country='USA';
create view CanUSA as select USA.city as USA,USA.code as USCode, Canada.city as Canada, Canada.code as CanCode from USA,Canada;

create view allpairs1 as select distinct usa as city1,canada as city2 from canusa;
create view allpairs2 as select distinct canada as city1, usa as city2 from canusa;
create view allpairs as select * from allpairs1 as a1 union all select * from allpairs2 as a2;

create view direct_USA as 
select Canada as origin,USA as dest,count(*) as direct, 0 as one_con,  0 as two_con, min(s_arv) as earliest
from flight join CanUSA on (outbound=CanCode and inbound=USCode)
where date(s_arv)='2021-04-30' and date(s_dep)='2021-04-30'
group by Canada,USA; 

create view direct_Canada as 
select USA as origin,Canada as dest,count(*) as direct, 0 as one_con,  0 as two_con, min(s_arv) as earliest
from flight join CanUSA on (outbound=USCode and inbound=CanCode) 
where date(s_arv)='2021-04-30' and  date(s_dep)='2021-04-30'
group by USA,Canada;

create view directFlights as select * from direct_Canada union all select * from direct_USA;

create view airportCities as 
select airline, outbound ,inbound ,a1.city as outboundCity, a2.city as inboundCity, s_dep, s_arv from flight join airport as a1 on outbound=a1.code join airport as a2 on a2.code=inbound;

-- Must be right
create view originUS as 
select canusa.USA as origin, CanUSA.Canada as dest, f1.inbound as stop,min(f2.s_arv) as earliest
from airportCities as f1 join canusa on  f1.outbound = Canusa.USCode and f1.inbound<>CanUSA.CanCode, airportCities as f2 
where canusa.CanCode=f2.inbound and f2.outbound=f1.inbound and (f2.s_dep-f1.s_arv) >= '0:30:00' and date(f2.s_arv)='2021-04-30' and date(f1.s_dep)='2021-04-30'
group by Canusa.Canada,canusa.USA,f1.inbound;

create view originCan as 
select canusa.Canada as origin, CanUSA.USA as dest,f1.inbound as stop, min(f2.s_arv) as earliest
from airportCities as f1 join canusa on  f1.outbound = Canusa.CanCode and f1.inbound<>CanUSA.USCode, airportCities as f2 
where canusa.USCode=f2.inbound and f2.outbound=f1.inbound and (f2.s_dep-f1.s_arv) >= '0:30:00' and date(f2.s_arv)='2021-04-30' and date(f1.s_dep)='2021-04-30'
group by Canusa.Canada,canusa.USA,f1.inbound;

create view joinTables as select * from originCan union all select * from originUS;
create view oneCon as select origin,dest, 0 as direct ,count(*) as one_con, 0 as two_con, min(earliest) as earliest from joinTables group by origin,dest;


-- 2 stops
create view originUS2 as 
select canusa.USA as origin, CanUSA.Canada as dest, f1.inbound as firstStop,f2.inbound as secondStop, min(f3.s_arv) as earliest
from airportCities as f1 join canusa on  f1.outbound = Canusa.USCode and f1.inbound<>CanUSA.CanCode, airportCities as f2,airportCities as f3
where canusa.CanCode=f3.inbound and f2.inbound=f3.outbound and f2.outbound=f1.inbound 
and (f2.s_dep-f1.s_arv) >= '0:30:00' and (f3.s_dep - f2.s_arv) >= '0:30:00' and date(f3.s_arv)='2021-04-30' and date(f2.s_arv)='2021-04-30' and date(f1.s_dep)='2021-04-30'
group by Canusa.Canada,canusa.USA,f1.inbound,f2.inbound;

create view originCan2 as 
select canusa.Canada as origin, CanUSA.USA as dest, f1.inbound as firstStop,f2.inbound as secondStop, min(f3.s_arv) as earliest
from airportCities as f1 join canusa on  f1.outbound = Canusa.CanCode and f1.inbound<>CanUSA.USCode, airportCities as f2,airportCities as f3
where canusa.USCode=f3.inbound and f2.inbound=f3.outbound and f2.outbound=f1.inbound 
and (f2.s_dep-f1.s_arv) >= '0:30:00' and (f3.s_dep - f2.s_arv) >= '0:30:00' and date(f3.s_arv)='2021-04-30' and date(f2.s_arv)='2021-04-30' and date(f1.s_dep)='2021-04-30'
group by Canusa.Canada,canusa.USA,f1.inbound,f2.inbound;

create view joinTables2 as select * from originCan2 union all select * from originUS2;
create view twoCon as select origin,dest,0 as direct, 0 as one_con, count(*) as two_con, min(earliest) as earliest from joinTables2 group by origin,dest;


create view allConnections as
select a1.city2 as inbound, a1.city1 as outbound, COALESCE(max(df.direct),0) as direct,COALESCE(max(onecon.one_con),0) as one_con, COALESCE(max(twocon.two_con),0) as two_con,least(least(min(df.earliest),min(onecon.earliest)),min(twocon.earliest)) as earliest 
from directflights as df full join onecon on df.origin=onecon.origin and
df.dest=onecon.dest full join twocon on onecon.origin=twocon.origin and onecon.dest=twocon.dest right join allpairs as a1 on
(df.origin=a1.city1 and df.dest=a1.city2) or (onecon.origin=a1.city1 and onecon.dest=a1.city2) or (twoCon.origin=a1.city1 and twoCon.dest=a1.city2)
group by a1.city1,a1.city2;


-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q3
select * from allConnections;

