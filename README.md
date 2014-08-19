# prone

Better exception reporting middleware for Ring. Heavily inspired by
[better_errors for Rails](https://github.com/charliesome/better_errors).

## Usage

Add it as a middleware to your Ring stack:

```clj
(ns example
  (:require [prone.core :as prone]))

(def app
  (-> my-app
      prone/wrap-exceptions))
```

## Known problems

- We have not yet found a way to differentiate `some-name` and `some_name`
  function names by inspecting the stack trace. Currently, we assume kebab case.

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
