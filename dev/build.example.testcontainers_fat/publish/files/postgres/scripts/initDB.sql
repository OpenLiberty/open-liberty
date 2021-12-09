DROP TABLE IF EXISTS testtable;

-- Create table
CREATE TABLE testtable (
    PersonID int,
    LastName varchar(255),
    FirstName varchar(255),
    City varchar(255) 
);

-- Insert test data
INSERT INTO testtable (PersonID, LastName, FirstName, City) VALUES (1, 'Doe', 'John', 'Rochester');
