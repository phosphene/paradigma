(ns phosphene.paradigma.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [phosphene.paradigma.core :as core]
            [phosphene.paradigma.greimas :as greimas]
            [phosphene.paradigma.connotation :as conn]
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

(deftest semiotic-square-test
  (testing "Construction of Greimassian semiotic square"
    (let [sq (greimas/semiotic-square :square/life-death :life :death)]
      (is (true? (greimas/semiotic-square? sq)))
      (is (= :life (:square/s1 sq)))
      (is (= :death (:square/s2 sq)))
      (is (= :non-life (:square/non-s1 sq)))
      (is (= :non-death (:square/non-s2 sq)))

      ;; Relational axes
      (is (true? (greimas/contrary? sq :life :death)))
      (is (true? (greimas/contrary? sq :non-life :non-death)))
      (is (false? (greimas/contrary? sq :life :non-life)))

      (is (true? (greimas/contradictory? sq :life :non-life)))
      (is (true? (greimas/contradictory? sq :death :non-death)))
      (is (false? (greimas/contradictory? sq :life :death)))

      (is (true? (greimas/presupposes? sq :non-death :life)))
      (is (true? (greimas/presupposes? sq :non-life :death)))
      (is (= :positive (greimas/deixis-for sq :life)))
      (is (= :positive (greimas/deixis-for sq :non-death)))
      (is (= :negative (greimas/deixis-for sq :death)))
      (is (= :negative (greimas/deixis-for sq :non-life)))))

  (testing "Trajectories of signification (negation, assertion, narrative transition)"
    (let [sq (greimas/semiotic-square :square/nature-culture :nature :culture)]
      ;; Negation (diagonal operation)
      (is (= :non-nature (greimas/negation sq :nature)))
      (is (= :nature (greimas/negation sq :non-nature)))
      ;; Assertion (vertical implication)
      (is (= :culture (greimas/assertion sq :non-nature)))
      (is (= :nature (greimas/assertion sq :non-culture)))

      ;; Canonical narrative transition: S_1 -> ~S_1 -> S_2
      (let [trans (greimas/narrative-transition sq :nature)]
        (is (true? (:trajectory/valid? trans)))
        (is (= :nature (:trajectory/initial trans)))
        (is (= :non-nature (:trajectory/negation trans)))
        (is (= :culture (:trajectory/destination trans))))))

  (testing "Empirical feature projection onto the square"
    (let [sq (greimas/semiotic-square :square/veridiction :being :seeming
                                      {:non-s1 :non-being :non-s2 :non-seeming
                                       :complex :truth :neutral :falsity})]
      ;; Truth = Being + Seeming (Complex term)
      (is (= {:projection/position :complex :projection/term :truth :projection/square :square/veridiction}
             (greimas/project-onto-square sq [:being :seeming])))

      ;; Falsity = Non-being + Non-seeming (Neutral term)
      (is (= {:projection/position :neutral :projection/term :falsity :projection/square :square/veridiction}
             (greimas/project-onto-square sq [:non-being :non-seeming])))

      ;; Secret = Being + Non-seeming
      (is (= {:projection/position :s1 :projection/term :being :projection/square :square/veridiction}
             (greimas/project-onto-square sq [:being])))

      ;; Delusion = Seeming + Non-being
      (is (= {:projection/position :s2 :projection/term :seeming :projection/square :square/veridiction}
             (greimas/project-onto-square sq [:seeming]))))))

(deftest connotative-semiotics-test
  (testing "Canonical sign collapse (S^(1) -> order-1 sign)"
    (let [sq-proj {:projection/position :s1 :projection/term :being :projection/square :square/veridiction}
          s1 (conn/collapse-sign sq-proj)]
      (is (true? (conn/order-n-sign? s1)))
      (is (= 1 (conn/sign-order s1)))
      (is (= :square/veridiction-being (:sign/id s1)))
      (is (contains? (:sign/content s1) :being))
      (is (= sq-proj (:sign/derived-from s1))))

    (let [pn-state {:pn/id :pn/retrieval
                    :state/subject :actor/jason
                    :state/junction :conjunction
                    :state/object :item/fleece}
          s2 (conn/collapse-sign pn-state)]
      (is (true? (conn/order-n-sign? s2)))
      (is (= :pn/retrieval-conjunction (:sign/id s2)))))

  (testing "Order-(n+1) lifted semiotic square construction"
    (let [sq2 (conn/lift-square :square/epistemology
                                :field-continuity :action-at-a-distance
                                #{:pn/retrieval-conjunction :curved-lines}
                                #{:straight-rays :instantaneous-pull}
                                {:order 2})]
      (is (true? (conn/lifted-square? sq2)))
      (is (= 2 (:square/order sq2)))
      (is (= :field-continuity (:square/s1 sq2)))
      (is (= :action-at-a-distance (:square/s2 sq2)))))

  (testing "Connotative projection (order 1 -> order 2)"
    (let [sq2 (conn/lift-square :square/epistemology
                                :field-continuity :action-at-a-distance
                                #{:pn/retrieval-conjunction :curved-lines}
                                #{:straight-rays :instantaneous-pull}
                                {:order 2})
          s-cont (conn/collapse-sign :curved-lines {:id :curved-lines})
          s-dist (conn/collapse-sign :straight-rays {:id :straight-rays})]

      ;; Positive pole projection
      (let [p1 (conn/connotative-projection sq2 [s-cont])]
        (is (= 2 (:projection/order p1)))
        (is (= :s1 (:projection/position p1)))
        (is (= :field-continuity (:projection/term p1)))
        (is (= [:curved-lines] (:projection/derived-from p1))))

      ;; Contrary pole projection
      (let [p2 (conn/connotative-projection sq2 [s-dist])]
        (is (= 2 (:projection/order p2)))
        (is (= :s2 (:projection/position p2)))
        (is (= :action-at-a-distance (:projection/term p2)))
        (is (= [:straight-rays] (:projection/derived-from p2))))

      ;; Complex synthesis (both present simultaneously)
      (let [p3 (conn/connotative-projection sq2 [s-cont s-dist])]
        (is (= 2 (:projection/order p3)))
        (is (= :complex (:projection/position p3)))
        (is (= :field-continuity+action-at-a-distance (:projection/term p3))))

      ;; Neutral synthesis (unmatched order-1 signs)
      (let [s-neutral (conn/collapse-sign :other-matter {:id :other-matter})
            p4 (conn/connotative-projection sq2 [s-neutral])]
        (is (= 2 (:projection/order p4)))
        (is (= :neutral (:projection/position p4)))))))

(deftest generative-spec-check-test
  (testing "Generative property checking with test.check across dense term vocabularies"
    (let [res (stest/check [`core/create-vocabulary
                            `core/member?
                            `core/build-equivalence-map
                            `core/project-sequence
                            `core/chunk-mwe-sequence
                            `greimas/semiotic-square
                            `greimas/negation
                            `greimas/project-onto-square
                            `conn/collapse-sign
                            `conn/sign-order
                            `conn/lift-square
                            `conn/connotative-projection]
                           {:clojure.spec.test.check/opts {:num-tests 100}})]
      (doseq [r res]
        (let [sym (:sym r)
              check-ret (:clojure.spec.test.check/ret r)
              err (:error check-ret)]
          (is (nil? err) (str "Generative check failed for " sym ": " (pr-str check-ret)))
          (is (true? (:result check-ret)) (str "Check was not true for " sym)))))))
