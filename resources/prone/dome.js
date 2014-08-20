/**
 * Dome 0.2.0, 2014-08-20
 * Copyright (c) 2013
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
var dome = (function (C) {
    if (!C && typeof require === "function") {
        C = require("culljs");
    }

    function _assert(pred, msg) {
        if (!pred) { throw new TypeError(msg); }
    }

    function _refute(pred, msg) {
        _assert(!pred, msg);
    }

    function children(elements) {
        if (C.isList(elements)) { return C.flatten(C.map(children, elements)); }
        var results = [], child = elements.firstChild;
        while (child) {
            if (child.nodeType === 1) { results.push(child); }
            child = child.nextSibling;
        }
        return results;
    }

    function id(idStr) {
        return document.getElementById(idStr);
    }

    function byClass(className, parent) {
        var ctx = parent || document;
        if (ctx.getElementsByClassName) {
            var elementsByClass = ctx.getElementsByClassName(className);

            // PhantomJS (at least v1.9.2) might return a function. Weird.
            if (typeof elementsByClass !== 'function') {
                return elementsByClass;
            }
        }
        var elements = ctx.getElementsByTagName("*"), i, l, result = [];
        var regexp = new RegExp("(^|\\s)" + className + "(\\s|$)");
        for (i = 0, l = elements.length; i < l; ++i) {
            if (regexp.test(elements[i].className)) {
                result.push(elements[i]);
            }
        }
        return result;
    }

    function remove(element) {
        element.parentNode.removeChild(element);
    }

    function replace(element, replacement) {
        if (!element.parentNode) { return; }
        element.parentNode.insertBefore(replacement, element);
        element.parentNode.removeChild(element);
    }

    function hasClassName(className, element) {
        var regexp = new RegExp("(^|\\s)" + className + "(\\s|$)");
        return regexp.test(element.className);
    }

    function addClassName(cn, element) {
        if (C.isList(element)) {
            return C.doall(C.partial(addClassName, cn), element);
        }
        if (hasClassName(cn, element)) { return; }
        element.className = C.trim(element.className + " " + cn);
    }

    function removeClassName(cn, element) {
        if (C.isList(element)) {
            return C.doall(C.partial(removeClassName, cn), element);
        }

        if (!hasClassName(cn, element)) { return; }

        var isCn = function (c) { return c === cn; };
        var classes = element.className.split(" ");
        element.className = C.reject(isCn, classes).join(" ");
    }

    // Implementation from jQuery/Sizzle. Simplified.
    function text(elm) {
        _assert(typeof elm !== "undefined" &&
                typeof elm.nodeType === "number",
                "text() expects DOM element");
        var nodeType = elm.nodeType;

        if (nodeType === 1 || nodeType === 9 || nodeType === 11) {
            // Use textContent for elements
            // innerText usage removed for consistency of new lines
            // (see jQuery #11153)
            if (typeof elm.textContent === "string") {
                return elm.textContent;
            }
            var ret = "";
            for (elm = elm.firstChild; elm; elm = elm.nextSibling) {
                ret += text(elm);
            }
            return ret;
        }
        if (nodeType === 3 || nodeType === 4) {
            return elm.nodeValue;
        }
        return "";
    }

    function frag(items) {
        var fragment = document.createDocumentFragment();
        C.doall(C.bind(fragment, "appendChild"), C.toList(items));
        return fragment;
    }

    var _uuid = 0;

    function uuid(object) {
        if (!object) { return null; }
        if (typeof object._dome_uuid !== "number") {
            object._dome_uuid = _uuid++;
        }
        return object._dome_uuid;
    }

    var containsCache = {};

    function contains(element, child) {
        if (!child || !element) { return false; }

        var elementId = uuid(element);
        if (!containsCache[elementId]) { containsCache[elementId] = {}; }
        if (containsCache[elementId][uuid(child)]) { return true; }

        var ids = [];
        while (child && child !== element) {
            ids.push(uuid(child));
            child = child.parentNode;
        }

        var result = !!child, i, l;

        for (i = 0, l = ids.length; i < l; i += 1) {
            containsCache[elementId][ids[i]] = result;
        }

        return result;
    }

    var el;

    var isContent = function (content) {
        return content !== null && typeof content !== "undefined" &&
            (typeof content.nodeType !== "undefined" ||
             typeof content === "string" ||
             C.isList(content));
    };

    function setData(data, element) {
        var name;
        data = data || {};

        for (name in data) {
            if (data.hasOwnProperty(name)) {
                element.setAttribute("data-" + name, data[name]);
                element["data-" + name] = data[name];
            }
        }
    }

    function getData(property, element) {
        return element.getAttribute("data-" + property);
    }

    var propmap = {
        style: function (element, styles) {
            var property;
            for (property in styles) {
                if (styles.hasOwnProperty(property)) {
                    element.style[property] = styles[property];
                }
            }
        },

        data: function (el, data) {
            setData(data, el);
        }
    };

    function setProp(properties, element) {
        var name, mapper;
        properties = properties || {};

        if (properties.hasOwnProperty('type')) {
            element.type = properties.type;
            delete properties.type;
        }

        for (name in properties) {
            if (properties.hasOwnProperty(name)) {
                mapper = propmap[name];
                if (mapper) {
                    mapper(element, properties[name]);
                } else {
                    element[name] = properties[name];
                }
            }
        }
    }

    function append(content, element) {
        _assert(isContent(content),
                "Content should be one or a list of [string, DOM element]");
        content = C.toList(content);
        var i, l;
        for (i = 0, l = content.length; i < l; ++i) {
            if (typeof content[i] === "string") {
                element.appendChild(document.createTextNode(content[i]));
            } else {
                element.appendChild(content[i]);
            }
        }
    }

    function setContent(children, element) {
        _assert(element && typeof element.innerHTML !== "undefined",
                "setContent() needs element");
        element.innerHTML = "";
        append(children, element);
    }

    el = function (tagName, attrProps, content) {
        _refute(arguments.length > 3,
                "Content should be one or a list of [string, DOM element]");
        if (!content && isContent(attrProps)) {
            return el(tagName, {}, attrProps);
        }
        _refute(attrProps && attrProps.tagName,
                "Cannot set attribute property tagName. Use a list when " +
                "adding multiple content elements.");
        var element = document.createElement(tagName);
        setProp(attrProps, element);
        append(content || [], element);
        return element;
    };

    el.toString = function () {
        return "dome.el()";
    };

    C.doall(function (tagName) { el[tagName] = C.partial(el, tagName); }, [
        "a", "br", "div", "fieldset", "form", "h2", "h3", "h4",
        "h5", "img", "input", "label", "li", "p", "span", "strong",
        "textarea", "ul", "span", "select", "option", "ol", "iframe",
        "table", "tr", "td", "pre", "button", "i"
    ]);

    /** docs:function-list */
    return {
        propmap: propmap,
        el: el,
        setProp: setProp,
        append: append,
        setContent: setContent,
        children: children,
        id: id,
        byClass: byClass,
        remove: remove,
        replace: replace,
        frag: frag,
        text: text,
        data: { get: getData, set: setData },
        cn: { has: hasClassName, add: addClassName, rm: removeClassName },
        uuid: uuid,
        contains: contains
    };
}(this.cull));

if (typeof require === "function" && typeof module === "object") {
    module.exports = dome;
}/*global cull, dome, window*/

// This is a modified version of code by Juriy Zaytsev originally published at
// http://msdn.microsoft.com/en-us/magazine/ff728624.aspx
(function (C, D) {
    function isHostMethod(object, method) {
        return (/^(?:function|object|unknown)$/).test(typeof object[method]);
    }

    var getUniqueId = (function () {
        if (typeof document.documentElement.uniqueID !== "undefined") {
            return function (element) {
                return element.uniqueID;
            };
        }
        var uid = 0;
        return function (element) {
            if (!element.__uniqueID) {
                element.__uniqueID = "uniqueID__" + uid;
                uid += 1;
            }
            return element.__uniqueID;
        };
    }());

    var elements = {}, on, off, d = document.documentElement;

    function createWrappedHandler(uid, handler) {
        return function (e) {
            handler.call(elements[uid], e || window.event);
        };
    }

    function createListener(uid, handler) {
        return {
            handler: handler,
            wrappedHandler: createWrappedHandler(uid, handler)
        };
    }

    if (isHostMethod(d, "addEventListener") &&
            isHostMethod(d, "removeEventListener") &&
            isHostMethod(window, "addEventListener") &&
            isHostMethod(window, "removeEventListener")) {
        on = function (element, eventName, handler) {
            element.addEventListener(eventName, handler, false);
            return {
                cancel: function () { off(element, eventName, handler); }
            };
        };

        off = function (element, eventName, handler) {
            element.removeEventListener(eventName, handler, false);
        };
    } else if (isHostMethod(d, "attachEvent") &&
                   isHostMethod(d, "detachEvent") &&
                   isHostMethod(window, "attachEvent") &&
                   isHostMethod("detachEvent")) {
        var listeners = {};

        on = function (element, eName, handler) {
            var uid = getUniqueId(element);
            elements[uid] = element;
            if (!listeners[uid]) { listeners[uid] = {}; }
            if (!listeners[uid][eName]) { listeners[uid][eName] = []; }
            var listener = createListener(uid, handler);
            listeners[uid][eName].push(listener);
            element.attachEvent("on" + eName, listener.wrappedHandler);

            return { cancel: function () { off(element, eName, handler); } };
        };

        off = function (element, eName, handler) {
            var uid = getUniqueId(element);
            if (!listeners[uid] || !listeners[uid][eName]) { return; }
            listeners[uid][eName] = C.select(function (listener) {
                if (listener.handler !== handler) { return true; }
                element.detachEvent("on" + eName, listener.wrappedHandler);
            }, listeners[uid][eName]);
        };
    }

    function delegate(delegator, element, event, handler) {
        on(element, event, function (e) {
            if (delegator(e.target, event, e)) {
                handler.call(e.target, e);
            }
        });
    }

    delegate.bycn = function (className, element, event, handler) {
        delegate(C.partial(D.cn.has, className), element, event, handler);
    };

    dome.events = {
        mouseenter: function (element, handler) {
            var current = null;

            var min = on(element, "mouseover", function (event) {
                if (current !== element) {
                    handler.call(element, event);
                    current = element;
                }
            });

            var mout = on(element, "mouseout", function (e) {
                var target = e.relatedTarget || e.toElement;

                try {
                    if (target && !target.nodeName) {
                        target = target.parentNode;
                    }
                } catch (err) {
                    return;
                }

                if (element !== target && !dome.contains(element, target)) {
                    current = null;
                }
            });

            return {
                cancel: function () {
                    min.cancel();
                    mout.cancel();
                }
            };
        },

        mouseleave: function (element, handler) {
            return on(element, "mouseout", function (event) {
                if (!dome.contains(element, event.relatedTarget) &&
                        element !== event.relatedTarget) {
                    handler.call(element, event);
                }
            });
        }
    };

    dome.on = function (element, event, handler) {
        if (dome.events[event]) {
            return dome.events[event](element, handler);
        }
        return on(element, event, handler);
    };

    dome.off = off;
    dome.delegate = delegate;

    dome.propmap.events = function (el, events) {
        C.doall(function (prop) {
            on(el, prop, events[prop]);
        }, C.keys(events));
    };
}(cull, dome));
