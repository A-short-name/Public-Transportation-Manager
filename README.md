# Public-Transportation-Manager
This is a Web Applications II group project carried out during academic year 2021/22. It consists of a microservices system.

## Architecture

Developers singed in with github can find the architecture of the project from [diagrams.net (or draw.io)](https://app.diagrams.net/)

![image](https://user-images.githubusercontent.com/62254235/213884657-103d949b-8a00-44a4-b5e2-97d90885f09b.png)

## Key management

![image](https://user-images.githubusercontent.com/62254235/213884680-c6d19430-53c4-45b5-9d60-7d8297bdd2c7.png)

## Flowchart of main user stories

### User actions

```plantuml
@startuml

actor User
participant LoginService
participant TicketCatalogService
participant PaymentService
participant TravelerService
participant ValidatorService

autonumber
User -> LoginService : /user/register
User -> LoginService : /user/validate
User -> LoginService : /user/login
User -> TicketCatalogService : /tickets
User -> TicketCatalogService : /shop/{ticket-id}/
TicketCatalogService --> TravelerService : /services/user/{username}/birthdate/
TicketCatalogService -[#add1b2]> PaymentService : Place payment order on Kafka
PaymentService -[#add1b2]> TicketCatalogService : Read outcome of payment from Kafka
TicketCatalogService -> TravelerService : /services/user/{username}/tickets/add/
User -> TicketCatalogService: /orders
User -> TravelerService: /my/tickets/{ticket-sub}
User -> ValidatorService: /{clientZid}/validate

@enduml
```

### Admin actions

```plantuml
@startuml
actor Admin
actor SuperAdmin

participant LoginService

autonumber
SuperAdmin -> LoginService : /admin/create
Admin -> LoginService : /user/login
Admin -> ValidatorService : /get/stats
Admin -> PaymentService : /admin/transations
Admin -> TravelerService : /admin/traveler/{userID}/profile/
Admin -> TravelerService : /admin/traveler/{userID}/tickets/
Admin -> TravelerService : /stats/
Admin -> TicketCatalogService : /admin/orders/{user-id}/
Admin -> TicketCatalogService : /admin/orders/
Admin -> TicketCatalogService : /admin/tickets/

@enduml
```

## How to open this project

In order to open this project you need:
- Docker 
- JDK (version 16) - Prefer Amazon Corretto 16

## Steps

To run the project in a local environment it is needed to:
- Execute the docker-compose.yml file in the main directory (using `docker-compose up -d`). In this way all the needed containers (kafka, postgres) will be up and running.
- Postgres databases will be created using the env variable `POSTGRES_MULTIPLE_DATABASES` that is used by the script `pg-init-scripts` for the initialization and creation of the databases
  
## Checks

### Tests

Try to execute tests inside each module

### API HTTP requests

Try to use the http requests that you can find in each module
