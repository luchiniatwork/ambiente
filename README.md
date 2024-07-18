# Ambiente

[![Clojars][clojars-badge]][clojars]
[![License][license-badge]][license]
![Status][status-badge]

`Ambiente` is a Clojure library for managing environment settings from
a number of different sources. It works well for applications
following the [12 Factor App](http://12factor.net/) pattern.

Currently, `Ambiente` supports four sources, resolved and merged down
in the following order:

1. A `.env.edn` file in the project directory
2. A `.env` file on the project directory
3. Environment variables
4. Java system properties

The `.env.edn` file is expected to be an `edn` file containing a map
where keys are the sanitized, keywordized names of the variables you
intend to overwrite.

The `.env` file is expected to be a file in the traditional `dotenv`
format.


## Installation

Include the following dependency in your `deps.edn`:

```clojure
{:deps {luchiniatwork/ambiente {:mvn/version "0.1.5"}}}
```


## Usage

Let's say you have an application that requires a database
connection. Let's pull the database connection details from the key
`:database-url` on the `ambiente.core/env` map.

```clojure
(require '[ambiente.core :refer [env]])

(def database-url
  (env :database-url))
```

The value of this key can be set in several different ways. The most
common way during development is to use either a local `.env.edn` or
`.env` file in your project directory. These files would be in your
`.gitignore` to avoid them getting dangerously exposed (i.e. instead
of `:database-url`, we could have an `:api-key`.)

The `.env.edn` file contains a map that will be merged with
environment variables and system properties:

```clojure
{:database-url "jdbc:postgresql://localhost/dev"}
```

Alternatively, the `.env` file could have been used. It contains a
list key/value pairs separated by newlines:

``` text
$ DATABASE_URL=jdbc:postgresql://localhost/dev
```

This means that if you run your repl locally (i.e. `$ clojure`), the
dev database will be used, and if you run `$
DATABASE_URL=jdbc:postgresql://localhost/test clojure`, the test
database will be used.

When you deploy to a production environment, you can make use of
environment variables, like so:

```bash
$ DATABASE_URL=jdbc:postgresql://localhost/prod java -jar standalone.jar
```

Or use Java system properties:

```bash
$ java -Ddatabase.url=jdbc:postgresql://localhost/prod -jar standalone.jar
```

Note that `Ambiente` automatically lower-cases keys, replaces the
characters "_" and "." with "-", and keywordize variable names. The
environment variable `DATABASE_URL` and the system property
`database.url` are therefore both converted to the same keyword
`:database-url`.

## Development

Tests can be run with:

``` bash
$ clojure -A:test --watch
```

## Motivation and Inspiration

`Ambiente` is heavily and openly inspired by [environ][] and
[dotenv][].

I've used [environ][] for ages and it's a great project but, the more
I move to tools.deps, the less I like seeing file named after Leiningen
in my project directory.

Sometimes `.env` files are shared around developers of specific teams
(particularly in non-Clojure or multi-language projects). For years
I've wanted [environ][] to support these files.

Lastly, [dotenv][]'s reliance on Leiningen's profiles also frustrated
me because it's an approach that might lead to developers wrongly
storing data in the `project.clj` file. `Ambiente` of course does not
fully remove this possibility (developers may still commit their
`.env` files) but it at least assumes that `deps.edn`, environment
variables and profiles are different things to be managed.

As a bonus, I also wanted to be more explicit on the warnings
emitted - particularly when variables are overwritten.

## License

Distributed under the MIT Public License. Use it as you
will. Contribute if you have the time.

<!-- links -->

[environ]: https://github.com/weavejester/environ
[dotenv]: https://github.com/LynxEyes/dotenv.clj

[license-badge]: https://img.shields.io/badge/license-MIT-blue.svg
[license]: #license

[clojars-badge]: https://img.shields.io/clojars/v/luchiniatwork/ambiente.svg
[clojars]: http://clojars.org/luchiniatwork/ambiente

[status-badge]: https://img.shields.io/badge/project%20status-prod-brightgreen.svg
