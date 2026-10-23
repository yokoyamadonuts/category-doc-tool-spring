---
id: groups
title: Group Theory
domain: mathematics
author: CatDoc Example
tags:
  - algebra
  - group-theory
  - mathematics
---

# Group Theory

Group theory is the study of algebraic structures known as groups.

## Definition

A **group** is a set G together with a binary operation `*` that satisfies four axioms:

1. **Closure**: For all a, b in G, the result of `a * b` is also in G
2. **Associativity**: For all a, b, c in G, `(a * b) * c = a * (b * c)`
3. **Identity**: There exists an element e in G such that for all a in G, `e * a = a * e = a`
4. **Inverse**: For each a in G, there exists an element b in G such that `a * b = b * a = e`

## Examples

### Integers under Addition

The set of integers `ℤ` forms a group under addition:
- Identity: 0
- Inverse of n: -n

### Non-zero Rationals under Multiplication

The set `ℚ*` (non-zero rationals) forms a group under multiplication:
- Identity: 1
- Inverse of q: 1/q

### Symmetric Groups

The symmetric group `Sₙ` consists of all permutations of n elements.

## Subgroups

A **subgroup** H of G is a subset of G that is itself a group under the same operation.

## Homomorphisms

A **group homomorphism** is a function `f: G → H` between two groups that preserves the group operation:

```
f(a * b) = f(a) * f(b)
```

## Related Topics

- @sets - Groups are built on sets with additional structure
- @rings - Adding another operation leads to rings
- #set-to-group - The morphism adding group structure

## Category Theory Perspective

In the category **Grp** of groups:
- Objects are groups
- Morphisms are group homomorphisms
- Composition is function composition
- Identity morphisms are identity functions
