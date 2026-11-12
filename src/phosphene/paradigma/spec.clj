(ns phosphene.paradigma.spec
  "Formal specification, property invariants, and dense generators for paradigma."
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [phosphene.paradigma.core :as core]
            [phosphene.paradigma.glossematics :as glo]))

;; --- 1. Dense Generators & Domain Alphabets ---

(def ^:dynamic *term-alphabet*
  "Finite controlled vocabulary of domain terms."
  [:magnetic :electric :flux :current :voltage :curve :field :force :anode :cathode])

(def ^:dynamic *feature-alphabet*
  "Finite controlled set of content-plane pleremes / semantic features."
  [:charge :motion :potential :vector :spatial :intensity :material :inductive])

(s/def :vocab/id (s/or :kw keyword? :str string?))
(s/def :vocab/term
  (s/with-gen keyword?
    #(gen/elements *term-alphabet*)))
(s/def :vocab/terms (s/coll-of :vocab/term :kind set?))
(s/def :vocab/metadata map?)

(s/def ::vocabulary
  (s/keys :req [:vocab/id :vocab/terms]
          :opt [:vocab/metadata]))

(s/def ::token-sequence
  (s/coll-of :vocab/term :kind vector?))

(s/def ::equivalence-map
  (s/map-of :vocab/term :vocab/term))

;; Glossematics Specs
(s/def :sign/expression :vocab/term)
(s/def :sign/content-feature
  (s/with-gen keyword?
    #(gen/elements *feature-alphabet*)))
(s/def :sign/content (s/coll-of :sign/content-feature :kind set?))
(s/def :sign/metadata map?)

(s/def ::sign
  (s/keys :req [:sign/expression :sign/content]
          :opt [:sign/metadata]))

(s/def :system/signs (s/coll-of ::sign :kind vector?))
(s/def :system/exp->sign (s/map-of :sign/expression ::sign))
(s/def :system/content->signs (s/map-of :sign/content (s/coll-of ::sign :kind vector?)))

(s/def ::sign-system
  (s/with-gen
    (s/keys :req [:system/signs :system/exp->sign :system/content->signs])
    #(gen/fmap glo/sign-system (s/gen (s/coll-of ::sign :kind vector? :min-count 1 :max-count 6)))))

;; --- 2. Function Specs: Core Paradigma ---

(s/fdef core/create-vocabulary
  :args (s/or :two (s/cat :id :vocab/id :terms (s/coll-of :vocab/term))
              :three (s/cat :id :vocab/id :terms (s/coll-of :vocab/term) :metadata :vocab/metadata))
  :ret ::vocabulary
  :fn (fn [{:keys [ret]}]
        (set? (:vocab/terms ret))))

(s/fdef core/member?
  :args (s/cat :vocab ::vocabulary :term any?)
  :ret boolean?
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [vocab term]} args]
          (= ret (contains? (:vocab/terms vocab) term)))))

(s/fdef core/build-equivalence-map
  :args (s/cat :synonym-rings (s/coll-of (s/coll-of :vocab/term :kind set?) :kind vector?))
  :ret ::equivalence-map
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [synonym-rings]} args
              terms-present (set (apply concat synonym-rings))]
          ;; Invariant: All mapped terms point to some canonical representative present in the rings
          (and (every? #(contains? terms-present %) (vals ret))
               (every? (fn [[term target]] (contains? terms-present term)) ret)))))

(s/fdef core/project-sequence
  :args (s/cat :equiv-map ::equivalence-map :tokens ::token-sequence)
  :ret ::token-sequence
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [tokens]} args]
          ;; Invariant: Length is strictly preserved under 1-to-1 projection
          (= (count tokens) (count ret)))))

(s/fdef core/chunk-mwe-sequence
  :args (s/cat :trie map? :tokens ::token-sequence)
  :ret ::token-sequence
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [tokens]} args]
          ;; Invariant: Chunking can only reduce or preserve sequence length, never expand it
          (<= (count ret) (count tokens)))))

;; --- 3. Function Specs: Glossematics & Semiometrics ---

(s/fdef glo/make-sign
  :args (s/or :two (s/cat :exp :sign/expression :content (s/coll-of :sign/content-feature))
              :three (s/cat :exp :sign/expression :content (s/coll-of :sign/content-feature) :meta map?))
  :ret ::sign
  :fn (fn [{:keys [ret]}]
        (and (contains? ret :sign/expression)
             (set? (:sign/content ret)))))

(s/fdef glo/sign-system
  :args (s/cat :signs (s/coll-of ::sign :kind vector?))
  :ret ::sign-system)

(s/fdef glo/commute?
  :args (s/cat :sys ::sign-system :exp-a :sign/expression :exp-b :sign/expression)
  :ret boolean?
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [sys exp-a exp-b]} args
              sign-a (glo/lookup-expression sys exp-a)
              sign-b (glo/lookup-expression sys exp-b)]
          ;; Invariant: Reflexive substitution with oneself never commutes (identical content)
          (if (and sign-a sign-b (= exp-a exp-b))
            (false? ret)
            true))))

(s/fdef glo/dependency-type
  :args (s/cat :frames (s/coll-of (s/coll-of :vocab/term :kind set?) :kind vector?)
               :term-a :vocab/term
               :term-b :vocab/term)
  :ret #{:interdependence :determination-ab :determination-ba :constellation :disjoint})

(s/fdef glo/plerematic-overlap
  :args (s/cat :sign-a ::sign :sign-b ::sign)
  :ret (s/and double? #(<= 0.0 % 1.0))
  :fn (fn [{:keys [args ret]}]
        (let [{:keys [sign-a sign-b]} args]
          ;; Invariant: Reflexive overlap is 1.0; symmetric overlap
          (and (if (= sign-a sign-b) (== 1.0 ret) true)
               (== ret (glo/plerematic-overlap sign-b sign-a))))))
