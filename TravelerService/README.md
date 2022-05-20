# WA2-lab4
WA2-lab4


In order to try some api the testApp.http is a file with some http requests with valid tokens.


Also In integration tests there are more usecases


If you want generate new tokens there is the function generateToken in test files that it accepts the username, the set of roles and the expiration date.

Here 2 valid token with a late exp date:
Custemer R2D2: `eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJSMkQyIiwiaWF0IjoxNjUyNDU2MjQ5LCJleHAiOjE5MjQ5MDU2MDAsInJvbGVzIjpbIkNVU1RPTUVSIl19.Z_qium_7XDN3yIKzQMGj83YElSWfvR6h3TI0NiSj9sI`

Admin BigBoss: `eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJCaWdCb3NzIiwiaWF0IjoxNjUyNDU2MjQ5LCJleHAiOjE5MjQ5MDU2MDAsInJvbGVzIjpbIkFETUlOIl19.z8RClg7GgpT4e-OjihMJbflIWiDxzrdrYNhL2HteE_A`