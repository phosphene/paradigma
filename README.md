# paradigma

> *paradigmatic substitution over notation.*  
> *vocabularies, synonym rings, and multi-word terminological expressions.*

`phosphene/paradigma` implements the vertical (paradigmatic) axis of structural language analysis in Clojure. It serves as the vocabulary complement to `phosphene/syntagma` (the horizontal order-relation engine).

---

## Capabilities

1. **Formal Vocabularies (`phosphene.paradigma.core/create-vocabulary`):**
   - Immutable vocabularies with domain metadata and set membership semantics.
2. **Equivalence Rings & Quotient Projection (`build-equivalence-map`, `project-sequence`):**
   - Maps tokens through synonym rings ($t \mapsto [t]_\sim$) for seamless composition with `phosphene.syntagma.core/equiv-subsequence?`.
3. **Vocabulary Morphisms (`vocabulary-morphism`, `apply-morphism`):**
   - Order-preserving maps between historical and modern scientific terminology without hermeneutic inflation.
4. **Multi-Word Expression (MWE) Trie Chunking (`build-mwe-trie`, `chunk-mwe-sequence`):**
   - Deterministic prefix-trie chunking of compound scientific terms (*"lines of magnetic force"*) into singular compound notation tokens before sequence verification.
5. **Generative Verification with `clojure.spec.alpha` and `test.check`:**
   - Formal property testing covering length preservation invariants and trie monotonic boundaries.

---

## Testing

```bash
clojure -M:runner
```
