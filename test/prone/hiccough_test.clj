(ns prone.hiccough-test
  (:require [prone.hiccough :refer :all]
            [clojure.test :refer :all]))

(deftest render-maps-test
  (is (= (render-maps [:h1 {} "Hey"]) "<h1>Hey</h1>"))
  (is (= (render-maps [:h1 {:class "ok"} "Hey"]) "<h1 class=\"ok\">Hey</h1>"))
  (is (= (render-maps [:h1 {} [:span {} "Hey"]]) "<h1><span>Hey</span></h1>"))
  (is (= (render-maps [:h1 {} [:span {} [:span {} "Hey"]]]) "<h1><span><span>Hey</span></span></h1>"))
  (is (= (render-maps [:h1 {} [:span {}] [:span {} "Hey"]]) "<h1><span></span><span>Hey</span></h1>"))
  (is (= (render-maps [:h1 [:span "Easy"]]) "<h1><span>Easy</span></h1>"))
  (is (= (render-maps [:h1 nil [:span "Easy"]]) "<h1><span>Easy</span></h1>")))
