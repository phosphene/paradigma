---
uri: paradigma/doc/semiotic-square
title: Algirdas Julien Greimas's Semiotic Square (Carré Sémiotique) in phosphene.paradigma
owner: edphos
status: living
updated: 2026-11-14
description: "Theoretical framework and Clojure implementation of Greimas's elementary structure of signification, four-vertex relational axes, and narrative trajectories."
---

# Greimas's Semiotic Square (*Carré Sémiotique*) in `phosphene.paradigma`

> *"The elementary structure of signification is defined as the logical development of a binary category."*  
> — Algirdas Julien Greimas, *Sémantique structurale* (1966) & *Du Sens* (1970)

`phosphene.paradigma.greimas` extends our Hjelmslevian glossematics foundation by implementing the **Semiotic Square**—the canonical 20th-century structuralist model of how semantic opposition generates meaning.

---

## 1. Mathematical & Logical Topology

Given an elementary contrary pair $S_1$ and $S_2$ along a semantic axis (e.g. Life vs. Death, Nature vs. Culture, Being vs. Seeming):

$$\begin{array}{ccc}
S_1 & \overset{\text{contrariety}}{\longleftrightarrow} & S_2 \\
\left\uparrow\vphantom{\Big\downarrow}\right.\text{deixis} & \times & \left\uparrow\vphantom{\Big\downarrow}\right.\text{deixis} \\
\sim S_2 & \underset{\text{sub-contrariety}}{\longleftrightarrow} & \sim S_1
\end{array}$$

### The Three Structural Axes
1. **Axis of Contrariety (Horizontal):**
   - $S_1 \leftrightarrow S_2$: Mutual exclusion. Both cannot be true simultaneously, but both can be false.
   - $\sim S_1 \leftrightarrow \sim S_2$: Sub-contrariety on the negative plane.
2. **Axis of Contradiction (Diagonal):**
   - $S_1 \leftrightarrow \sim S_1$ and $S_2 \leftrightarrow \sim S_2$: Strict binary negation. One must be true, one false.
   - Invariant: Double negation is identity ($\sim(\sim S) = S$).
3. **Axis of Deixis / Presupposition (Vertical):**
   - Positive Deixis: $\sim S_2 \implies S_1$ (e.g. non-death implies life).
   - Negative Deixis: $\sim S_1 \implies S_2$ (e.g. non-life implies death).

### The Meta-Terms (Syntheses)
- **Complex Term:** $S_1 + S_2$ (e.g. Being + Seeming = **Truth**; Life + Death = **Undead / Avatar**).
- **Neutral Term:** $\sim S_1 + \sim S_2$ (e.g. Non-being + Non-seeming = **Falsity**; Non-life + Non-death = **Inorganic / Void**).

---

## 2. Clojure Implementation (`phosphene.paradigma.greimas`)

```clojure
(require '[phosphene.paradigma.core :as para])

;; 1. Construct Semiotic Square
(def sq (para/semiotic-square :square/veridiction :being :seeming
                              {:non-s1 :non-being :non-s2 :non-seeming
                               :complex :truth :neutral :falsity}))

;; 2. Relational Queries
(para/contrary? sq :being :seeming)
;;=> true

(para/contradictory? sq :being :non-being)
;;=> true

(para/presupposes? sq :non-seeming :being)
;;=> true

;; 3. Canonical Narrative Trajectory: S_1 -> ~S_1 -> S_2
(para/narrative-transition sq :being)
;;=> {:trajectory/initial :being,
;;    :trajectory/negation :non-being,
;;    :trajectory/destination :seeming,
;;    :trajectory/valid? true}

;; 4. Empirical Feature Projection
(para/project-onto-square sq [:being :seeming])
;;=> {:projection/position :complex, :projection/term :truth, :projection/square :square/veridiction}
```
