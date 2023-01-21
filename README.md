# Public-Transportation-Manager

This is a Web Applications II group project carried out during academic year 2021/22. It consists of a microservices
system.

## Architecture

Developers singed in with github can find the architecture of the project
from [diagrams.net (or draw.io)](https://app.diagrams.net/)

![image](https://user-images.githubusercontent.com/62254235/213884657-103d949b-8a00-44a4-b5e2-97d90885f09b.png)

## Key management

![image](https://user-images.githubusercontent.com/62254235/213884680-c6d19430-53c4-45b5-9d60-7d8297bdd2c7.png)

## Flowchart of main user stories

### User actions

![User actions](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/A-short-name/Public-Transportation-Manager/main/User_actions.iuml?token=GHSAT0AAAAAAB2MGKP6MMACQHHGJRUOWQS6Y6MIZ3A)

### Admin actions

![Admin actions](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/A-short-name/Public-Transportation-Manager/main/Admin_actions.iuml?token=GHSAT0AAAAAAB2MGKP7LB7BL6LDARGU54HQY6MIZJQ)

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
