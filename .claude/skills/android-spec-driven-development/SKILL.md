---
name: android-spec-driven-development
description: Converts Android PRD into an engineering spec before writing any code. Use when starting a new Android feature, screen, or significant change. Use when requirements are vague, touch multiple Clean Architecture layers, or require architectural decisions. Use when given a PRD document and asked to implement it.
---

# Android Spec-Driven Development

## Overview

Translate a PRD into a concrete Android engineering spec before writing a single line of code. The spec defines UiState models, API contracts, Clean Architecture layer boundaries, and acceptance criteria that all parties agree on. Code written without a spec embeds PM assumptions and architectural guesses that compound into rework.

## When to Use

- Receiving a PRD or feature requirement for a new screen or flow
- Requirements touch more than one Clean Architecture layer (Data / Domain / UI)
- Architectural decisions must be made (navigation strategy, DI scope, state model)
- The task would take more than 30 minutes to implement
- Multiple engineers or agents will work on the same feature

**When NOT to use:** Single-line bug fixes, config-only changes, or copy/text updates with no behavioral impact.

## The Gated Workflow

Do not advance to the next phase without explicit human confirmation. The Remediation Loop in Phase 5 is the only automated step.

```
SPECIFY ──→ PLAN ──→ CONTRACT ──→ TASKS ──→ IMPLEMENT ──→ [ESCALATE]
   │          │          │          │          │                │
   ▼          ▼          ▼          ▼          ▼                ▼
 Human      Human      Human      Human     Auto loop        Human
 reviews    reviews    reviews    reviews   (max 3x)         decides
```

### Skip Guard（防呆機制）

**在每次回應使用者訊息之前，必須先判斷：此訊息是否隱含跳過任何尚未完成的 step 或 phase？**

觸發情境包括但不限於：
- 使用者說「確認」、「直接做」、「開始實作」、「跳過」
- 使用者在 Phase 1 結束後直接要求實作，跳過 Phase 2–4
- 使用者在 Phase 1 Step 1 完成後直接要求寫 spec，跳過 Step 2（確認技術假設）
- 使用者在 Phase 3 Step 1 完成後直接要求實作，跳過 Step 2–3（鎖定 interface 與 test stubs）
- 任何其他造成「目前所在步驟 → 被要求執行的步驟」之間存在未完成步驟的情況

偵測到跳過時，**禁止直接執行**，必須先輸出：

```
⚠️ SKIP DETECTED — 以下步驟尚未完成：

  Phase X / Step Y：<步驟名稱>
  Phase X / Step Z：<步驟名稱>
  ...

跳過風險：<一句話說明此步驟被略過的具體後果>

請選擇：
  A. 確定跳過，我了解風險
  B. 依序執行剩餘步驟（建議）
```

僅在使用者**明確回覆 A** 之後，才可跳過並繼續。
回覆 B 或任何非明確 A 的回應，一律進入下一個未完成的步驟。

---

## Phase 1: Specify

### Step 1: PRD Blind-Spot Detection

Before writing anything, scan the PRD for missing definitions. If any blind spots are found, **stop immediately and list them**. Do not proceed until the human answers.

```
BLIND SPOTS FOUND — TASK PAUSED:
1. PRD does not define behavior when the user's account is suspended
2. "Pull to refresh" — does it replace existing data or merge?
3. Pagination end behavior is undefined (infinite scroll vs. load-more button)
4. Error state copy is missing — who owns this content?
→ Please answer the above before I continue to spec writing.
```

Only proceed to Step 2 when zero blind spots remain.

### Step 2: Surface Android-Specific Assumptions

List technical assumptions and confirm before writing the spec:

```
ASSUMPTIONS I'M MAKING:
1. Min SDK: 21 (Android 5.0)
2. UI layer: Fragment + ViewBinding（專案混用 Compose，但以 Fragment+ViewBinding 為主；
   僅當功能本身已是 Compose 時才用 Compose）
3. DI: Hilt with @HiltViewModel
4. Navigation: Jetpack Navigation Component with Safe Args
5. Async: Kotlin Coroutines + StateFlow (not RxJava)
6. Network: Retrofit + Gson + OkHttp
7. Module: 單一 :app module（./gradlew :app:testDebugUnitTest）
8. Error handling contract: Result<T> at all layer boundaries
9. This screen is part of the existing nav graph (not a new standalone flow)
→ Correct me now or I'll proceed with these.
```

### Step 3: Write the Android Engineering Spec

Produce a spec document covering these eight areas:

**1. Objective**
What are we building? Who is the user? What user problem does this solve? What does success look like in user-visible terms?

**2. Screen Inventory**
List every screen and its states. For each screen, define `UiState` as a `sealed interface` and user actions as a `sealed interface`:

```kotlin
// sealed interface allows implementations to extend other classes
sealed interface InvoiceListUiState {
    data object Loading : InvoiceListUiState
    data class Success(val invoices: List<Invoice>) : InvoiceListUiState
    data class Error(val message: String) : InvoiceListUiState
    data object Empty : InvoiceListUiState
}

sealed interface InvoiceListViewIntent {
    data class OnInvoiceClicked(val invoiceId: String) : InvoiceListViewIntent
    data object OnRetryClicked : InvoiceListViewIntent
    data object OnRefresh : InvoiceListViewIntent
}
```

Every state a user can observe must be modeled. `Loading` and `Empty` are not optional.

**3. API Contracts**
Define request/response schemas and error codes. Flag every nullable or undefined field — these become defensive coding requirements.

```
GET /api/v1/invoices
Query: page: Int, pageSize: Int, status: String?

Response 200:
{
  "data": [{ "id": "string", "amount": number, "status": "PAID|UNPAID", "date": "ISO8601" }],
  "total": number,
  "hasNextPage": boolean
}
Error 401: { "code": "UNAUTHORIZED" }
Error 500: { "code": "SERVER_ERROR", "message": "string" }

⚠ "amount" — confirm currency unit (cents or float?)
⚠ "date" — confirm timezone handling
```

All errors cross layer boundaries as `Result<T>`. Direct `throw` and silent `catch` are prohibited.

**4. Clean Architecture Layer Map**
Define concrete class names per layer for this feature:

```
UI Layer
  └─ InvoiceListScreen (Composable) / InvoiceListFragment
  └─ InvoiceListViewModel

Domain Layer
  └─ GetInvoiceListUseCase
  └─ Invoice (Domain model)
  └─ InvoiceRepository (interface — impl lives in Data)

Data Layer
  └─ InvoiceRepositoryImpl
  └─ InvoiceApiService (Retrofit)
  └─ InvoiceDto (Network model)
  └─ InvoiceDtoMapper
```

No layer imports from a layer above it. `Context` never enters Domain or Data.

**5. Build & Test Commands**

```
Build debug APK:     ./gradlew assembleDebug
Run unit tests:      ./gradlew :feature-invoice:testDebugUnitTest
Static analysis:     ./gradlew detekt ktlintCheck
Full gate:           ./gradlew detekt ktlintCheck :feature-invoice:testDebugUnitTest
```

**6. Testing Strategy**

| Layer | Framework | What to test |
|-------|-----------|-------------|
| ViewModel | JUnit 5 + Mockk + Turbine | UiState transitions, error handling, ViewIntent mapping |
| UseCase | JUnit 5 + Mockk | Business logic, edge cases, Result<T> propagation |
| Repository | JUnit 5 + Mockk | Error mapping, DTO → Domain mapping, Result<T> wrapping |
| UI | Compose UI Test / Espresso | Critical happy path only |

Coverage expectation: ViewModel and UseCase layers ≥ 80%. UI tests for happy path only.

**7. Boundaries**

- **Always:** Null-safe Kotlin (`!!` requires a written justification), `_binding = null` in `onDestroyView`, Flow collected via `repeatOnLifecycle`, coroutines only in `viewModelScope`, all cross-layer errors wrapped in `Result<T>`
- **Ask first:** Adding a Gradle module, changing the navigation graph, introducing a new dependency, changing an existing DI scope
- **Never:** `Context` or `Activity` in ViewModel, log sensitive user data, store tokens in plain `SharedPreferences`, use `GlobalScope`, add `@Suppress` to silence static analysis, modify or delete a locked test stub to make a build pass, swallow exceptions with an empty `catch` block

If the feature handles authentication, PII, tokens, or untrusted external input (Deep Links, Intents), apply `security-and-hardening` during this step to identify the full Android threat surface before implementation begins.

**8. Success Criteria**

Reframe vague PRD requirements into specific, testable conditions:

```
PRD: "Show the invoice list"

SUCCESS CRITERIA:
- [ ] Invoice list renders within 2s on a real device (Pixel 5, Android 12)
- [ ] Empty state shown when API returns 0 items
- [ ] Error state with retry CTA shown on network failure
- [ ] Loading skeleton shown while fetching
- [ ] Tapping an invoice navigates to detail screen with correct invoiceId via Safe Args
- [ ] No memory leaks detected by LeakCanary in debug build
- [ ] Full gate passes: ./gradlew detekt ktlintCheck :feature-invoice:testDebugUnitTest
→ Confirm these criteria with PM before proceeding.
```

### Spec Template

```markdown
# Android Spec: [Feature Name]

## Objective
## Assumptions
## Screen Inventory         ← sealed interface UiState + ViewIntent per screen
## API Contracts            ← request, response, error codes, flagged ambiguities
## Clean Architecture Layer Map
## Build & Test Commands
## Testing Strategy
## Boundaries               ← Always / Ask first / Never
## Success Criteria         ← specific, testable, confirmed with PM
## Open Questions           ← zero remaining before proceeding
```

---

## Phase 2: Plan

Follow the `planning-and-task-breakdown` skill. Use this Android dependency graph as the starting point for Step 2 (Identify the Dependency Graph):

```
API Contract (spec)
    │
    ├── ApiService + DTO models          ← Data Layer foundation
    │       │
    │       └── RepositoryImpl           ← Data Layer
    │               │
    │               └── UseCase          ← Domain Layer
    │                       │
    │                       └── ViewModel (UiState / ViewIntent)  ← UI Layer
    │                               │
    │                               └── Fragment / Composable     ← UI Layer
    │
    └── Unit Tests (parallel with UI layer tasks)
```

Layers must be built bottom-up. Test tasks (Task 7–8) can run in parallel with UI tasks (Task 5–6).

---

## Phase 3: Contract Lock

**Goal: Freeze the behavioral contract before writing any implementation.**

This phase converts the spec into code artifacts that are locked for the remainder of development. Implementation in Phase 5 fills these in — it does not redefine them.

### Step 1: Write Gherkin Scenarios

Translate each UiState transition into a Given-When-Then scenario:

```gherkin
Feature: Invoice List

  Scenario: Successful load
    Given the user opens the invoice list screen
    When the repository returns a non-empty list
    Then the ViewModel emits Success state with the invoice data

  Scenario: Empty list
    Given the user opens the invoice list screen
    When the repository returns an empty list
    Then the ViewModel emits Empty state

  Scenario: Network failure
    Given the user opens the invoice list screen
    When the repository returns Result.failure
    Then the ViewModel emits Error state with the failure message

  Scenario: Retry after error
    Given the ViewModel is in Error state
    When the user sends OnRetryClicked intent
    Then the ViewModel emits Loading state and re-fetches
```

### Step 2: Lock the Interface Definitions

Write Repository and UseCase interfaces with no implementation. These are the layer contracts:

```kotlin
// LOCKED — do not modify during Phase 5
interface InvoiceRepository {
    suspend fun getInvoices(page: Int, pageSize: Int): Result<List<Invoice>>
}

// LOCKED — do not modify during Phase 5
interface GetInvoiceListUseCase {
    suspend operator fun invoke(page: Int): Result<List<Invoice>>
}
```

### Step 3: Lock the ViewModel Test Stubs

Write JUnit 5 test stubs that map directly to the Gherkin scenarios. These stubs must fail (they have no implementation to pass against). That is correct.

```kotlin
// LOCKED — do not modify or delete during Phase 5
class InvoiceListViewModelTest {

    private val repository: InvoiceRepository = mockk()
    private val useCase: GetInvoiceListUseCase = mockk()

    @Test
    fun `loading state emitted on init`() { /* stub */ }

    @Test
    fun `success state emitted when use case returns non-empty list`() { /* stub */ }

    @Test
    fun `empty state emitted when use case returns empty list`() { /* stub */ }

    @Test
    fun `error state emitted when use case returns failure`() { /* stub */ }

    @Test
    fun `retry intent triggers loading then re-fetch`() { /* stub */ }
}
```

### Lock Declaration

After producing the above artifacts, state explicitly:

```
CONTRACT LOCKED.
The following are immutable for the rest of this feature:
- InvoiceRepository interface
- GetInvoiceListUseCase interface
- InvoiceListViewModelTest stubs

Any conflict between implementation and these contracts must be escalated (Phase 6),
not resolved by modifying the contracts.
```

---

## Phase 4: Tasks

Follow the `planning-and-task-breakdown` skill for task structure and sizing. Use this layer order as the default sequence:

```
Task 1: Data Layer — ApiService + DTO models
Task 2: Data Layer — RepositoryImpl + error mapping (DTO → Domain, Result<T>)
Task 3: Domain Layer — Domain model + UseCase implementation
Task 4: ViewModel — StateFlow logic filling in the locked test stubs
Task 5: UI — Fragment/Composable layout + state binding
Task 6: UI — Navigation wiring + Safe Args
Task 7: Tests — Complete ViewModel test stubs    ← parallel with Task 5–6
Task 8: Tests — Repository unit tests            ← parallel with Task 5–6
```

Verification for every task:
- `./gradlew :feature-module:testDebugUnitTest`
- `./gradlew assembleDebug`
- Manual smoke test on emulator or real device

---

## Phase 5: Implement + Remediation Loop

Follow `incremental-implementation` — build one layer, verify, commit before moving on.
Follow `test-driven-development` — complete the locked test stubs before finalizing the ViewModel.
Follow `git-workflow-and-versioning` for branch naming and commit discipline.

At each layer transition, verify the contract before building the next layer:

```
RepositoryImpl returns correct Result<T> → implement UseCase
UseCase propagates Result<T> correctly   → implement ViewModel
ViewModel emits correct UiState          → implement UI
UI renders all UiState branches          → smoke test on device
```

### Remediation Loop

After each implementation increment, run the full gate:

```
./gradlew detekt ktlintCheck :feature-module:testDebugUnitTest
```

```
PASS ──→ Commit and continue to next task

FAIL ──→ Read the error log in full
         Identify root cause in implementation (never in the locked contract)
         Fix the implementation
         Re-run the full gate
         │
         └─ Still failing after 3 attempts? ──→ Phase 6: Escalation
```

**Remediation rules:**
- Fix the implementation, never the locked test stubs
- Never add `@Suppress` to silence a detekt or lint warning
- Never disable or delete a test to make the build pass
- Count retries from the first failure — a new class of error resets the counter

Before opening a PR, run `code-review-and-quality` as a self-review pass.

---

## Phase 6: Escalation

**Triggered when:** Phase 5 remediation fails 3 consecutive times, or when a fundamental conflict is found between the spec and implementation that cannot be resolved within the locked contracts.

**Stop all code changes immediately.**

Produce `ESCALATION_REPORT.md`:

```markdown
# Escalation Report: [Feature Name]

## Conflict Description
[Precise description of the conflict. Example: "The UI spec requires displaying
a success state for invoices with zero amount, but the Repository contract
defines Result.failure for this case per the API error contract."]

## Attempted Solutions
1. [What was tried] → [Why it failed]
2. [What was tried] → [Why it failed]
3. [What was tried] → [Why it failed]

## Architectural Options
**Option A:** [Description] — trade-offs: [...]
**Option B:** [Description] — trade-offs: [...]

## Decision Required
Which option should I proceed with?
```

Wait for human decision before resuming any implementation.

---

## Keeping the Spec Alive

- Update the spec when API contracts change — never let the spec drift from the implementation
- Commit the spec alongside the code (`docs/specs/<feature-name>.md`)
- Reference the spec in the PR description: "Implements Phase 1 of docs/specs/invoice-list.md"
- When scope changes mid-implementation, update the spec first, then resume
- Record significant architectural decisions (choosing Compose over Views, introducing a new DI scope) using `documentation-and-adrs`

---

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "The PRD is clear enough, I can start coding" | PRDs define product behavior, not UiState models, Result<T> contracts, or layer ownership. The spec converts PM intent into engineering decisions. |
| "I'll define the UiState as I go" | Undefined state means missing branches. `Empty` and `Error` are discovered at review time, not design time. |
| "The API isn't ready, I'll mock it later" | Define the contract now. A mock built without a contract is built twice. |
| "This is small, no spec needed" | Small screens fail on edge cases that only a spec forces you to enumerate. |
| "I know the architecture, I don't need the layer map" | The layer map is for the reviewer, the PR, and the next engineer — not for you. |
| "I'll add `@Suppress` just this once to unblock progress" | Suppressing a warning hides a real problem. Fix the code; don't silence the tool. |
| "The test is wrong, I'll adjust it to match my implementation" | The locked test stub represents the agreed contract. If the test seems wrong, escalate — don't modify it. |
| "I'll handle errors properly later" | `Result<T>` wrapping is a layer contract, not a polish step. Retrofitting it breaks callers. |
| "Three retries isn't enough, let me try one more" | The iteration limit exists to prevent infinite loops. Escalate and get a human decision. |

---

## Red Flags

- Writing a `ViewModel` before defining its `sealed interface UiState`
- Starting UI implementation before the Repository interface is locked
- No `Empty` state in the UiState model (every list screen can be empty)
- API response has nullable fields with no `Result<T>` handling plan
- `Context` appearing in a UseCase or Repository constructor
- `@Suppress` added to silence a static analysis warning
- A locked test stub was modified to match the implementation
- An exception is caught with an empty `catch` block (silent failure)
- `throw Exception(...)` appearing in Repository or UseCase (use `Result.failure` instead)
- Remediation loop ran more than 3 times without escalation
- Success criteria written as "feature works" rather than specific, testable conditions

---

## Verification

### Phase 1 → Phase 2 gate (Specify complete)
- [ ] Zero blind spots remain — all PRD ambiguities resolved
- [ ] Assumptions confirmed by human
- [ ] Every screen has a `sealed interface UiState` with all observable states including `Empty` and `Error`
- [ ] Every screen has a `sealed interface ViewIntent` for user actions
- [ ] API contract defines request, response, all error codes, and flags all ambiguous fields
- [ ] Error handling contract: `Result<T>` at all layer boundaries — confirmed in spec
- [ ] Clean Architecture layer map lists concrete class names
- [ ] Build and test commands documented and verified runnable
- [ ] Boundaries (Always / Ask first / Never) defined including anti-cheat rules
- [ ] Success criteria specific, testable, and confirmed with PM
- [ ] Spec saved to repository (`docs/specs/<feature-name>.md`)

### Phase 3 → Phase 4 gate (Contract locked)
- [ ] Gherkin scenarios cover every UiState transition
- [ ] Repository and UseCase interfaces written with no implementation
- [ ] ViewModel test stubs exist for every Gherkin scenario and currently fail
- [ ] Lock declaration committed to repository

### Phase 5 → PR gate (Implementation complete)
- [ ] All locked test stubs are now passing (no stubs deleted or modified)
- [ ] Full gate passes: `./gradlew detekt ktlintCheck :feature-module:testDebugUnitTest`
- [ ] No `@Suppress` annotations added during implementation
- [ ] All error paths use `Result<T>` — no bare `throw`, no empty `catch`
- [ ] Manual smoke test covers all UiState branches on real device or emulator
- [ ] No memory leaks in LeakCanary debug build
- [ ] `code-review-and-quality` self-review completed
- [ ] Spec file updated if any contracts changed during implementation
