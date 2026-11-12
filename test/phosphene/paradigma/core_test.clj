(ns phosphene.paradigma.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [phosphene.paradigma.core :as core]
            [phosphene.paradigma.spec :as pspec]))

(deftest vocabulary-construction-test
  (testing "Vocabulary creation and membership"
    (let [v (core/create-vocabulary :faraday-1832
                                    [:magnetic-curves :lines-of-force :electro-tonic-state]
                                    {:domain "electromagnetism"})]
      (is (= :faraday-1832 (:vocab/id v)))
      (is (core/member? v :magnetic-curves))
      (is (core/member? v :lines-of-force))
      (is (not (core/member? v :displacement-current)))

      (let [v2 (core/add-terms v [:induction])]
        (is (core/member? v2 :induction))))))

(deftest equivalence-ring-projection-test
  (testing "Synonym ring construction and sequence projection"
    (let [rings [#{:magnetic-curves :lines-of-force :magnetic-lines}
                 #{:toroid :annular-ring}]
          equiv-map (core/equivalence-map rings)
          tokens [:faraday :observed :magnetic-curves :around :the :annular-ring]
          projected (core/quotient-sequence equiv-map tokens)]
      (is (= (count tokens) (count projected)))
      ;; Both :magnetic-curves and :magnetic-lines map to the same canonical representative
      (is (= (core/quotient-term equiv-map :magnetic-curves)
             (core/quotient-term equiv-map :magnetic-lines)))
      (is (= (core/project-term equiv-map :magnetic-curves)
             (core/project-term equiv-map :magnetic-lines)))
      (is (= [:faraday :observed :lines-of-force :around :the :annular-ring] projected)))))

(deftest morphism-test
  (testing "Vocabulary morphism application"
    (let [v-hist (core/create-vocabulary :hist [:caloric :dephlogisticated-air])
          v-mod  (core/create-vocabulary :mod  [:heat :oxygen])
          morph  (core/vocabulary-morphism v-hist v-mod {:caloric :heat
                                                         :dephlogisticated-air :oxygen}
                                           :strict? true)
          tokens [:caloric :flows :with :dephlogisticated-air]
          mapped (core/apply-morphism morph tokens)]
      (is (= [:heat :flows :with :oxygen] mapped)))))

(deftest mwe-trie-chunking-test
  (testing "Multi-word expression greedy chunking"
    (let [mwe-map {[:lines :of :force] :term/lines-of-force
                   [:lines :of :magnetic :force] :term/lines-of-magnetic-force
                   [:electric :current] :term/electric-current}
          trie (core/mwe-trie mwe-map)
          tokens [:by :observing :lines :of :magnetic :force :and :electric :current :in :wire]
          chunked (core/chunk-sequence trie tokens)]
      (is (= [:by :observing :term/lines-of-magnetic-force :and :term/electric-current :in :wire]
             chunked)))))

(deftest generative-spec-check-test
  (testing "Generative property checking with test.check across dense term vocabularies"
    (let [res (stest/check [`core/create-vocabulary
                            `core/member?
                            `core/build-equivalence-map
                            `core/project-sequence
                            `core/chunk-mwe-sequence]
                           {:clojure.spec.test.check/opts {:num-tests 100}})]
      (doseq [r res]
        (let [sym (:sym r)
              check-ret (:clojure.spec.test.check/ret r)
              err (:error check-ret)]
          (is (nil? err) (str "Generative check failed for " sym ": " (pr-str check-ret)))
          (is (true? (:result check-ret)) (str "Check was not true for " sym)))))))
