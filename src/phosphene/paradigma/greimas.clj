(ns phosphene.paradigma.greimas
  "Algirdas Julien Greimas's Elementary Structure of Signification (The Semiotic Square).
   Formalizes:
     - The Semiotic Square (Carré sémiotique) over sovereign semantic poles S_1 and S_2
     - The 4 vertices: S_1, S_2, ~S_1 (non-S_1), ~S_2 (non-S_2)
     - The 3 relational axes:
         * Contrariety (horizontal axis of mutual exclusion: S_1 <-> S_2)
         * Contradiction (diagonal axis of strict negation: S_1 <-> ~S_1, S_2 <-> ~S_2)
         * Complementarity / Deixis (vertical axis of presupposition: ~S_2 -> S_1, ~S_1 -> S_2)
     - Complex term (S_1 + S_2) and Neutral term (~S_1 + ~S_2)
     - Projection of empirical feature bundles or signs onto the square."
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]))

;; --- The Semiotic Square Record & Constructor ---

(defn semiotic-square
  "Constructs a Greimassian Semiotic Square from an elementary contrary pair (s1 and s2).
   id: Sovereign keyword identifier (e.g. :square/life-death or :square/nature-culture).
   s1: Positive term (e.g. :life, :nature, :preservation).
   s2: Contrary term (e.g. :death, :culture, :entropy).
   opts: Optional map with custom labels for contradictions (~s1, ~s2)
         and meta-terms (complex, neutral). Default negation prefix is :non-."
  ([id s1 s2]
   (semiotic-square id s1 s2 {}))
  ([id s1 s2 opts]
   (let [neg-prefix (get opts :negation-prefix "non-")
         non-s1 (get opts :non-s1 (keyword (namespace s1) (str neg-prefix (name s1))))
         non-s2 (get opts :non-s2 (keyword (namespace s2) (str neg-prefix (name s2))))
         complex-t (get opts :complex (keyword (str (name s1) "+" (name s2))))
         neutral-t (get opts :neutral (keyword (str (name non-s1) "+" (name non-s2))))]
     {:square/id id
      :square/s1 s1
      :square/s2 s2
      :square/non-s1 non-s1
      :square/non-s2 non-s2
      ;; Axes & Relations
      :square/contraries #{#{s1 s2} #{non-s1 non-s2}}
      :square/contradictions #{#{s1 non-s1} #{s2 non-s2}}
      :square/deixis {:positive [non-s2 s1]   ;; non-s2 implies s1
                      :negative [non-s1 s2]}  ;; non-s1 implies s2
      ;; Meta-terms (syntheses)
      :square/complex complex-t
      :square/neutral neutral-t
      :square/vertices #{s1 s2 non-s1 non-s2}
      :square/metadata opts})))

(defn semiotic-square?
  "Returns true if x is a valid Greimassian semiotic square record."
  [x]
  (and (map? x)
       (keyword? (:square/id x))
       (keyword? (:square/s1 x))
       (keyword? (:square/s2 x))
       (keyword? (:square/non-s1 x))
       (keyword? (:square/non-s2 x))
       (set? (:square/vertices x))
       (= 4 (count (:square/vertices x)))))

;; --- Logical Relational Queries over the Square ---

(defn contrary?
  "Returns true if term-a and term-b stand in a relation of contrariety
   (horizontal axis: s1 <-> s2, or sub-contrariety ~s1 <-> ~s2)."
  [sq term-a term-b]
  (contains? (:square/contraries sq) #{term-a term-b}))

(defn contradictory?
  "Returns true if term-a and term-b stand in a relation of contradiction
   (diagonal axis of strict negation: s1 <-> ~s1, or s2 <-> ~s2)."
  [sq term-a term-b]
  (contains? (:square/contradictions sq) #{term-a term-b}))

(defn presupposes?
  "Returns true if antecedent presupposes/implies consequent along the vertical deixis
   (~s2 -> s1 along positive deixis, or ~s1 -> s2 along negative deixis)."
  [sq antecedent consequent]
  (or (= [antecedent consequent] (get-in sq [:square/deixis :positive]))
      (= [antecedent consequent] (get-in sq [:square/deixis :negative]))))

(defn deixis-for
  "Returns which deixis (:positive or :negative) a term belongs to, or nil if none."
  [sq term]
  (cond
    (some #{term} (get-in sq [:square/deixis :positive])) :positive
    (some #{term} (get-in sq [:square/deixis :negative])) :negative
    :else nil))

;; --- Trajectories of Signification (Veridictory & Narrative Operations) ---

(defn negation
  "Returns the contradictory term (diagonal operation) on the square:
   S_1 -> ~S_1, S_2 -> ~S_2, ~S_1 -> S_1, ~S_2 -> S_2."
  [sq term]
  (cond
    (= term (:square/s1 sq))     (:square/non-s1 sq)
    (= term (:square/s2 sq))     (:square/non-s2 sq)
    (= term (:square/non-s1 sq)) (:square/s1 sq)
    (= term (:square/non-s2 sq)) (:square/s2 sq)
    :else nil))

(defn assertion
  "Returns the asserted term along the deixis (vertical implication):
   ~S_2 -> S_1, ~S_1 -> S_2."
  [sq term]
  (cond
    (= term (:square/non-s2 sq)) (:square/s1 sq)
    (= term (:square/non-s1 sq)) (:square/s2 sq)
    :else nil))

(defn narrative-transition
  "Computes Greimas's canonical narrative transformation trajectory:
   A state starting at S_1 moves by negation to ~S_1, then by assertion to S_2:
     S_1 -(negate)-> ~S_1 -(assert)-> S_2."
  [sq term]
  (let [neg (negation sq term)]
    (when neg
      (let [target (assertion sq neg)]
        {:trajectory/initial term
         :trajectory/negation neg
         :trajectory/destination target
         :trajectory/valid? (some? target)}))))

;; --- Empirical Feature Projection onto the Square ---

(defn project-onto-square
  "Evaluates an empirical feature set or predicate map against the square.
   Tests presence of s1, s2, non-s1, and non-s2 features.
   Returns:
     - :vertex (if single vertex satisfied)
     - :complex (if both s1 and s2 satisfied)
     - :neutral (if both non-s1 and non-s2 satisfied)
     - :unpositioned (if none or unmapped)."
  [sq features]
  (let [feat-set (if (map? features) (set (keys features)) (set features))
        has-s1 (contains? feat-set (:square/s1 sq))
        has-s2 (contains? feat-set (:square/s2 sq))
        has-non-s1 (contains? feat-set (:square/non-s1 sq))
        has-non-s2 (contains? feat-set (:square/non-s2 sq))]
    (cond
      (and has-s1 has-s2)
      {:projection/position :complex
       :projection/term (:square/complex sq)
       :projection/square (:square/id sq)}

      (and has-non-s1 has-non-s2)
      {:projection/position :neutral
       :projection/term (:square/neutral sq)
       :projection/square (:square/id sq)}

      has-s1
      {:projection/position :s1
       :projection/term (:square/s1 sq)
       :projection/square (:square/id sq)}

      has-s2
      {:projection/position :s2
       :projection/term (:square/s2 sq)
       :projection/square (:square/id sq)}

      has-non-s1
      {:projection/position :non-s1
       :projection/term (:square/non-s1 sq)
       :projection/square (:square/id sq)}

      has-non-s2
      {:projection/position :non-s2
       :projection/term (:square/non-s2 sq)
       :projection/square (:square/id sq)}

      :else
      {:projection/position :unpositioned
       :projection/term nil
       :projection/square (:square/id sq)})))
