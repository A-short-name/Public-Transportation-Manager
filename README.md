# Public-Transportation-Manager
Web Applications II project

## How to open this project

In order to open this project you need:
- jdk (version 16) (we use Amazon corrett 16)
- wait the build of the maven project
- have docker installed

## Steps

If you want to run the project in a local environment you have to
- execute the docker-compose infrastructure using `docker-compose up -d`, in this way kafka and postgres containers will be up and running.
- create the db associated to that service
  - in some services you have to create the database and tables associated to the specific service. Check the resources folder of that service to find the .sql file
  
## Checks

### Tests

Try to execute tests inside each module

### API HTTP requests

Try to use the http requests that you can find 
