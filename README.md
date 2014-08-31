# prone

Better exception reporting middleware for Ring. Heavily inspired by
[better_errors for Rails](https://github.com/charliesome/better_errors).

See it to believe it:
[a quick video demoing Prone](https://dl.dropboxusercontent.com/u/3378230/prone-demo.mp4).

Prone presents your stack traces in a consumable form, optionally filters out
stack frames that did not originate in your application, allowing you to focus
on your code. It also allows you to browse environment data, such as the request
map and exception data (when using `ex-info`). Prone also provides a debug
function that enables you to visually browse local bindings and any piece of
data you pass to `debug`.

## Usage

Install via [Clojars](https://clojars.org/prone). Add it as a middleware to your
Ring stack:

```clj
(ns example
  (:require [prone.middleware :as prone]))

(def app
  (-> my-app
      prone/wrap-exceptions))
```

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

(debug {:id 42) ;; Inspect locals and the specific map
                ;; Halts the page if there are no exceptions

(debug person project) ;; Same as above, with multiple values

(debug "What's this?" person project) ;; Same as above, with message
;;;

## Known problems / planned features

- We have not yet found a way to differentiate `some-name` and `some_name`
  function names by inspecting the stack trace. Currently, we assume kebab case.
- Using a middleware to always load the Austin `browser-connected-repl` for
  ClojureScript causes JavaScript errors that partly trips up Prone
- Libraries like prismatic schema can benefit from some kind of plugin
  functionality, using domain knowledge of schemas to format error messages and
  exception data in a more helpful way

## Contribute

Yes, please do. And add tests for your feature or fix, or we'll certainly break
it later.

#### Up and running

To start the server:

- run `lein cljsbuild auto` in one terminal
- run `lein ring server-headless` in another.

`lein test` will run all tests.

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
