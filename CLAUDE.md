# CLAUDE.md

## Rules

1. Always use ASD-STE100 Simplified Technical English. This applies to all text: answers in the CLI and text in files (comments, documentation, commit messages).
2. Do not add JavaDoc comments unless the user asks for them explicitly.
3. Each new feature and each fix must have test(s).
4. Implement each feature and each fix in its own git branch. Do not work directly on `main`.
5. To make a git commit, always use the `/git-commit` skill. Do not run `git commit` directly. Make a commit only when the user asks for it.
6. Do not add references to Claude in commits or pull requests. For example, do not add a "Co-Authored-By: Claude" line to commit messages, and do not add a "Generated with Claude Code" line to PR descriptions. Reason: the human author is responsible for the code, not Claude.
7. Each constructor that exists only for tests must have this line comment directly above it: `// Test-only constructor. Do not call it from production code.` A test-only constructor must not be `public`. Use package-private access.

## Development

- The Makefile controls the tests.
- To run the full test suite, use `make test`.
