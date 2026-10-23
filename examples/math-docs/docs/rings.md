---
id: rings
title: Ring Theory
domain: mathematics
author: CatDoc Example
tags:
  - algebra
  - ring-theory
  - mathematics
---

# Ring Theory

Ring theory is the study of rings—algebraic structures with two binary operations.

## Definition

A **ring** is a set R together with two binary operations `+` and `·` satisfying:

1. **(R, +)** is an abelian group with identity 0
2. **(R, ·)** is a monoid (associative with identity 1)
3. **Distributivity**: For all a, b, c in R:
   - `a · (b + c) = (a · b) + (a · c)` (left distributivity)
   - `(a + b) · c = (a · c) + (b · c)` (right distributivity)

## Examples

### Integers

The integers `ℤ` form a ring under standard addition and multiplication.

### Polynomials

For any ring R, the polynomial ring `R[x]` is also a ring.

### Matrices

The set of n×n matrices over a ring R forms a ring `Mₙ(R)`.

## Types of Rings

### Commutative Rings

A ring where multiplication is commutative: `a · b = b · a`

### Integral Domains

A commutative ring with no zero divisors.

### Division Rings

A ring where every non-zero element has a multiplicative inverse.

## Ideals

An **ideal** I of a ring R is a subset such that:
- (I, +) is a subgroup of (R, +)
- For all r in R and a in I: `r · a ∈ I` and `a · r ∈ I`

## Ring Homomorphisms

A **ring homomorphism** is a function `f: R → S` that preserves both operations:
- `f(a + b) = f(a) + f(b)`
- `f(a · b) = f(a) · f(b)`
- `f(1ᵣ) = 1ₛ`

## Related Topics

- @groups - The additive structure of a ring is a group
- @fields - Fields are rings with division
- @modules - Modules are like vector spaces over rings
- #group-to-ring - Adding multiplication to a group
