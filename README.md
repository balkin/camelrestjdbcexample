# camelrestjdbcexample
Example web application provides REST API that make available to execute CRUD operations with an "User" entities, that are persisted in the in-memory Derby database. 
REST API is configured in com.peterservice.example.camel.restjdbc.ExampleUsersRouteConfiguration class. 

U can to start that app by typing **mvn jetty:run** at your console (be sure that 28080 port is free).
After that U can to get some list of users by executing the GET method: http://localhost:28080/rs/users

App is similar to [camel-example-restlet-jdbc](http://mvnrepository.com/artifact/org.apache.camel/camel-example-restlet-jdbc) (and is based on its code)

## Integration testing

Integration tests should have postfix `IntegrationTest`. To run those, please use the

    mvn verify

## Pipeline

* Compile the app
* Run unit tests
* Package the app
* Run the resulting WAR using embedded jetty (28080 port should be available)
* Integration tests
