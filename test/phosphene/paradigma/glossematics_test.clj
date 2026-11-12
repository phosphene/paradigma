(ns phosphene.paradigma.glossematics-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [phosphene.paradigma.glossematics :as glo]))

;; --- Specs for Glossematics ---

(s/def :sign/expression keyword?)
(s/def :sign/content (s/coll-of keyword? :kind set?))
(s/def ::sign (s/keys :req [:sign/expression :sign/content]))

(s/fdef glo/make-sign
  :args (s/cat :exp :sign/expression :content (s/coll-of keyword?) :meta (s/? map?))
  :ret ::sign)

(s/fdef glo/plerematic-overlap
  :args (s/cat :a ::sign :b ::sign)
  :ret (s/and double? #(<= 0.0 % 1.0)))

(deftest sign-and-commutation-test
  (testing "Sign function construction and commutation test"
    (let [sign-faraday (glo/make-sign :magnetic-curves
                                      #{:plereme/field :plereme/lines :plereme/magnetic})
          ;; Exact synonym variant: identical content bundle
          sign-lines   (glo/make-sign :lines-of-force
                                      #{:plereme/field :plereme/lines :plereme/magnetic})
          ;; Distinct concept: different content bundle
          sign-current (glo/make-sign :electric-current
                                      #{:plereme/flow :plereme/charge :plereme/conductor})
          sys (glo/sign-system [sign-faraday sign-lines sign-current])]

      ;; Commutation between variants is false (they share the same content plane)
      (is (false? (glo/commute? sys :magnetic-curves :lines-of-force)))

      ;; Commutation between distinct invariants is true (substitution shifts content)
      (is (true? (glo/commute? sys :magnetic-curves :electric-current)))

      ;; Plerematic overlap
      (is (= 1.0 (glo/plerematic-overlap sign-faraday sign-lines)))
      (is (= 0.0 (glo/plerematic-overlap sign-faraday sign-current))))))

(deftest hjelmslev-dependencies-test
  (testing "The three formal glossematic dependency functions"
    (let [;; Frames representing context occurrences across a corpus
          ;; Frame 1: anode and cathode always appear together (solidarity)
          ;; Induction current only appears if magnetic field is present (determination)
          ;; Needle observation is compatible with magnetic field (constellation)
          frames [#{:anode :cathode :magnetic-field :induction-current :needle}
                  #{:anode :cathode :magnetic-field :induction-current}
                  #{:anode :cathode :magnetic-field :needle}
                  #{:magnetic-field}]]

      ;; 1. Interdependence (Solidarity): anode <-> cathode (always co-occur)
      (is (true? (glo/interdependence? frames :anode :cathode)))
      (is (= :interdependence (glo/dependency-type frames :anode :cathode)))

      ;; 2. Determination (Unilateral): magnetic-field <- induction-current
      ;; Induction-current NEVER appears without magnetic-field, but magnetic-field can appear alone
      (is (true? (glo/determination? frames :magnetic-field :induction-current)))
      (is (= :determination-ab (glo/dependency-type frames :magnetic-field :induction-current)))
      (is (false? (glo/determination? frames :induction-current :magnetic-field)))

      ;; 3. Constellation (Compatibility): needle | induction-current
      ;; Both can co-occur, but each also occurs without the other
      (is (true? (glo/constellation? frames :needle :induction-current)))
      (is (= :constellation (glo/dependency-type frames :needle :induction-current)))

      ;; 4. Disjoint: completely absent co-occurrence
      (is (= :disjoint (glo/dependency-type frames :anode :phantom-entity))))))

(deftest generative-glossematics-check-test
  (testing "Generative property checks on glossematics functions"
    (let [res (stest/check [`glo/make-sign
                            `glo/plerematic-overlap]
                           {:clojure.spec.test.check/opts {:num-tests 50}})]
      (doseq [r res]
        (let [sym (:sym r)
              check-ret (:clojure.spec.test.check/ret r)
              err (:error check-ret)]
          (is (nil? err) (str "Generative check failed for " sym ": " (pr-str check-ret)))
          (is (true? (:result check-ret)) (str "Check was not true for " sym)))))))
