---
name: test-design-review
description: Review test design quality for code changes against the defined test strategy. Use when reviewing a feature branch, commit, or uncommitted changes to ensure new or modified tests are well-designed, placed at the correct test level, and conform to the architecture defined in the strategy.
arguments: [scope]
argument-hint: [uncommitted|last-commit|last-N-commits|branch]
disable-model-invocation: true
allowed-tools: Bash(git *) Read Grep
---

## Test Design Review

Analyze the design quality of tests introduced or modified in a code change, and verify they conform to the defined test strategy.

## Step 1: Detect scope of change

The scope defines which code changes to analyze for test design quality.

**Scope options:**
- If `$scope` is "uncommitted" or "unstaged": analyze uncommitted changes (`git diff HEAD`)
- If `$scope` is "last-commit": analyze the most recent commit (`git diff HEAD~1 HEAD`)
- If `$scope` matches pattern "last-N-commits": analyze last N commits from HEAD
- If `$scope` is empty or "branch": analyze diff between current feature branch and main (`git diff main...HEAD`)
- If `$scope` is a custom git range (e.g., "commit1..commit2"): use that range
- If `$scope` is unclear or not provided, ask the user what scope they want to analyze

Determine the appropriate git command and execute it to get the diff. Focus on test files introduced or modified in the diff.

## Step 2: Locate test strategy

Look for the test strategy definition to understand the expected design and level structure.

**Strategy location:**
- First, check if `TEST_STRATEGY.md` exists in the repository root
- If not found, check `.claude/` directory for strategy documentation
- If user mentioned a specific strategy in their prompt, use that instead
- If no strategy file found, ask the user where the test strategy is defined

## Step 3: Review test design quality

Read the strategy file and identify all defined test levels. For each level, check whether a `### Design` section exists with real content (not placeholder text in square brackets). Only evaluate levels that have test file changes present in the diff from Step 1 — levels with no changes are out of scope, do not report on them.

**Test level conformance**
- Using `### Purpose`, `### Covers`, and `### Excludes` as the definition of what belongs at this level, evaluate whether each new or modified test belongs here based on what it does and how it is written
- Flag tests whose nature or granularity contradicts the level's intent — e.g. a single field validation in an E2E test, or a full flow assertion in a unit test

**Framework architecture conformance** — apply only to levels with a defined `### Design` section
- Identify the architectural pattern described in the `### Design` section (e.g. Screenplay pattern, Page Object Model, custom actor/DSL approach)
- Check whether new or modified tests follow that pattern — are the correct abstractions used and is the defined pattern respected throughout?
- Flag any tests that bypass the defined pattern and interact with the system at a lower level than intended

## Step 4: Report results

Provide a structured summary:
- **Scope analyzed**: what test changes were reviewed
- **Levels reviewed**: only levels that had changes in the diff
- **Well-designed**: tests that correctly follow the strategy
- **Design issues found**: specific tests with placement or design problems, with explanation of why
- **Recommendations**: concrete suggestions for each issue (e.g. move to unit level, refactor to use correct actor abstraction)