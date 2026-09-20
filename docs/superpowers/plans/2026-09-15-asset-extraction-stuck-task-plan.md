# 资产提取卡住任务修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent visual-asset extraction tasks from remaining permanently in `PROCESSING`, and recover stale tasks when the user retries.

**Architecture:** Keep the existing fixed-thread-pool design. Wrap the entire claimed-task execution in failure handling, and add a Mapper operation that requeues only `PROCESSING` tasks older than a configurable timeout. `start()` will requeue and resubmit stale tasks while leaving fresh tasks untouched.

**Tech Stack:** Spring Boot 2.6, Java 17, MyBatis annotations, JUnit 5, Mockito, MySQL.

## Global Constraints

- Do not alter the existing asset extraction AI prompt or asset data model.
- Do not expose credentials in logs or tests.
- Default stale-task timeout is 10 minutes.
- Preserve existing `COMPLETED`, `PARTIAL_FAILED`, and `FAILED` behavior.

### Task 1: Regression tests for task recovery and failure handling

**Files:**
- Modify: `novel/src/test/java/com/novelgeneration/novel/service/impl/AssetExtractionServiceImplTest.java`

- [ ] **Step 1: Add a test that a pre-processing update failure marks the task failed.** Mock `claimTask` and `selectTask`, make the first `updateTask` throw, call `execute`, and assert a later `updateTask` call contains status `FAILED`.
- [ ] **Step 2: Add a test that a stale `PROCESSING` task is reset and submitted.** Set an old `updateTime`, mock `resetStaleTask` to return `1`, call `start`, and verify reset plus asynchronous `claimTask` occur.
- [ ] **Step 3: Add a test that a fresh `PROCESSING` task is not resubmitted.** Set a recent `updateTime`, call `start`, and verify `resetStaleTask` is not called.
- [ ] **Step 4: Run the focused tests and confirm the new tests fail before production changes.**

### Task 2: Implement robust service state transitions

**Files:**
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/AssetExtractionServiceImpl.java`

- [ ] **Step 1: Add a configurable timeout property with a 10-minute default.**
- [ ] **Step 2: In `start`, detect stale `PROCESSING` tasks using `updateTime`, reset them through the Mapper, and submit them for execution.**
- [ ] **Step 3: Move `getStatus`, the initial progress update, and all execution logic inside one `try-catch`.**
- [ ] **Step 4: On any claimed-task exception, reload the task when possible and update it to `FAILED` with an error message.**
- [ ] **Step 5: Run focused tests and confirm they pass.**

### Task 3: Add Mapper support for stale task recovery

**Files:**
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/VisualAssetMapper.java`

- [ ] **Step 1: Add `resetStaleTask(String taskId, int timeoutSeconds)` using `TIMESTAMPDIFF(SECOND, update_time, NOW())`.**
- [ ] **Step 2: Run Mapper parameter tests and the focused service tests.**

### Task 4: Verify the live orphan task

**Files:**
- No production file changes.

- [ ] **Step 1: Compile/test with available Maven dependencies; if dependency resolution is unavailable, record that limitation.**
- [ ] **Step 2: Query task `fb8b35cc-801a-4c7e-a982-15895a3b8cd1` and verify it no longer remains indefinitely in `PROCESSING` after a retry.**
- [ ] **Step 3: Run `git diff --check` and review only intended changes.**
