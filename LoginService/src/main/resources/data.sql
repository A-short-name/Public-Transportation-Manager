/* Script to initialize the roles database and manually create an admin and a customer+admin */

INSERT INTO roles(id,name) VALUES(1,'ADMIN'); /* avoid throwing exception is role already present */
INSERT INTO roles(id,name) VALUES(2,'SUPERADMIN'); /* avoid throwing exception is role already present */
INSERT INTO roles(id,name) VALUES(3,'CUSTOMER'); /* avoid throwing exception is role already present */
INSERT INTO roles(id,name) VALUES(4,'EMBEDDED'); /* avoid throwing exception is role already present */
INSERT INTO roles(id,name) VALUES(5,'SERVICE'); /* avoid throwing exception is role already present */
/*bcrypted password superadmin = $2a$12$5UL/sjysEtyD3m3i7/xHmupbWCfdgAHq5EYPIWOZnFvk4X8NWg/AW */
INSERT INTO users(id,active,email,nickname,password) VALUES(1,'true','superadmin@me.com','superadmin','superadmin');
/*bctripted password validator1 = $2a$12$s6oYucvWXPfeUmSjGMmnDudw.d1YunSucW90B7Bn5ABEOIa4EvwtW */
INSERT INTO users(id,active,email,nickname,password) VALUES(2,'true','val1@me.com','validator1','$2a$12$s6oYucvWXPfeUmSjGMmnDudw.d1YunSucW90B7Bn5ABEOIa4EvwtW');
INSERT INTO user_roles(user_id,role_id) VALUES(1,2);
INSERT INTO user_roles(user_id,role_id) VALUES(2,4);