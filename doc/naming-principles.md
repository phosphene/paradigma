---
uri: phosphene/paradigma/naming-principles
owner: edphos
status: living
updated: 2026-11-12
title: Paradigma Naming Principles and Linguistic Ontology
description: "Codification of naming standards, Fregean indirection, and scope-boundary rules in phosphene.paradigma per Tellman, Saussure, and Hjelmslev."
---

# Paradigma: Naming Principles and Linguistic Ontology

> *"In language there are only differences without positive terms."*  
> — Ferdinand de Saussure, *Course in General Linguistics*
>
> *"A name’s sense should be timeless. While the referent (implementation) may change tomorrow, the sense remains constant."*  
> — Zachary Tellman, *Elements of Clojure*

`phosphene.paradigma` models the vertical axis of language and formal notation: **paradigmatic substitution** ($t \mapsto [t]_\sim$), equivalence rings, vocabulary quotienting, terminological morphisms, and Hjelmslevian glossematics.

This document records the naming principles, Fregean semiotic distinctions, and scope boundaries applied across `phosphene.paradigma`.

---

## 1. Domain Ontology & Synthetic Semantics

In structural linguistics and formal semiotics (Saussure, Hjelmslev, Jakobson), the vertical associative axis describes sets of elements that can commute within the same syntagmatic slot without violating grammatical or ontological validity.

`phosphene.paradigma` relies on explicit **synthetic names** grounded in linguistic theory:

| Linguistic / Semiometric Concept | Sign (`phosphene.paradigma`) | Formal Sense (Invariants) |
| :--- | :--- | :--- |
| **Paradigmatic Axis** | `paradigm` / `vocabulary` | Closed or bounded set of terms sharing substitutability within a context. |
| **Synonym Ring / Quotient Class** | `quotient-ring` / `equivalence-map` | Equivalence relation $\sim$ partition mapping terms to canonical representatives: $t \mapsto [t]_\sim$. |
| **Equivalence Projection** | `project-sequence` / `quotient-sequence` | 1-to-1 homomorphism mapping tokens to canonical quotient classes while strictly preserving sequence length. |
| **Multi-Word Expression (MWE)** | `mwe-trie` / `chunk-sequence` | Prefix tree representing syntagmatic compound terms greedily compressed into single atomic terms. |
| **Bilateral Sign** | `sign` | Hjelmslevian quadripartite sign connecting Expression-Form (`:sign/expression`) to Content-Form (`:sign/content`). |
| **Commutation Test** | `commute?` | Exchange of expression forms that correlates with a shift in the content plane (invariants vs. variants). |
| **Plerematic Overlap** | `plerematic-overlap` | Jaccard intersection of semantic content features (pleremes) between two signs: $\frac{\|C_a \cap C_b\|}{\|C_a \cup C_b\|}$. |
| **Interdependence (Solidarity)** | `:interdependence` | Bilateral presupposition ($A \leftrightarrow B$): neither occurs without the other. |
| **Determination (Selection)** | `:determination-ab` / `:determination-ba` | Unilateral presupposition ($A \leftarrow B$): $B$ presupposes $A$, but $A$ can occur alone. |
| **Constellation (Combination)** | `:constellation` | Mutual compatibility without presupposition ($A \mid B$): both co-occur, but neither necessitates the other. |

Because users of `phosphene.paradigma` understand structural linguistics, these synthetic terms provide exact mathematical narrowness without natural language ambiguity.

---

## 2. Fregean Indirection: Sign, Sense, and Referent

Every definition in `paradigma` adheres to the Fregean separation:

1. **Sign:** The textual identifier (e.g. `quotient-sequence`, `commute?`, `plerematic-overlap`).
2. **Sense:** Specified formally in `phosphene.paradigma.spec` via `s/def` and `s/fdef`:
   * `project-sequence`: Length is invariant: $|S'| = |S|$.
   * `chunk-sequence`: Monotonically non-expanding: $|S_{\text{chunk}}| \le |S|$.
   * `plerematic-overlap`: Bounded $[0.0, 1.0]$, symmetric, and reflexively equal to $1.0$.
3. **Referent:** The persistent hash-map lookup, prefix-tree traversal, or set intersection. The implementation mechanism never leaks into the sign.

---

## 3. The Scope-Boundary Rule for Function Names

Per Tellman, functions within pure functional scopes **avoid verbs** and name the resulting value, while operational cross-boundary functions **must use verbs**:

### Pure Transformations (No Verbs)
* ❌ `calculate-plerematic-overlap` $\to$ ✅ `plerematic-overlap`
* ❌ `determine-dependency-type` $\to$ ✅ `dependency-type`
* ❌ `compute-equivalence-map` $\to$ ✅ `equivalence-map` (alias `build-equivalence-map`)
* ❌ `create-mwe-trie` $\to$ ✅ `mwe-trie` (alias `build-mwe-trie`)

### Predicates (Ending in `?`)
* `member?`
* `commute?`
* `interdependence?`
* `determination?`
* `constellation?`

### Projection and Chunking
* `quotient-sequence` (alias `project-sequence`): Homomorphic sequence mapping.
* `chunk-sequence` (alias `chunk-mwe-sequence`): Greedy MWE token consolidation.
