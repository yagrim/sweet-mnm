# CLAUDE.md

## Rules

1. Always use ASD-STE100 Simplified Technical English. This applies to all text: answers in the CLI and text in files (comments, documentation, commit messages).
2. Do not add JavaDoc comments unless the user asks for them explicitly.
3. Each new feature and each fix must have test(s).
4. To make a git commit, always use the `/git-commit` skill. Do not run `git commit` directly. Make a commit only when the user asks for it.

## Development

- The Makefile controls the tests.
- To run the full test suite, use `make test`.
