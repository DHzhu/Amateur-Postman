# Implementation Plan - Variable Resolver Logic Optimization

## Phase 1: Benchmarking & Reproducibility ✅ PLANNED
- [ ] Task: Create a benchmark test in `VariableResolverTest.kt` with a large body (1MB+) containing 500+ variable placeholders.
- [ ] Task: Measure current execution time and memory allocations (using a simple profiling script or IntelliJ's built-in tools).

## Phase 2: Core Algorithm Refactoring ✅ PLANNED
- [ ] Task: Implement a single-pass `StringBuilder` based substitution algorithm in `VariableResolver.kt`.
- [ ] Task: Ensure the new algorithm correctly identifies `{{` and `}}` markers, handling nested-like or broken braces gracefully.
- [ ] Task: Integrate existing built-in function resolution ($timestamp, $uuid) into the new single-pass logic.

## Phase 3: Recursive & Edge Case Handling ✅ PLANNED
- [ ] Task: Port recursive resolution logic to the new algorithm while maintaining the `MAX_RECURSION_PASSES` safety limit.
- [ ] Task: Implement tests for edge cases: empty variables, missing closing braces, nested variables `{{outer_{{inner}}}}`, and extremely long variable names.

## Phase 4: Performance Verification ✅ PLANNED
- [ ] Task: Run the benchmark created in Phase 1 and compare the results with the baseline.
- [ ] Task: Profile the new implementation to confirm reduced object allocations.

## Phase 5: Verification & Audit ✅ PLANNED
- [ ] Task: Run all project unit tests (330+) to ensure no regressions in variable resolution.
- [ ] Task: Conductor - Final Audit & Documentation.
