---
name: git-commit
context: fork
description: ALWAYS use this skill to create any git commit in this repository. Do not run git commit directly. It creates a commit for the current changes, with a title that summarizes the goal of the PR and a bullet list of the changes. Use only when the user asks to commit.
---

# Commit

Make a commit only when the user asks for it.

## Steps

1. Run `git status` to find the staged changes, the changes that are not staged, and the untracked files.
2. If there are changes that are not staged or untracked files:
   1. Show a list of these files. For each file, give a short summary of its changes (use `git diff <file>`, or read the file if it is untracked).
   2. Ask the user which files to add to the commit.
   3. Add only the files that the user selects (`git add <file>`).
3. Run `git diff --staged` to see all the changes for the commit.
4. If the branch name has an issue number, read the issue with `gh issue view <number>` to find the goal of the PR.
5. Write the commit message in the format below. Describe only the staged changes.
6. Show the message to the user. Do not ask the user to approve the message. Continue to the next step immediately.
7. Commit. Use a HEREDOC to keep the format of the message.

## Message format

```
<Title>

* <Verb>s <change>
* <Verb>s <change>

Refactors:
* <Verb>s <change>

closes #<issue-id>
```

### Title

* Summarize the goal of the PR, not the individual changes.
* Start with a verb in the imperative form, for example "Add", "Fix", "Create".
* Do not put a period at the end of the title.

### Body

* Put an empty line between the title and the body.
* Use a bullet list with `*`. Write one bullet for each change.
* Start each bullet with a verb, for example "Adds", "Updates", "Deletes", "Moves", "Renames".
* Do not put a period at the end of a bullet.
* Make each bullet very concise. Give only what changed, not why or how.
* Do not include changes to tests. Include them only if they are refactors or architecture changes.

### Refactors

* Put all refactors at the end of the body, in a section with the title `Refactors:`.
* Do not put a `*` before the `Refactors:` title.
* If there are no refactors, do not add the section.

### Issue

* If an issue is associated with the changes, always put `closes #<issue-id>` on the last line, after an empty line.
* Get the issue number from the branch name. If the branch name has no issue number, ask the user for it.
* Add the line only when there is an associated issue.

### Trailers

* Do not add a `Co-Authored-By` line or other attribution lines.

## Language

Use ASD-STE100 Simplified Technical English (see `CLAUDE.md`).

## Example

```
Create a user guide to select Windows or Linux

* Adds build selection guide
* Adds build selection flow diagram
* Adds "docs" Makefile goal
* Shows Linux build first in README

Refactors:
* Moves shared login steps to "First start"

closes #126
```
