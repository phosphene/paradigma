(ns phosphene.paradigma.connotation
  "Louis Hjelmslev & Roland Barthes' Staggered Semiotic Systems (Connotation).
   Formalizes:
     - Recursive semiotic escalation: S^(1) = (E_1 R_1 C_1) -> S^(2) = ((E_1 R_1 C_1) R_2 C_2)
     - collapse-sign: Collapses an evaluated paradigm square projection, glossematic sign,
       or narrative-program state into a canonical order-n sign record.
     - lift-square: Constructs an order-(n+1) semiotic square whose predicates operate
       over lifted order-n sign identifiers or content bundles via quotient equivalence.
     - connotative-projection: Projects an order-n sign or sign sequence onto an order-(n+1)
       semiotic square vertex or meta-term (:square/complex, :square/neutral),
       preserving full derivation provenance."
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [phosphene.paradigma.glossematics :as glos]
            [phosphene.paradigma.greimas :as greimas]))

;; --- Canonical Sign Collapse (S^(1) -> S^(n)) ---

(defn collapse-sign
  "Collapses an evaluated first-order structure (a Greimassian square projection,
   a glossematic sign, or a narrative program state map) into a canonical sign record.
   - struct: The incoming structure to quotient/collapse.
   - opts: Optional map with overrides (:order, :id, :metadata).
   Produces:
     {:sign/id keyword?
      :sign/order pos-int?
      :sign/expression any?
      :sign/content set?
      :sign/derived-from any?
      :sign/metadata map?}"
  ([struct]
   (collapse-sign struct {}))
  ([struct opts]
   (let [order (get opts :order (or (:sign/order struct) 1))
         ;; Determine expression and content based on struct shape
         [exp content derived-id]
         (cond
           ;; Already a sign
           (and (map? struct) (contains? struct :sign/expression) (contains? struct :sign/content))
           [(:sign/expression struct)
            (if (set? (:sign/content struct)) (:sign/content struct) (set (:sign/content struct)))
            (or (:sign/id struct) (keyword (str "sign-" (hash struct))))]

           ;; Greimassian square projection ({:projection/position ..., :projection/term ..., :projection/square ...})
           (and (map? struct) (contains? struct :projection/position) (contains? struct :projection/square))
           (let [sq-kw (:projection/square struct)
                 term-kw (or (:projection/term struct) (:projection/position struct))]
             [(:projection/square struct)
              (into #{} (filter some? [(:projection/position struct) (:projection/term struct)]))
              (if (namespace sq-kw)
                (keyword (namespace sq-kw) (str (name sq-kw) "-" (name term-kw)))
                (keyword (str (name sq-kw) "-" (name term-kw))))])

           ;; Narrative program state or execution ({:state/subject ..., :state/junction ..., :state/object ...} or {:status ..., :pn/id ...})
           (and (map? struct) (or (contains? struct :state/junction) (contains? struct :pn/id)))
           (let [pn-id (or (:pn/id struct) :pn/anonymous)
                 subj (get-in struct [:state/current :state/subject] (:state/subject struct))
                 junc (get-in struct [:state/current :state/junction] (:state/junction struct))
                 obj (get-in struct [:state/current :state/object] (:state/object struct))]
             [pn-id
              (into #{} (filter some? [subj junc obj]))
              (if (namespace pn-id)
                (keyword (namespace pn-id) (str (name pn-id) "-" (name (or junc :trans))))
                (keyword (str (name pn-id) "-" (name (or junc :trans)))))])

           ;; Generic map
           (map? struct)
           [(or (:id struct) (keyword (str "sign-" (hash struct))))
            (into #{} (keys struct))
            (or (:id struct) (keyword (str "sign-" (hash struct))))]

           ;; Token or keyword
           (keyword? struct)
           [struct #{struct} struct]

           :else
           [(keyword (str struct)) #{(keyword (str struct))} (keyword (str "sign-" (hash struct)))])

         sign-id (get opts :id derived-id)]
     {:sign/id sign-id
      :sign/order order
      :sign/expression exp
      :sign/content content
      :sign/derived-from struct
      :sign/metadata (merge (get struct :sign/metadata {}) opts)})))

(defn sign-order
  "Returns the semiotic order n of a sign record (default 1)."
  [sign]
  (get sign :sign/order 1))

(defn order-n-sign?
  "Returns true if x is a valid order-n sign record."
  [x]
  (and (map? x)
       (keyword? (:sign/id x))
       (integer? (:sign/order x))
       (pos? (:sign/order x))
       (contains? x :sign/expression)
       (set? (:sign/content x))))

;; --- Staggered Semiotic Square Construction (lift-square) ---

(defn lift-square
  "Constructs an order-(n+1) semiotic square designed to classify order-n signs.
   id: Sovereign keyword identifier for the higher-order square.
   s1: Positive higher-order ideological/epistemic pole.
   s2: Contrary higher-order pole.
   s1-matcher: Predicate or set of order-n sign-ids / content features satisfying s1.
   s2-matcher: Predicate or set of order-n sign-ids / content features satisfying s2.
   opts: Optional map with custom labels for contradictions and meta-terms."
  ([id s1 s2 s1-matcher s2-matcher]
   (lift-square id s1 s2 s1-matcher s2-matcher {}))
  ([id s1 s2 s1-matcher s2-matcher opts]
   (let [base-sq (greimas/semiotic-square id s1 s2 opts)
         wrap-matcher (fn [m]
                        (cond
                          (fn? m) m
                          (set? m) (fn [sign]
                                     (or (contains? m (:sign/id sign))
                                         (some #(contains? m %) (:sign/content sign))
                                         (contains? m (:sign/expression sign))))
                          (coll? m) (let [s (set m)]
                                      (fn [sign]
                                        (or (contains? s (:sign/id sign))
                                            (some #(contains? s %) (:sign/content sign))
                                            (contains? s (:sign/expression sign)))))
                          :else (fn [sign]
                                  (or (= m (:sign/id sign))
                                      (contains? (:sign/content sign) m)
                                      (= m (:sign/expression sign))))))]
     (assoc base-sq
            :square/order (get opts :order 2)
            :square/s1-matcher (wrap-matcher s1-matcher)
            :square/s2-matcher (wrap-matcher s2-matcher)))))

(defn lifted-square?
  "Returns true if x is a valid lifted order-(n+1) semiotic square."
  [x]
  (and (greimas/semiotic-square? x)
       (integer? (:square/order x))
       (fn? (:square/s1-matcher x))
       (fn? (:square/s2-matcher x))))

;; --- Connotative Projection ---

(defn- evaluate-sign-against-matchers
  [lifted-sq sign]
  (let [match-s1? ((:square/s1-matcher lifted-sq) sign)
        match-s2? ((:square/s2-matcher lifted-sq) sign)]
    {:has-s1 (boolean match-s1?)
     :has-s2 (boolean match-s2?)}))

(defn connotative-projection
  "Projects an order-n sign or collection of order-n signs onto an order-(n+1) semiotic square.
   Matches the sign against the lifted square's s1 and s2 matchers.
   Returns:
     {:projection/order (inc n)
      :projection/position #{:s1 :s2 :non-s1 :non-s2 :complex :neutral :unpositioned}
      :projection/term keyword?
      :projection/square keyword?
      :projection/derived-from vector-of-sign-ids}"
  [lifted-sq signs]
  (let [sign-coll (cond
                    (nil? signs) []
                    (map? signs) [signs]
                    (sequential? signs) (vec signs)
                    :else [(collapse-sign signs)])
        canonical-signs (mapv (fn [s] (if (order-n-sign? s) s (collapse-sign s))) sign-coll)
        input-order (if (empty? canonical-signs) 1 (apply max (map sign-order canonical-signs)))
        target-order (max (:square/order lifted-sq) (inc input-order))
        matches (mapv #(evaluate-sign-against-matchers lifted-sq %) canonical-signs)
        any-s1? (boolean (some :has-s1 matches))
        any-s2? (boolean (some :has-s2 matches))
        all-s1? (and (seq matches) (every? :has-s1 matches))
        all-s2? (and (seq matches) (every? :has-s2 matches))
        derived-ids (mapv :sign/id canonical-signs)]
    (cond
      ;; Complex term: both S_1 and S_2 connoted simultaneously
      (and any-s1? any-s2?)
      {:projection/order target-order
       :projection/position :complex
       :projection/term (:square/complex lifted-sq)
       :projection/square (:square/id lifted-sq)
       :projection/derived-from derived-ids}

      ;; S_1 pole
      any-s1?
      {:projection/order target-order
       :projection/position :s1
       :projection/term (:square/s1 lifted-sq)
       :projection/square (:square/id lifted-sq)
       :projection/derived-from derived-ids}

      ;; S_2 pole
      any-s2?
      {:projection/order target-order
       :projection/position :s2
       :projection/term (:square/s2 lifted-sq)
       :projection/square (:square/id lifted-sq)
       :projection/derived-from derived-ids}

      ;; Neither: Neutral term (Non-S_1 and Non-S_2) if signs were present, else unpositioned
      (seq canonical-signs)
      {:projection/order target-order
       :projection/position :neutral
       :projection/term (:square/neutral lifted-sq)
       :projection/square (:square/id lifted-sq)
       :projection/derived-from derived-ids}

      :else
      {:projection/order target-order
       :projection/position :unpositioned
       :projection/term nil
       :projection/square (:square/id lifted-sq)
       :projection/derived-from []})))
