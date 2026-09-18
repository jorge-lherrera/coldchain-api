# `rules/` — what this project enforces

This folder is the **normative authority of the project**. If a rule is not here, it is not a rule of
this project; if a document in any other folder says otherwise, this one wins.

It sits at the repository root rather than inside `docs/` on purpose: `docs/` explains how the system
works, and this is a different thing — it is what cannot be broken.

## What is here

| File | What it is |
|---|---|
| **[`project-rules.md`](project-rules.md)** | **The catalogue.** Every rule, from the folder down to the field, with its severity and who checks it. Start here |
| [`database-rules.md`](database-rules.md) | The long statement and the reasoning behind the `N0`–`N9` rules that the catalogue indexes in group R7. It does not declare: it explains |

## How they are checked

```bash
./gradlew rules      # every rule that does not need Oracle. This is the CI gate
./gradlew rulesDb    # the ones that can only be checked against a real database
```

`rules` runs on **every push and every pull request**. That is what makes a rule mandatory; without
it, complying would depend on each person remembering.

The gate does not pick by a list of classes, it picks **by location**: every enforcement class lives
directly in `com.coldchain` under `src/test`. A new rule placed there joins the gate on its own, and
`RuleGateCoverageTest` fails if somebody writes a rule outside that package, where CI would not see it.

## How to read a row

Each rule has a **stable id** (`R<group>.<n>`). Within a group the ids run consecutively, with no
gaps: the number means nothing on its own, it only places the rule in its group.

| Column | Values | What it answers |
|---|---|---|
| **Sev** | `SEC` `INT` `CON` `COST` `STYLE` | What breaks if it is not followed |
| **Enforcer** | a class or `Class.method`, several separated by ` · `, or `—` | Who checks it |
| **Machine** | `yes` `partial` `planned` `none` | Does the check exist? |
| **Status** | `green` `red` `pending` `—` | Does it pass right now? |

**`Machine` and `Status` are independent axes, and the distinction matters.** A rule with a machine
that is currently red (`yes` / `red`) is *working*: it caught something, and a waiver says who accepted
it. A rule with no machine (`none` / `—`) has caught nothing because nobody is looking. The first is
known outstanding work; the second is an unknown risk.

`planned` / `pending` is this project's third state, and it exists because the catalogue was written
**before** the code. It means: the rule is in force, the enforcer is named, and the branch that builds
that area is the one that must deliver it. A module does not close while a rule of its area is still
`pending` (R14.3), and no row carries that state today: the last of them was closed in
`feat/rules-runtime`. It stays documented because the next rule written before its machine will use it,
and because `everyPlannedEnforcerIsStillMissing` turns it red the moment the machine appears.

### Severity

It orders the backlog. Without it, tenant isolation formally weighs the same as import order.

| Code | Meaning | What happens if broken |
|---|---|---|
| **SEC** | Security | Somebody sees data they must not, or does something they cannot |
| **INT** | Data integrity | A value is corrupted or lost, or a number becomes false |
| **CON** | Contract | The client breaks. The API stops being the one the consumer implemented |
| **COST** | Cost and availability | Performance degrades or the system stops responding, corrupting nothing |
| **STYLE** | Style and maintenance | It does not change what the system does; it changes what it costs to read and change it |

### In force vs proposed

A rule **in force** lives in its group's table. A **proposal** lives in that group's `Proposals` table
and **obliges nothing** until it is approved.
