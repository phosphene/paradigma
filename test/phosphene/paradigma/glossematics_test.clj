(ns phosphene.paradigma.glossematics-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.test.alpha :as stest]
            [phosphene.paradigma.glossematics :as glo]
            [phosphene.paradigma.spec :as pspec]))

(deftest sign-and-commutation-test
  (testing "Sign function construction and commutation test"
    (let [sign-faraday (glo/make-sign :magnetic-curves
                                      #{:field :lines :magnetic})
          ;; Exact synonym variant: identical content bundle
          sign-lines   (glo/make-sign :lines-of-force
                                      #{:field :lines :magnetic})
          ;; Distinct concept: different content bundle
          sign-current (glo/make-sign :electric-current
                                      #{:flow :charge :conductor})
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
    (let [frames [#{:anode :cathode :magnetic :current :voltage}
                  #{:anode :cathode :magnetic :current}
                  #{:anode :cathode :magnetic :voltage}
                  #{:magnetic}]]

      ;; 1. Interdependence (Solidarity): anode <-> cathode (always co-occur)
      (is (true? (glo/interdependence? frames :anode :cathode)))
      (is (= :interdependence (glo/dependency-type frames :anode :cathode)))

      ;; 2. Determination (Unilateral): magnetic <- current
      (is (true? (glo/determination? frames :magnetic :current)))
      (is (= :determination-ab (glo/dependency-type frames :magnetic :current)))
      (is (false? (glo/determination? frames :current :magnetic)))

      ;; 3. Constellation (Compatibility): voltage | current
      (is (true? (glo/constellation? frames :voltage :current)))
      (is (= :constellation (glo/dependency-type frames :voltage :current)))

      ;; 4. Disjoint: completely absent co-occurrence
      (is (= :disjoint (glo/dependency-type frames :anode :phantom-entity))))))

(deftest generative-glossematics-check-test
  (testing "Generative property checks on glossematics functions across dense plereme alphabet"
    (let [res (stest/check [`glo/make-sign
                            `glo/sign-system
                            `glo/commute?
                            `glo/dependency-type
                            `glo/plerematic-overlap]
                           {:clojure.spec.test.check/opts {:num-tests 100}})]
      (doseq [r res]
        (let [sym (:sym r)
              check-ret (:clojure.spec.test.check/ret r)
              err (:error check-ret)]
          (is (nil? err) (str "Generative check failed for " sym ": " (pr-str check-ret)))
          (is (true? (:result check-ret)) (str "Check was not true for " sym)))))))
