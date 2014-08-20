/**
 * Cull.JS 0.3.0, 2014-08-20
 * Copyright (c) 2012
 * Magnar Sveen, Christian Johansen
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 * 
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
var cull = (function (global) {
    "use strict";

    var slice = Array.prototype.slice;
    var toString = Object.prototype.toString;

    function _assert(pred, msg) {
        if (!pred) { throw new TypeError(msg); }
    }

    function _refute(pred, msg) {
        _assert(!pred, msg);
    }

    /** Is `list` an object with a numeric length, but not a DOM element? */
    function isList(list) {
        return !!list &&
            typeof list === "object" &&
            typeof list.length === "number" &&
            !list.tagName;
    }

    /** Returns a version of `value` that is an actual Array. */
    function toList(value) {
        var ts = toString.call(value);
        if (ts === "[object Array]") { return value; }
        if (ts === "[object Arguments]") { return slice.call(value); }
        if (typeof value === "undefined" || value === null) { return []; }
        return slice.call(arguments);
    }

    /**
     * Calls `fn` on every item in `list`, presumably for side-effects.
     * Returns the list.
     */
    function doall(fn, list) {
        var i, l;
        for (i = 0, l = list.length; i < l; ++i) {
            fn(list[i], i, list);
        }
        return list;
    }

    /** Is `fn` a function? */
    function isFunction(fn) {
        return typeof fn === "function";
    }

    /**
     * Returns the result of applying `fn` to `initial` and the first
     * item in `list`, then applying `fn` to that result and the 2nd
     * item, etc.
     *
     * Can also be called without `initial`, in which case the first
     * invocation of `fn` will be with the first two items in `list`.
     */
    function reduce(fn, initial, items) {
        _assert(typeof fn === "function", "reducer should be a function");
        var i = 0, l, list = items, accumulator = initial;

        if (arguments.length === 2) {
            list = initial;
            accumulator = list[0];
            i = 1;
        }

        if (typeof list === "string") { list = list.split(""); }
        _assert(isList(list), "reduce needs to work on a list");

        for (l = list.length; i < l; ++i) {
            accumulator = fn(accumulator, list[i]);
        }

        return accumulator;
    }

    /** Is `pred` truthy for all items in `list`? */
    function all(pred, list) {
        var i, l;
        for (i = 0, l = list.length; i < l; ++i) {
            if (!pred(list[i])) { return false; }
        }
        return true;
    }

    /** Is `pred` truthy for any items in `list`? */
    function some(pred, list) {
        var i, l;
        for (i = 0, l = list.length; i < l; ++i) {
            if (pred(list[i])) { return true; }
        }
        return false;
    }

    /**
     * Is `pred` truthy for at least one item in `list`, and also falsy
     * for at least one item in `list`?
     */
    function onlySome(pred, list) {
        var i, l, t, f;
        for (i = 0, l = list.length; i < l; ++i) {
            if (pred(list[i])) {
                t = true;
            } else {
                f = true;
            }
            if (t && f) { return true; }
        }
        return false;
    }

    /** Returns `string` with white space at either end removed. */
    function trim(string) {
        return (string.trim && string.trim()) ||
            string.replace(/^\s+|\s+$/, "");
    }

    /** Returns `arg` unchanged. */
    function identity(arg) { return arg; }

    /** Is `o` neither undefined nor null? */
    function defined(o) { return typeof o !== "undefined" && o !== null; }

    /** Returns a version of `fn` that only accepts one argument. */
    function unary(fn) {
        return function (arg) {
            return fn.call(this, arg);
        };
    }

    /**
     * Returns a function that takes one argument and returns its
     * `name`-property.
     */
    function prop(name) {
        return function (object) {
            return object[name];
        };
    }

    /**
     * Returns a function that takes one argument and calls its
     * `name`-function with `args` (optional).
     */
    function func(name, args) {
        return function (object) {
            return object[name].apply(object, toList(args));
        };
    }

    /**
     * Returns a function that takes one argument and returns true if
     * it is equal to `x`.
     */
    function eq(x) {
        return function (y) { return x === y; };
    }

    var composeSignature = "compose takes func|[funcs] " +
            "and optional thisp object";

    /**
     * Returns a function that calls the last function in `fns`, then
     * calls the second to last function in `fns` with the result of
     * the first, and so on, with an optional this-binding in `thisp`.
     */
    function compose(fns, thisp) {
        _refute(isFunction(thisp) || arguments.length > 2, composeSignature);

        var _fns = toList(fns);

        _assert(all(isFunction, _fns), composeSignature);

        return function () {
            var i = _fns.length;
            var result = arguments;
            while (i--) {
                result = [_fns[i].apply(thisp || this, result)];
            }
            return result[0];
        };
    }

    /**
     * Takes any number of arguments, and returns a function that
     * takes one function and calls it with the arguments.
     */
    function callWith() {
        var args = arguments;
        return function (fn) {
            return fn.apply(this, args);
        };
    }

    /**
     * Takes a function `fn` and any number of additional arguments,
     * fewer than the normal arguments to `fn`, and returns a
     * function. When called, the returned function calls `fn` with
     * the given arguments first and then additional args.
     */
    function partial(fn) {
        var args = slice.call(arguments, 1);
        return function () {
            return fn.apply(this, args.concat(slice.call(arguments)));
        };
    }

    /**
     * Returns a function that calls `callee` with `obj` as this.
     * `callee` can be a function, or it can be a string - in which
     * case it will be used to look up a method on `obj`.
     *
     * Optionally takes additional arguments that are partially
     * applied.
     */
    function bind(obj, callee) {
        var fn = typeof callee === "string" ? obj[callee] : callee;
        var args = slice.call(arguments, 2);
        return function () {
            return fn.apply(obj, args.concat(slice.call(arguments)));
        };
    }

    /** Flatten `list` recursively and return a list of non-list values */
    function flatten(list) {
        _assert(isList(list), "flatten expects a list");
        var res = [], i, l;

        for (i = 0, l = list.length; i < l; i++) {
            res = res.concat(isList(list[i]) ? flatten(list[i]) : list[i]);
        }

        return res;
    }

    /** Return the first index of `needle` in `list`, otherwise < 0 */
    function indexOf(needle, list) {
        _assert(isList(list), "indexOf expects a needle and a list");
        var i, l;
        for (i = 0, l = list.length; i < l; ++i) {
            if (needle === list[i]) { return i; }
        }
        return -1;
    }

    /** Return a list with only the unique values in `list` */
    function uniq(list) {
        _assert(isList(list), "uniq expects a list");
        var result = [], i, l;

        for (i = 0, l = list.length; i < l; ++i) {
            if (indexOf(list[i], result) < 0) {
                result.push(list[i]);
            }
        }

        return result;
    }

    /** Return the first item in `list` for which `fn` returns `true` */
    function first(fn, list) {
        _assert(isFunction(fn) && isList(list),
                "first expects a function and a list");

        var i, l;
        for (i = 0, l = list.length; i < l; ++i) {
            if (fn(list[i])) {
                return list[i];
            }
        }
    }

    /** Return a new list containing the items from `list` for which
        `fn` is `true` */
    function select(fn, list) {
        _assert(isFunction(fn) && isList(list),
                "select expects a function and a list");
        var result = [], i, l;
        for (i = 0, l = list.length; i < l; ++i) {
            if (fn(list[i])) {
                result.push(list[i]);
            }
        }
        return result;
    }

    /** Return a list with the items present in `list` but not in `other` */
    function difference(list, other) {
        _assert(isList(list) && isList(other), "difference expects two lists");
        return select(function (value) {
            return indexOf(value, other) < 0;
        }, list);
    }

    /** Return a list with the items present in both `list1` and `list2` */
    function intersection(list1, list2) {
        _assert(isList(list1) && isList(list2),
                "intersection expects two lists");
        return select(function (value) {
            return indexOf(value, list2) >= 0;
        }, list1);
    }

    /** Return a list of enumerable own property keys in `object` */
    function keys(object) {
        var prop, result = [];
        for (prop in object) {
            if (object.hasOwnProperty(prop)) {
                result.push(prop);
            }
        }
        return result;
    }

    /** Return a list of enumerable own property values in `object` */
    function values(object) {
        var prop, result = [];
        for (prop in object) {
            if (object.hasOwnProperty(prop)) {
                result.push(object[prop]);
            }
        }
        return result;
    }

    /** Return a list of non-{null, undefined} items in `list` */
    var seldef = partial(select, defined);

    /**
     * Returns a new list consisting of the result of applying `fn` to
     * the items in `list`.
     */
    function map(fn, list) {
        var result = [], i, l;
        for (i = 0, l = list.length; i < l; i++) {
            result.push(fn(list[i]));
        }
        return result;
    }

    /**
     * Returns the complement of `pred`, ie a function that returns true
     * when `pred` would be falsy, and false when `pred` would be truthy.
     */
    function negate(pred) {
        return function () {
            return !pred.apply(this, arguments);
        };
    }

    /**
     * Returns a new list of the items in `list` for which `pred`
     * returns nil.
     */
    function reject(pred, list) {
        return select(negate(pred), list);
    }

    /**
     * Returns a new list with the concatenation of the elements in
     * `list1` and `list2`.
     */
    function concat(list1, list2) {
        return toList(list1).concat(toList(list2));
    }

    /**
     * Returns a new list with the items in `list` grouped into
     * `n-`sized sublists.
     *
     * The last group may contain less than `n` items.
     */
    function partition(n, list) {
        var result = [], i, l;
        for (i = 0, l = list.length; i < l; i += n) {
            result.push(list.slice(i, i + n));
        }
        return result;
    }

    /**
     * Returns a new list consisting of the result of applying `fn` to
     * the items in `list`, but filtering out all null or undefined
     * values from both `list` and the resulting list.
     */
    function mapdef(fn, list) {
        return seldef(map(fn, seldef(list)));
    }

    /**
     * Returns the result of applying concat to the result of applying
     * map to `fn` and `list`. Thus function `fn` should return a
     * collection.
     */
    function mapcat(fn, list) {
        return reduce(concat, [], map(fn, list));
    }

    /**
     * Returns an object with `keys` mapped to `vals`. Superflous keys
     * or vals are discarded.
     */
    function zipmap(keys, vals) {
        var result = {}, i, l = Math.min(keys.length, vals.length);
        for (i = 0; i < l; i++) {
            result[keys[i]] = vals[i];
        }
        return result;
    }

    /**
     * Returns a new list of all elements in `list` separated by
     * `sep`.
     */
    function interpose(sep, list) {
        var result = [], i, l;
        for (i = 0, l = list.length; i < l; i += 1) {
            result.push(list[i]);
            if (i < l - 1) {
                result.push(sep);
            }
        }
        return result;
    }

    // cull.advice

    /**
     * Advices the method `name` on `obj`, calling `fn` after the
     * method is called. `fn` is called with the return value of the
     * method as its first argument, then the methods original
     * arguments. If `fn` returns anything, it will override the
     * return value of the method.
     */
    function after(obj, name, fn) {
        var originalFn = obj[name];
        obj[name] = function () {
            var ret1 = originalFn.apply(this, arguments);
            var ret2 = fn.apply(this, [ret1].concat(slice.call(arguments)));
            return typeof ret2 !== "undefined" ? ret2 : ret1;
        };
    }

    /**
     * Advices the method `name` on `obj`, calling `fn` before the
     * method is called. `fn` is called with the same arguments as the
     * method.
     */
    function before(obj, name, fn) {
        var originalFn = obj[name];
        obj[name] = function () {
            fn.apply(this, arguments);
            return originalFn.apply(this, arguments);
        };
    }

    /**
     * Advices the method `name` on `obj`, calling `fn` instead of the
     * method. `fn` receives the original method as its first
     * argument, and then the methods original arguments. It is up to
     * the advicing function if and how the original method is called.
     */
    function around(obj, name, fn) {
        var f = partial(fn, obj[name]);
        obj[name] = function () {
            return f.apply(this, arguments);
        };
    }

    /** docs:function-list */
    return {
        _assert: _assert,
        _refute: _refute,
        trim: trim,
        doall: doall,
        reduce: reduce,
        all: all,
        some: some,
        onlySome: onlySome,
        isFunction: isFunction,
        isList: isList,
        toList: toList,
        identity: identity,
        defined: defined,
        unary: unary,
        prop: prop,
        func: func,
        eq: eq,
        compose: compose,
        callWith: callWith,
        partial: partial,
        bind: bind,
        keys: keys,
        values: values,
        concat: concat,
        flatten: flatten,
        uniq: uniq,
        first: first,
        select: select,
        negate: negate,
        reject: reject,
        seldef: seldef,
        map: map,
        mapdef: mapdef,
        mapcat: mapcat,
        zipmap: zipmap,
        partition: partition,
        difference: difference,
        intersection: intersection,
        interpose: interpose,
        indexOf: indexOf,
        after: after,
        before: before,
        around: around
    };
}(this));

if (typeof require === "function" && typeof module !== "undefined") {
    module.exports = cull;
}
