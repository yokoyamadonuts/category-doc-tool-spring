---
id: fields
title: Field Theory
domain: mathematics
author: CatDoc Example
tags:
  - algebra
  - field-theory
  - mathematics
---

# Field Theory

Field theory studies fields—algebraic structures where division (except by zero) is always possible.

## Definition

A **field** is a set F with two operations `+` and `·` such that:

1. **(F, +, ·)** is a commutative ring with identity
2. **(F \ {0}, ·)** is an abelian group (every non-zero element has a multiplicative inverse)

## Examples

### Rational Numbers

The rationals `ℚ` form the smallest field containing the integers.

### Real Numbers

The reals `ℝ` are a complete ordered field.

### Complex Numbers

The complex numbers `ℂ` are an algebraically closed field.

### Finite Fields

For any prime p, `ℤ/pℤ` is a field with p elements, denoted `𝔽ₚ`.

## Field Extensions

A **field extension** L/K is a pair of fields where K ⊆ L.

### Algebraic Extensions

An element α ∈ L is **algebraic** over K if it satisfies a polynomial with coefficients in K.

### Transcendental Extensions

An element is **transcendental** if it is not algebraic.

## Galois Theory

Galois theory connects field extensions with group theory:
- The **Galois group** of an extension L/K is the group of automorphisms of L that fix K
- This correspondence explains the solvability of polynomial equations

## Related Topics

- @rings - Fields are special kinds of rings
- #ring-to-field - The morphism adding multiplicative inverses
