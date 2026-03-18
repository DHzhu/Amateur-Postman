# Specification - Variable Resolver Logic Optimization

## Background
The current `VariableResolver` uses a regex-based `findAll` and repeated `String.replace` calls. This approach creates many temporary string objects and is inefficient (potentially $O(N^2)$ complexity) when many variables are present in a large body.

## Goals
- **Algorithm Optimization**: Implement a single-pass scanner to identify and substitute variables.
- **Memory Efficiency**: Use `StringBuilder` to minimize object allocations during substitution.
- **Recursive Support**: Retain support for recursive variable resolution with a safety depth limit.
- **Scalability**: Support substitution in bodies up to 10MB with minimal memory overhead.

## Architecture
- **Single-Pass Scanner**: A custom parser that iterates through the input string once, searching for `{{` and `}}` markers.
- **Caching**: Cache variable lookups during a single substitution pass.
- **Refactoring**: Replace `variableRegex.findAll(text).forEach { ... result = result.replace(...) }` with a more efficient `StringBuilder` approach.

## Acceptance Criteria
1. No performance degradation for standard small requests.
2. Significant performance improvement (at least 2x faster) for bodies with 100+ variable occurrences.
3. Memory object churn (GC pressure) is reduced during variable resolution.
4. Correctly handles recursive variables and built-in functions ($uuid, $timestamp).
