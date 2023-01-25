# Public-Transportation-Manager

This is a Web Applications II group project carried out during academic year 2021/22. It consists of a microservices
system.

## Presentation

https://prezi.com/view/SyfRTVuknWk29oNQD7GF/

## Architecture

Developers singed in with github can find the architecture of the project
from [diagrams.net (or draw.io)](https://app.diagrams.net/)

![image](./doc/WA2_Public_travelservice-System_architecture.png)

## Key management

![image](./doc/WA2_Public_travelservice-Key_management.png)

## Flowchart of main user stories

### User actions

![image](https://user-images.githubusercontent.com/62254235/213910702-1c882558-7684-47a2-9d76-87433151f4c0.png)

### Admin actions

![image](https://user-images.githubusercontent.com/62254235/213910665-37169195-a188-4072-87db-d20828c736ac.png)

## How to open this project

In order to open this project you need:

- Docker
- JDK (version 16) - Prefer Amazon Corretto 16

## Steps

To run the project in a local environment it is needed to:

- Execute the docker-compose.yml file in the main directory (using `docker-compose up -d`). In this way all the needed
  containers (kafka, postgres) will be up and running.
- Postgres databases will be created using the env variable `POSTGRES_MULTIPLE_DATABASES` that is used by the
  script `pg-init-scripts` for the initialization and creation of the databases

## Checks

### Tests

Try to execute tests inside each module

### API HTTP requests

Try to use the http requests that you can find in each module
