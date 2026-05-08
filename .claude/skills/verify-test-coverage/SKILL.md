---
name: verify-test-coverage
description: Verify test coverage for code changes against the defined test strategy. Use when reviewing a feature branch, commit, or uncommitted changes to ensure adequate test coverage.
arguments: [scope]
argument-hint: [uncommitted|last-commit|last-N-commits|branch]
disable-model-invocation: true
allowed-tools: Bash(git *) Read Grep
---

## Verify Test Coverage

Analyze test coverage for code changes and identify gaps based on the test strategy.

## Step 1: Detect scope of change

The scope defines which code changes to analyze for test coverage.

**Scope options:**
- If `$scope` is "uncommitted" or "unstaged": analyze uncommitted changes (`git diff HEAD`)
- If `$scope` is "last-commit": analyze the most recent commit (`git diff HEAD~1 HEAD`)
- If `$scope` matches pattern "last-N-commits": analyze last N commits from HEAD
- If `$scope` is empty or "branch": analyze diff between current feature branch and main (`git diff main...HEAD`)
- If `$scope` is a custom git range (e.g., "commit1..commit2"): use that range
- If `$scope` is unclear or not provided, ask the user what scope they want to analyze

Determine the appropriate git command and execute it to get the diff.

## Step 2: Locate test strategy

Look for the test strategy definition to understand what needs to be tested.

**Strategy location:**
- First, check if `strategy.md` exists in the repository root
- If not found, check `.claude/` directory for strategy documentation
- If user mentioned a specific strategy in their prompt, use that instead
- If no strategy file found, ask the user where the test strategy is defined

## Step 3: Review current test coverage

Based on the code changes from Step 1 and test strategy from Step 2:

1. Identify what files were changed and what functionality was added/modified
2. Search for existing tests covering those changes
3. Compare existing test coverage against the test strategy requirements
4. Determine coverage according to the test levels defined in the strategy file

## Step 4: Report results and gaps

Provide a summary:
- **Scope analyzed**: what code changes were reviewed
- **Test coverage found**: list existing tests covering the changes
- **Gaps identified**: what's missing based on the test strategy
- **Recommendations**: specific tests that should be added to close the gaps

If gaps exist, propose concrete test cases that need to be written.