(ns prone.rendering-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [hiccup-find.core :refer [hiccup-string]]
            [prone.rendering :refer :all]))

(deftest build-stack-frame-clj-test
  (let [frame {:lang :clj
               :package "prone.rendering-test"
               :method-name "some-func"
               :file-name "prone/rendering_test.clj"
               :line-number 42}
        text (hiccup-string (build-stack-frame {:uri "/some/uri"} frame))]
    (is (= "prone.rendering-test/some-func prone/rendering_test.clj, line 42" text))))

(deftest build-stack-frame-java-test
  (let [frame {:lang :java
               :package "com.dom.dum"
               :class-name "Thingamajiggy"
               :method-name "doIt"
               :file-name "com/dom/dum/Thingamajiggy.java"
               :line-number 42}
        text (hiccup-string (build-stack-frame {:uri "/some/uri"} frame))]
    (is (= "com.dom.dum.Thingamajiggy$doIt com/dom/dum/Thingamajiggy.java, line 42" text))))

(deftest build-exception-test
  (let [error {:type "Exception"
               :message "Oh noes!"
               :frames [{:lang :java
                          :package "com.dom.dum"
                          :class-name "Thingamajiggy"
                          :method-name "doIt"
                          :file-name "com/dom/dum/Thingamajiggy.java"
                          :line-number 42}]}
        text (hiccup-string (build-exception {:uri "/some/uri"} error))]
    (is (re-find #"Exception at /some/uri" text))
    (is (re-find #"Oh noes!" text))))
