# paradigma

> *paradigmatic substitution over notation.*  
> *vocabularies, synonym rings, and multi-word terminological expressions.*

`phosphene/paradigma` implements the vertical (paradigmatic) axis of structural language analysis in Clojure. It serves as the vocabulary and glossematic complement to `phosphene/syntagma` (the horizontal order-relation engine).

[![CI](https://github.com/phosphene/paradigma/actions/workflows/ci.yml/badge.svg)](https://github.com/phosphene/paradigma/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## The Saussurean & Hjelmslevian Duality

Structural linguistic analysis operates across two orthogonal axes:

```
           PARADIGMATIC (Vertical Substitution / paradigma)
           Equivalent terms, synonym rings, plerematic features
                                 │
                                 ▼
                     [lines-of-force] ──> [magnetic-flux]
                                 │
───────────────┼───────────────────────────────► SYNTAGMATIC (Horizontal Chaining / syntagma)
               │                                 Order relations, meets/joins, posets
               ▼
[current] ──> [induces] ──> [lines-of-force]
```

1. **Syntagmatic (`syntagma`):** How notation tokens chain horizontally ($\sigma_1 \sqsubseteq \sigma_2$, $t_1 \prec t_2 \prec t_3$).
2. **Paradigmatic (`paradigma`):** Which tokens may substitute vertically ($t \mapsto [t]_\sim$, expression-plane $\leftrightarrow$ content-plane).

---

## Capabilities

1. **Formal Vocabularies (`phosphene.paradigma.core`):**
   - Immutable vocabularies with domain metadata and set membership semantics.
2. **Equivalence Rings & Quotient Projection:**
   - Maps tokens through synonym rings ($t \mapsto [t]_\sim$) for seamless composition with `phosphene.syntagma.core/equiv-subsequence?`.
3. **Vocabulary Morphisms:**
   - Order-preserving maps between historical and modern scientific terminology without hermeneutic inflation.
4. **Multi-Word Expression (MWE) Trie Chunking:**
   - Deterministic prefix-trie chunking of compound scientific terms (*"lines of magnetic force"*) into singular compound notation tokens before sequence verification.
5. **Hjelmslev Glossematics & Commutation Calculus (`phosphene.paradigma.glossematics`):**
   - Quadripartite sign function pairing Expression-Form with Content-Form.
   - Commutation test (`commute?`): validates invariant structural opposition vs. allomorphic/synonymic variance.
   - Tripartite dependency calculus (`interdependence`, `determination`, `constellation`).

---

## Quickstart (REPL)

```clojure
(require '[phosphene.paradigma.core :as para]
         '[phosphene.paradigma.glossematics :as glos])

;; 1. Equivalence Rings & Quotient Projection
(def rings [#{:flux :lines-of-force :magnetic-lines}
            #{:induced-current :voltaic-current :galvanic-current}])

(def equiv-map (para/build-equivalence-map rings))
;; => {:flux :flux, :lines-of-force :flux, :magnetic-lines :flux, ...}

(para/project-sequence equiv-map [:detect :magnetic-lines :and :galvanic-current])
;; => [:detect :flux :and :galvanic-current]

;; 2. Multi-Word Expression (MWE) Prefix-Trie Chunking
(def mwe-trie
  (para/build-mwe-trie
    {[:lines :of :magnetic :force] :lines-of-force
     [:electric :current]          :electric-current}))

(para/chunk-mwe-sequence mwe-trie [:the :lines :of :magnetic :force :induce :electric :current])
;; => [:the :lines-of-force :induce :electric-current]

;; 3. Hjelmslev Glossematics & The Commutation Test
(def sys
  (glos/sign-system
    [(glos/make-sign :flux #{:magnetic :field :vector})
     (glos/make-sign :lines-of-force #{:magnetic :field :vector})  ;; identical content
     (glos/make-sign :current #{:charge :motion :amperage})]))     ;; distinct content

;; Substituting :flux with :lines-of-force produces NO content change -> variants / synonyms
(glos/commute? sys :flux :lines-of-force)
;; => false

;; Substituting :flux with :current alters the content plane -> invariant opposition
(glos/commute? sys :flux :current)
;; => true

;; 4. Dependency Calculus across Contextual Frames
(def frames
  [#{:voltage :current :resistance}
   #{:voltage :current}
   #{:voltage}])

(glos/dependency-type frames :voltage :current)
;; => :determination-ab  (:current presupposes :voltage, but :voltage occurs independently)
```

---

## Generative Specification Invariants

Formal generative properties are enforced with `clojure.spec.alpha` and `clojure.test.check`:

- **Length Preservation:** Quotient projection ($t \mapsto [t]_\sim$) strictly preserves sequence length ($|S'| = |S|$).
- **Monotonic Trie Contraction:** Multi-word chunking monotonically contracts or preserves token count ($|S_{\text{chunk}}| \le |S|$), never expanding it.
- **Commutation Identity:** Substituting an expression with itself never commutes ($\text{commute?}(S, x, x) = \text{false}$).

---

## Testing

Hermetic, offline execution with zero network or database dependencies:

```bash
clojure -M:runner
```

---

## License

MIT License. Copyright (c) 2026 Ed Phillips / Phosphene.
