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

Read the strategy file and identify all defined test levels. For each level, check whether a `### Design` section exists with real content (not placeholder text in square brackets). Then apply the following checks to the test changes from Step 1:

**Test level placement — apply to all levels**
- Are modified or added tests placed at the correct level as defined in the strategy?
- Are there tests that implement logic belonging to a different level?

**Framework architecture conformance — apply only to levels with a defined `### Design` section**
- Identify the architectural pattern described in the `### Design` section for this level (e.g. Screenplay pattern, Page Object Model, custom actor/DSL approach)
- Check whether new or modified tests follow that pattern — are the correct abstractions used?
- Flag any tests that bypass the defined pattern and interact with the system at a lower level than intended

**Scenario conformance — apply only to levels with a defined `### Design` section**
- Do new or modified tests correspond to scenarios or flows listed under `### Covers` for this level?
- Are there tests covering scenarios not present in `### Covers` — either misplaced or candidates for a strategy update?

## Step 4: Report results

Provide a structured summary:
- **Scope analyzed**: what test changes were reviewed
- **Levels detected in strategy**: list levels found, noting which have a `### Design` section
- **Well-designed**: tests that correctly follow the strategy
- **Design issues found**: specific tests with placement or design problems, with explanation of why
- **Recommendations**: concrete suggestions for each issue (e.g. move to unit level, refactor to use correct actor abstraction, update strategy `### Covers` if the scenario is valid)