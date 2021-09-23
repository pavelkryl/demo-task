# Ataccama Homework

Start with building and running the application:

    mvn spring-boot:run

Then point your browser to [http://localhost:8080/swagger-ui](http://localhost:8080/swagger-ui)

There you will find documentation to the created REST API.

You may also find useful docker-compose in `docker` directory. Go into that directory and launch:

    docker-compose up

This will start a single MariaDB (formerly MySQL) instance along with UI on top of it, where you
can configure database/tables/columns. The Web UI lives on port 9090 [http://localhost:9090](http://localhost:9090) 
and you can login into the database instance with `root/example` combination of `username/password`.
