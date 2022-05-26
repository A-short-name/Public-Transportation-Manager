package it.polito.wa2.g15.lab5

import org.testcontainers.containers.PostgreSQLContainer

//https://stackoverflow.com/questions/59007414/testcontainers-postgresqlcontainer-with-kotlin-unit-test-not-enough-informatio
class MyPostgresSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgresSQLContainer>(imageName)