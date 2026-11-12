(ns phosphene.paradigma.glossematics
  "Hjelmslevian glossematics and semiotic dependency algebra.
   Formalizes the sign function (expression-form <-> content-form),
   the tripartite dependency functions (interdependence, determination, constellation),
   and the commutation test for invariant structural opposition."
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]))

;; --- The Quadripartite Sign Function ---
;; Sign Function: A reciprocal solidarity between Expression-Form and Content-Form.
;; Neither plane exists as language without the other.

(defn make-sign
  "Constructs a formal glossematic sign uniting expression-form and content-form.
   - expression: token or sequence of expression-figuræ (cenemes)
   - content: set or bundle of content-figuræ (pleremes / semantic features)
   - metadata: optional provenance / historical context map"
  ([expression content]
   (make-sign expression content {}))
  ([expression content metadata]
   {:sign/expression expression
    :sign/content (if (set? content) content (set content))
    :sign/metadata metadata}))

(defn sign-system
  "Builds a glossematic sign system (lexicon/glossary) from a collection of signs.
   Maintains two-way indexing: expression -> content and content -> expression."
  [signs]
  (let [sign-vec (vec signs)]
    {:system/signs sign-vec
     :system/exp->sign (into {} (map (juxt :sign/expression identity)) sign-vec)
     :system/content->signs (group-by :sign/content sign-vec)}))

(defn lookup-expression
  "Resolves the sign for a given expression-form."
  [sys exp]
  (get-in sys [:system/exp->sign exp]))

;; --- The Commutation Test (Invariant vs. Variant) ---
;; Hjelmslev's fundamental criterion:
;; Two entities in the expression plane commute (are in invariant opposition)
;; if substituting one for the other produces a correlated change in the content plane.
;; If substituting expression-a for expression-b produces NO difference in content,
;; they are merely variants (synonyms / allomorphs) of the same invariant.

(defn commute?
  "Returns true if the substitution of exp-a with exp-b yields a distinct content plane,
   confirming invariant opposition. Returns false if they share identical content (variants/synonyms)."
  [sys exp-a exp-b]
  (let [sign-a (lookup-expression sys exp-a)
        sign-b (lookup-expression sys exp-b)]
    (cond
      (or (nil? sign-a) (nil? sign-b))
      true ;; Unknown entities are assumed distinct / non-variant

      (= (:sign/content sign-a) (:sign/content sign-b))
      false ;; Invariant equivalence: substituting exp-a for exp-b induces NO content shift

      :else
      true ;; Commutation holds: different content bundles
      )))

;; --- Glossematic Dependencies (The Relational Calculus) ---
;; Hjelmslev defines language relations strictly through dependencies:
;; 1. Interdependence (Solidarity / Bilateral): x <-> y
;;    Both elements require each other within the specified scope/frame.
;; 2. Determination (Directional / Unilateral): x <- y
;;    Element y presupposes element x, but x does not presuppose y.
;; 3. Constellation (Mutual Independence / Compatibility): x | y
;;    Both elements can co-occur in the frame, but neither presupposes the other.

(defn dependency-type
  "Evaluates the empirical or formal dependency between term-a and term-b
   across a collection of contextual frames (sets of terms).
   Returns one of:
   - :interdependence  (a and b always co-occur: whenever one is present, both are present)
   - :determination-ab (b presupposes a: whenever b occurs, a is present, but not vice-versa)
   - :determination-ba (a presupposes b: whenever a occurs, b is present, but not vice-versa)
   - :constellation    (both occur independently and co-occur without strict presupposition)
   - :disjoint         (never co-occur in any observed frame)"
  [frames term-a term-b]
  (let [total-frames (count frames)
        has-a? (fn [f] (contains? (if (set? f) f (set f)) term-a))
        has-b? (fn [f] (contains? (if (set? f) f (set f)) term-b))
        frames-with-a (filter has-a? frames)
        frames-with-b (filter has-b? frames)
        frames-with-both (filter (fn [f] (and (has-a? f) (has-b? f))) frames)
        cnt-a (count frames-with-a)
        cnt-b (count frames-with-b)
        cnt-both (count frames-with-both)]
    (cond
      (zero? cnt-both)
      :disjoint

      (and (= cnt-a cnt-b cnt-both) (pos? cnt-both))
      :interdependence

      (and (= cnt-b cnt-both) (< cnt-both cnt-a))
      :determination-ab ;; b presupposes a (a <- b)

      (and (= cnt-a cnt-both) (< cnt-both cnt-b))
      :determination-ba ;; a presupposes b (b <- a)

      :else
      :constellation ;; compatible, co-occurring, neither unilaterally presupposing
      )))

(defn interdependence?
  "Returns true if term-a and term-b exhibit bilateral interdependence (solidarity) in frames."
  [frames term-a term-b]
  (= :interdependence (dependency-type frames term-a term-b)))

(defn determination?
  "Returns true if term-b presupposes term-a (determination: a <- b) in frames."
  [frames term-a term-b]
  (= :determination-ab (dependency-type frames term-a term-b)))

(defn constellation?
  "Returns true if term-a and term-b exhibit mutual compatibility without presupposition."
  [frames term-a term-b]
  (= :constellation (dependency-type frames term-a term-b)))

;; --- Glossematic Reduction (Plereme Decomposition) ---

(defn plerematic-overlap
  "Measures the content intersection between two signs as a Jaccard index
   over their plerematic content-feature bundles [0.0, 1.0]."
  [sign-a sign-b]
  (let [ca (:sign/content sign-a)
        cb (:sign/content sign-b)
        inter (set/intersection ca cb)
        uni (set/union ca cb)]
    (if (empty? uni)
      1.0
      (double (/ (count inter) (count uni))))))
