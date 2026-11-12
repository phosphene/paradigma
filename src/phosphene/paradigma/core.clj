(ns phosphene.paradigma.core
  "Core paradigmatic operations over vocabularies, synonym rings,
   and multi-word terminological expressions (MWEs).
   Composes with phosphene.syntagma for structural abstraction over notation."
  (:require [clojure.set :as set]
            [clojure.string :as str]))

;; --- Term & Vocabulary Constructors ---

(defn create-vocabulary
  "Constructs a formal vocabulary record with an id, term set, and optional metadata."
  ([id terms]
   (create-vocabulary id terms {}))
  ([id terms metadata]
   {:vocab/id id
    :vocab/terms (set terms)
    :vocab/metadata metadata}))

(defn add-terms
  "Adds terms to an existing vocabulary."
  [vocab new-terms]
  (update vocab :vocab/terms into new-terms))

(defn member?
  "Checks if a term is present in the vocabulary."
  [vocab term]
  (contains? (:vocab/terms vocab) term))

;; --- Equivalence Rings (Quotient Spaces & Projections) ---

(defn build-equivalence-map
  "Given a collection of synonym sets (rings) #{t1 t2 ...}, builds a deterministic
   projection map {t -> canonical-representative}."
  [synonym-rings]
  (reduce
    (fn [acc ring]
      (let [canonical (first (sort ring))]
        (reduce (fn [m term] (assoc m term canonical)) acc ring)))
    {}
    synonym-rings))

(def ^{:doc "Pure transformation alias for build-equivalence-map per Tellman naming rules."}
  equivalence-map build-equivalence-map)

(defn project-term
  "Projects a token to its equivalence class canonical representative if present in equiv-map,
   otherwise returns the token unchanged."
  [equiv-map term]
  (get equiv-map term term))

(def ^{:doc "Alias for project-term."}
  quotient-term project-term)

(defn project-sequence
  "Projects each token in a sequence through an equivalence map: t -> [t]~."
  [equiv-map tokens]
  (mapv #(project-term equiv-map %) tokens))

(def ^{:doc "Linguistically precise quotient alias for project-sequence."}
  quotient-sequence project-sequence)

;; --- Vocabulary Morphisms (Order & Vocabulary Mapping) ---

(defn vocabulary-morphism
  "Creates a morphism mapping terms from source-vocab to target-vocab according to term-map.
   Verifies that mapped images are valid terms in target-vocab when strict? is true."
  [source-vocab target-vocab term-map & {:keys [strict?] :or {strict? false}}]
  (when strict?
    (doseq [[src dst] term-map]
      (when-not (member? source-vocab src)
        (throw (ex-info (str "Source term not in source vocabulary: " src) {:term src})))
      (when-not (member? target-vocab dst)
        (throw (ex-info (str "Target term not in target vocabulary: " dst) {:term dst})))))
  {:morphism/source (:vocab/id source-vocab)
   :morphism/target (:vocab/id target-vocab)
   :morphism/map term-map})

(defn apply-morphism
  "Applies a vocabulary morphism to a token sequence."
  [morphism tokens]
  (let [mapping (:morphism/map morphism)]
    (mapv #(get mapping % %) tokens)))

;; --- Multi-Word Expression (MWE) Trie Chunking ---

(defn build-mwe-trie
  "Builds a prefix trie from a map of multi-word token sequences to compound terms:
   {[t1 t2 ...] :compound/term}."
  [mwe-map]
  (reduce
    (fn [trie [token-seq compound-term]]
      (assoc-in trie (conj (vec token-seq) :mwe/value) compound-term))
    {}
    mwe-map))

(def ^{:doc "Pure transformation alias for build-mwe-trie."}
  mwe-trie build-mwe-trie)

(defn chunk-mwe-sequence
  "Greedily chunks multi-word expressions in a token sequence using a prefix trie.
   Non-matching tokens remain unchanged."
  [trie tokens]
  (let [n (count tokens)]
    (loop [i 0
           acc []]
      (if (>= i n)
        acc
        ;; Try to find the longest matching prefix starting at i
        (let [match (loop [j i
                           curr-node trie
                           longest-len 0
                           longest-val nil]
                      (if (>= j n)
                        (when longest-val {:len longest-len :val longest-val})
                        (let [tok (nth tokens j)
                              next-node (get curr-node tok)]
                          (if-not next-node
                            (when longest-val {:len longest-len :val longest-val})
                            (let [val (:mwe/value next-node)]
                              (recur (inc j)
                                     next-node
                                     (if val (inc (- j i)) longest-len)
                                     (if val val longest-val)))))))]
          (if match
            (recur (+ i (:len match)) (conj acc (:val match)))
            (recur (inc i) (conj acc (nth tokens i)))))))))

(def ^{:doc "Concise alias for chunk-mwe-sequence."}
  chunk-sequence chunk-mwe-sequence)
