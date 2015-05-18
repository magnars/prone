# prone [![Build Status](https://secure.travis-ci.org/magnars/prone.png)](http://travis-ci.org/magnars/prone)

Better exception reporting middleware for Ring. Heavily inspired by
[better_errors for Rails](https://github.com/charliesome/better_errors).

See it to believe it:
[a quick video demoing Prone](https://dl.dropboxusercontent.com/u/3378230/prone-demo.mp4).

Prone presents your stack traces in a consumable form. It optionally filters out
stack frames that did not originate in your application, allowing you to focus
on your code. It allows you to browse environment data, such as the request map
and exception data (when using `ex-info`). Prone also provides a debug function
that enables you to visually browse local bindings and any piece of data you
pass to `debug`.

<img src="screenshot.png">

## Usage

Add `[prone "0.8.2"]` to `:dependencies` in your `project.clj`, then:

- **with lein-ring**

  Using `lein-ring` version `0.9.1` or above, add this to your `project.clj`:

  ```clj
  {:profiles {:dev {:ring {:stacktrace-middleware prone.middleware/wrap-exceptions}}}
  ```

- **without lein-ring**

  Add it as a middleware to your Ring stack:

  ```clj
  (ns example
    (:require [prone.middleware :as prone]))

  (def app
    (-> my-app
        prone/wrap-exceptions))
  ```

  Please note, with this configuration you should make sure to
  [only enable Prone in development](#should-i-use-prone-in-production).

## Debugging

Whether you've tripped on an exception or not, you can use Prone to debug your
application:

```clj
(ns example
  (:require [prone.debug :refer [debug]]))

(defn myhandler [req]
  ;; ...
  (let [person (lookup-person (:id (:params req)))]
    (debug)))
```

Calling `debug` without any arguments like this will cause Prone to render the
exception page with information about your environment: the request map, and any
local bindings (`req` and `person` in the above example).

You can call `debug` multiple times. To differentiate calls, you can pass a
message as the first argument, but Prone will also indicate the source location
that triggered debugging.

`debug` accepts any number of forms to present in a value browser on the
error/debug page:

```clj
(debug) ;; Inspect locals
        ;; Halts the page if there are no exceptions

(debug "Here be trouble") ;; Same as above, with a message

(debug {:id 42}) ;; Inspect locals and the specific map
                 ;; Halts the page if there are no exceptions

(debug person project) ;; Same as above, with multiple values

(debug "What's this?" person project) ;; Same as above, with message
```

## Q & A

### Should I use Prone in production?

No. You would be exposing your innards to customers, and maybe even to someone with
nefarious purposes.

Here's one way to avoid it:

```clj
(def prone-enabled? (= "true" (System.getProperty "prone.enable")))

(def app
  (cond-> my-app
          prone-enabled? prone/wrap-exceptions))
```

You can chain more optional middlewares in this `cond->` too. Pretty nifty.

### How does Prone determine what parts of a stack trace belongs to the application?

By default it reads your `project.clj` and looks for namespaces starting with
the project name.

You can change this behavior by passing in some options to `wrap-exceptions`,
like so:

```clj
(-> app
    (prone/wrap-exceptions 
      {:app-namespaces ["our" "app" "namespace" "prefixes"]}))
```

All frames from namespaces prefixed with the names in the list will be marked as
application frames.

### How do I skip prone for certain requests?

Pass a predicate function `skip-prone?` to `wrap-exceptions`. For example, to
exclude Postman requests check for `postman-token` in the headers:

```clj
(-> app
    (prone/wrap-exceptions 
      {:skip-prone? (fn [req] (contains? (:headers req) "postman-token"))}))
```

## Known problems

- We have not yet found a way to differentiate `some-name` and `some_name`
  function names by inspecting the stack trace. Currently, we assume kebab case.
- Using a middleware to always load the Austin `browser-connected-repl` for
  ClojureScript causes JavaScript errors that partly trips up Prone

## Contributors

- [Andrew Mcveigh](https://github.com/andrewmcveigh) added the `:app-namespaces` option.
- [Chris McDevitt](https://github.com/minimal) added the `:skip-prone?` option.
- [Malcolm Sparks](https://github.com/malcolmsparks) sorted map entries by keyword.
- [Ryo Fukumuro](https://github.com/rkworks) fixed several bugs.

Thanks!

## Contribute

Yes, please do. And add tests for your feature or fix, or we'll certainly break
it later.

#### Up and running

To start the server:

- run `lein build-auto` in one terminal to watch for css/js changes
- run `lein ring server-headless` in another.

Other commands:

* `lein test` will run all tests
* `lein test-refresh` will run tests continuously
* `lein minify-assets` will assets concatenate all vendor js files into `js/support.js`
and concatenate all css into `cs/prone.css`
* `lein build` is an alias for `lein do minify-assets, cljsbuild once`
* `lein build-auto` is an alias for `lein pdo minify-assets watch, cljsbuild auto` (pdo runs
in parallel)
* `lein release` will clean build everything from scratch, ready for packaging.

## License

Copyright © 2014 Christian Johansen & Magnar Sveen

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
