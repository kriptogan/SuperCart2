# Coding rules

- Any coding should be done with the TDD method: write a failing test first, make it pass, then refactor.
  When refactoring existing code that has no tests, first add characterization tests that pin current behaviour.
- Files should not be bigger than 300 lines of code.
  Applies to production and test code.
- Functions should not be bigger than 6 lines of code.
  - Counted as body lines: lines between the opening and closing brace (or after `=` for expression
    bodies), excluding the signature line, the closing brace, blank lines and comment-only lines.
  - Lambdas inside a function (including Compose content lambdas) count toward that function's body.
  - Test functions are exempt from this rule (they only need to respect the 300-line file limit).

Check with: `bash tools/check-size-rules.sh` (non-zero exit when a rule is broken).
