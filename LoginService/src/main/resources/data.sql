/* Script to initialize the roles database and manually create an admin and a customer+admin */

INSERT INTO roles(id,name) VALUES(1,'ADMIN'); /* avoid throwing exception is role already present */
INSERT INTO roles(id,name) VALUES(2,'CUSTOMER'); /* avoid throwing exception is role already present */