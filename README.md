# PetClinic Sample Application

This petclinic is an implementation of the [Spring Petcinic](https://github.com/spring-projects/spring-petclinic) using Clojure and the [Kit Framework](https://kit-clj.github.io/) with the default modules and trying to stick to its defaults.

I implemented this version to refresh my knowledge of web development in Clojure and the development experience using [Calva](https://calva.io), with an application that was not trivial but not too big either while trying to keep feature parity with the orignal PetClinic as close as possible.

## Highlights

* Written in Clojure with the Kit framework and standard modules (kit-html, kit-sql)
* Uses Selmer for HTML templates (instead of Thymeleaf on Spring Petclinic)
  * Custom functions for pagination and i10n/language support (uses the language translation files from Spring Petclinic)
* SCSS support via [sass4clj](https://github.com/Deraen/sass4clj/) (uses the same SCSS files from the Spring Petclinic with minimal changes)
* Web assets (Bootstrap/Font Awesome) served with [ring webjars](https://github.com/weavejester/ring-webjars)
* For simplicity, the only database engine is H2 (e.g. no PostgreSQL, etc.)
* Spring logo replaced with an awesome emoji-in-SVG logo.

## Requirements

* Java (version 17+ preferred)
* Clojure ([install guide](https://clojure.org/guides/install_clojure))

**Optional**

* Docker/Podman (to build a container with this app)
* GNU Make to run the commands from the `Makefile`: `make run`, `make repl`, `make test` or `make uberjar`


## Running the Petclinic locally

The natural way for Clojure development is to start a [REPL](#repls) for this project in your editor or terminal of choice.

Start the server with:

```clojure
(go)
```

The website will be available at http://localhost:3000/ and the default API is available under http://localhost:3000/api

System configuration is available under `resources/system.edn`.

To reload changes:

```clojure
(reset)
```

## Building

### Building an Uberjar

Run `clj -Sforce -T:build all` to compile everything into a JAR file which will be created at `target/petclinic-standalone.jar`.

The resulting JAR file can be run after setting the `JDBC_URL` and `PORT` environment variables:

```shell
export JDBC_URL=jdbc:h2:mem:prod
export PORT=8080
java -jar target/petclinic-standalone.jar
```

### Building and running the app in a container

Build the container with Docker/Podman:

```shell
docker build --pull --rm -f Dockerfile -t petclinic:latest .
```

To run the container, remember to provide the JDBC URL and the port, like this:

```shell
docker run -it --rm -p 3000:3000 -e PORT=3000 -e JDBC_URL=jdbc:h2:mem:prod petclinic:latest
```

## Database configuration

The `dev` and `test` environments use an in-memory H2 Database. For the Prod environment, the JDBC URL of the H2 database needs to be supplied in the `JDBC_URL` environment variable.

To support other database engines, the following changes are needed:

* Add the JDBC driver dependency in `deps.edn`.
* Edit the JDBC URLs entries under `:db.sql/connection` in the file `system.edn`.
* Update SQL DDL/inserts in the migration files using the SQL dialect of choice (these can be borrowed from the [Spring Petclinic files](https://github.com/spring-projects/spring-petclinic/tree/main/src/main/resources/db)).
* Update the queries in [resources/sql/queries.sql](resources/sql/queries.sql) with the SQL dialect of choice.

## Running the tests

Run `clj -M:test` from the terminal, or use the REPL/Editor integrations to run the tests for a given namespace.

## Compiling the CSS

You need to compile the SCSS files at least once to generate CSS files to see the correct styling of the web pages.

During development, you can run the following commands in the REPL (e.g. `clj -M:dev`):

```clojure
(require '[sass4clj.main :as sass])
(sass/-main "--source-paths" "./resources/scss" "-t" "./resources/public/css")
```

If you make changes to the SCSS files, you'll need to compile them manually again (there's no watcher installed for them yet).

The CSS files are automatically compiled when building the uberjar and container image.


## REPLs

### Visual Studio Code

Use the [Calva extension](https://calva.io/getting-started/), then follow the instructions on how to [Start a Project REPL and Connect](https://calva.io/connect/).

### Cursive

Configure a [REPL following the Cursive documentation](https://cursive-ide.com/userguide/repl.html). Using the default "Run with IntelliJ project classpath" option will let you select an alias from the ["Clojure deps" aliases selection](https://cursive-ide.com/userguide/deps.html#refreshing-deps-dependencies).

### CIDER

Use the `cider` alias for CIDER nREPL support (run `clj -M:dev:cider`). See the [CIDER docs](https://docs.cider.mx/cider/basics/up_and_running.html) for more help.

Note that this alias runs nREPL during development. To run nREPL in production (typically when the system starts), use the kit-nrepl library through the +nrepl profile as described in [the documentation](https://kit-clj.github.io/docs/profiles.html#profiles).

### Command Line

Run `clj -M:dev:nrepl` or `make repl`.

Note that, just like with [CIDER](#cider), this alias runs nREPL during development. To run nREPL in production (typically when the system starts), use the kit-nrepl library through the +nrepl profile as described in [the documentation](https://kit-clj.github.io/docs/profiles.html#profiles).

## License

This PetClinic application is released under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
