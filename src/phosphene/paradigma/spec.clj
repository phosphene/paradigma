(ns phosphene.paradigma.spec
  "Formal specification and generative property testing for paradigma."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [phosphene.paradigma.core :as core]))

;; --- Domain Specs ---

(s/def :vocab/id (s/or :kw keyword? :str string?))
(s/def :vocab/term keyword?)
(s/def :vocab/terms (s/coll-of :vocab/term :kind set?))
(s/def :vocab/metadata map?)

(s/def ::vocabulary
  (s/keys :req [:vocab/id :vocab/terms]
          :opt [:vocab/metadata]))

(s/def ::token-sequence
  (s/coll-of keyword? :kind vector?))

(s/def ::equivalence-map
  (s/map-of keyword? keyword?))

;; --- Function Specs ---

(s/fdef core/create-vocabulary
  :args (s/or :two (s/cat :id :vocab/id :terms (s/coll-of :vocab/term))
              :three (s/cat :id :vocab/id :terms (s/coll-of :vocab/term) :metadata :vocab/metadata))
  :ret ::vocabulary)

(s/fdef core/project-sequence
  :args (s/cat :equiv-map ::equivalence-map :tokens ::token-sequence)
  :ret ::token-sequence
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [equiv-map tokens]} args]
          ;; Invariant: Length is strictly preserved under 1-to-1 projection
          (= (count tokens) (count ret)))))

(s/fdef core/chunk-mwe-sequence
  :args (s/cat :trie map? :tokens ::token-sequence)
  :ret ::token-sequence
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [tokens]} args]
          ;; Invariant: Chunking can only reduce or preserve sequence length, never expand it
          (<= (count ret) (count tokens)))))
