(ns phosphene.paradigma.runner
  (:require [clojure.test :refer [run-tests]]
            [phosphene.paradigma.core-test]
            [phosphene.paradigma.glossematics-test]))

(defn -main [& _args]
  (println "Running paradigma test suite (unit + clojure.spec/test.check)...")
  (let [summary (run-tests 'phosphene.paradigma.core-test
                           'phosphene.paradigma.glossematics-test)]
    (if (and (zero? (:fail summary)) (zero? (:error summary)))
      (do
        (println "All tests and generative checks passed cleanly!")
        (System/exit 0))
      (do
        (println "Test failures encountered:" summary)
        (System/exit 1)))))
