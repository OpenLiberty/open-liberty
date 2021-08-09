SET SCHEMA jbatch;

-- job instance
SELECT jobinstanceid FROM jbatch.jobinstancedata WHERE id = '56888'

-- job execs
SELECT jobexecid FROM jbatch.executioninstancedata WHERE jobinstanceid = '56888'
-- returns 56889

-- job exec steps
SELECT * FROM jbatch.stepexecutioninstancedata WHERE jobexecid = '56889'

-- other queries

-- job instance with multiple executions
SELECT A.jobinstanceid, A.id AS jobexecid, A.exitstatus 
FROM jbatch.executioninstancedata AS A JOIN 
	(SELECT jobinstanceid, COUNT(*) total 
	FROM jbatch.executioninstancedata 
	GROUP BY jobinstanceid 
	HAVING COUNT(*) > 1) AS B
ON A.jobinstanceid = B.jobinstanceid
ORDER BY A.jobinstanceid, jobexecid


SELECT A.id, batchstatus 
FROM executioninstancedata AS A INNER JOIN jobinstancedata AS B
	ON A.jobinstanceid = B.id 
WHERE A.batchstatus = 'STARTED'
	AND B.name = 'job1'
	AND B.submitter = 'dummy'
	
select distinct id, name from jobinstancedata where name = 'job1'

-- experimenting with purge

select count(*) from JBATCH.JOBINSTANCEDATA
select count(*) from JBATCH.EXECUTIONINSTANCEDATA
select count(*) from JBATCH.STEPEXECUTIONINSTANCEDATA

DELETE FROM jbatch.jobinstancedata WHERE submitter = 'foo';

DELETE FROM JBATCH.EXECUTIONINSTANCEDATA
WHERE id IN (
	SELECT B.id FROM jbatch.jobinstancedata AS A INNER JOIN jbatch.executioninstancedata AS B
		ON A.id = B.jobinstanceid
	WHERE A.submitter = 'foo'
)
DELETE FROM JBATCH.STEPEXECUTIONINSTANCEDATA
WHERE id IN (
	SELECT C.id FROM jbatch.jobinstancedata AS A INNER JOIN jbatch.executioninstancedata AS B
		ON A.id = B.jobinstanceid INNER JOIN jbatch.stepexecutioninstancedata AS C
		ON B.id = C.jobexecid
	WHERE A.submitter = 'foo'
)

SELECT A.submitter FROM jobinstancedata A INNER JOIN executioninstancedata B ON A.jobinstanceid = B.jobinstanceid
WHERE B.jobexecid = 1

