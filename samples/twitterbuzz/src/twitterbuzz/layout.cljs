;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns twitterbuzz.layout
  (:require [twitterbuzz.anneal :as ann]
            [goog.math :as math]))

(defn random-loc []
  {:x (ann/random) :y (ann/random)})

(defn sqr [x]
  (* x x))

(defn sqrt [x]
  (js* "Math.sqrt(~{x})"))

(defn dist [{x1 :x y1 :y} {x2 :x y2 :y}]
  (sqrt (+ (sqr (- x2 x1)) (sqr (- y2 y1)))))

(defn init-state [mentions-data]
  {:locs (zipmap (keys mentions-data) (repeatedly #(random-loc)))
   :mentions mentions-data})

(defn score [{:keys [locs mentions]}]
  (let [metric (fn [d w] (sqr (- 1 (* d w))))
        score-user (fn [[k {:keys [mentions]}]]
                     (if (zero? (count mentions))
                       0
                       (let [loc (locs k)]
                         (reduce (fn [score [ok w]]
                                   (+ score (metric (dist loc (locs ok)) w)))
                                 0
                                 mentions))))]
    (reduce + (map score-user mentions))))

(defn permute-swap [{:keys [locs mentions]} t]
  ;;first cut - swap
  (let [xys (vec (vals locs))
        swap1 (math/randomInt (count xys))
        swap2 (math/randomInt (count xys))
        temp (xys swap1)
        xys (assoc xys swap1 (xys swap2))
        xys (assoc xys swap2 temp)]
    {:locs (zipmap (keys locs) xys)
     :mentions mentions}))

(defn permute-move [{:keys [locs mentions]} t]
  (let [adj #(min 1.0 (max 0 (+ % (- (* (ann/random) 0.2) 0.1))))
        move (fn [{:keys [x y]}]
               {:x (adj x) :y (adj y)})
        xys (vec (vals locs))]
    {:locs (zipmap (keys locs) (map move (vals locs)))
     :mentions mentions}))

(comment
;;(def test-data paste data from file here)

(def init (init-state test-data))

(map (fn [x] {:best-score (:best-score x) :t (:t x)})
     (take 10 (take-nth 100
                        (ann/anneal score
                                    (ann/linear-cooling 1000)
                                    permute-move
                                    ann/standard-prob
                                    init))))
  )