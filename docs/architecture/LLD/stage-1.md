# Stage 1 Low-Level Design: Single-Process Engine

## 1. Design Intent

This document refines the [Stage 1 HLD](../HLD/stage-1.md) into a set of
implementation-level responsibilities and data relationships.

The design is intentionally limited to an in-memory, single-process engine.
It defines what the first implementation needs without prematurely designing
database schemas, network messages, or independent worker processes.

## 2. Domain Model

### 2.1 Job

A `Job` represents one requested workflow.

| Field | Purpose |
|---|---|
| `id` | Uniquely identifies the job. |
| `state` | Represents the job lifecycle. |
| `createdAt` | Records when the job was created. |
| `startedAt` | Records when execution began. |
| `completedAt` | Records when execution reached a terminal state. |
| `tasks` | Holds the tasks belonging to the job. |
| `failureReason` | Describes why the job failed, when applicable. |

The `Job` owns its task collection and exposes state-changing operations
rather than allowing arbitrary external state mutation.

### 2.2 Task

A `Task` represents one executable unit of a job.

| Field | Purpose |
|---|---|
| `id` | Uniquely identifies the task within the job. |
| `jobId` | Identifies the parent job. |
| `state` | Represents the task lifecycle. |
| `dependencies` | References tasks that must complete first. |
| `priority` | Determines ordering among runnable tasks. |
| `input` | Contains the task's typed workload input. |
| `result` | Contains the typed output after successful execution. |
| `attemptCount` | Counts execution attempts for the task. |
| `failureReason` | Describes the most recent failure, when applicable. |

The `Task` does not know how it is scheduled or which thread executes it. It
only represents task data and valid state changes.

### 2.3 Job and Task States

```text
Job:
  CREATED -> RUNNING -> COMPLETED
                   \-> FAILED
  CREATED -> CANCELLED
  RUNNING -> CANCELLED

Task:
  PENDING -> RUNNING -> COMPLETED
                   \-> FAILED
  FAILED -> PENDING       retry
  PENDING -> CANCELLED
  RUNNING -> CANCELLED
```

Terminal states are `COMPLETED`, `FAILED`, and `CANCELLED`. A task may return
from `FAILED` to `PENDING` only when another attempt is allowed.

## 3. Generic Workload Types

The execution engine should not depend on CSV-specific types. The workload
boundary is represented conceptually as:

```text
WorkloadHandler<I, O>
  process(I input) -> O
```

Where:

- `I` is the task input type.
- `O` is the task result type.
- The handler contains workload-specific business logic.
- Scheduling, dependency handling, and state transitions remain outside the
  handler.

The first CSV implementation can provide a concrete handler without changing
the execution components.

## 4. Component Design

### 4.1 Job Manager

Responsibilities:

- Create a `Job`.
- Coordinate task planning and graph validation.
- Start execution.
- Request cancellation.
- Determine the final job state.

The Job Manager coordinates the workflow; it does not execute workload logic
directly.

### 4.2 Task Planner

Responsibilities:

- Convert a job input into `Task` objects.
- Assign task identifiers.
- Assign task dependencies.
- Assign initial priorities.
- Return the collection of tasks for graph construction.

The planner creates task definitions but does not choose execution order.

### 4.3 Task Graph

The graph stores relationships between task identifiers.

Conceptually:

```text
TaskGraph
  taskId -> dependent task IDs
  taskId -> prerequisite task IDs
```

Responsibilities:

- Store dependency relationships.
- Find initial tasks with no prerequisites.
- Find dependents after a task completes.
- Determine whether a task's prerequisites are all completed.
- Support cycle detection through graph traversal.

The graph does not decide which eligible task runs next. That is the queue's
responsibility.

### 4.4 DAG Validator

Responsibilities:

- Verify that every dependency references an existing task.
- Detect cycles.
- Reject invalid graphs before execution begins.

Validation should complete before any task is submitted to the executor.

### 4.5 In-Memory Priority Queue

The queue contains only runnable tasks.

Responsibilities:

- Insert tasks that are eligible to run.
- Remove the highest-priority runnable task.
- Preserve the configured tie-breaking rule.
- Provide safe access when multiple worker threads are active.

The queue must not be used to determine whether a task's dependencies are
complete. That decision belongs to the task graph and task state.

### 4.6 Scheduler

Responsibilities:

- Take tasks from the ready queue.
- Transition a selected task from `PENDING` to `RUNNING`.
- Submit the task to the worker thread pool.
- Ensure that one task is not submitted twice concurrently.
- Process task completion or failure.
- Ask the graph which dependent tasks have become runnable.
- Add newly runnable tasks to the queue.

### 4.7 Worker Thread Pool

The worker thread pool is the Stage 1 representation of workers. It is an
`N`-thread execution facility within the same process.

Each submitted task execution:

1. Invokes the workload handler with the task input.
2. Returns a result on success.
3. Reports a failure on handler error.
4. Updates task state through the scheduler or job coordination layer.

Worker threads should not directly mutate unrelated jobs or bypass task state
transition rules.

### 4.8 Result Aggregator

Responsibilities:

- Collect successful task results.
- Associate each result with its task identifier.
- Produce the final job result when the job's required tasks complete.
- Ensure aggregation is safe when results arrive concurrently.

The aggregation strategy is workload-specific, but the aggregation boundary
belongs to the engine.

### 4.9 In-Memory State

The initial state store is an in-memory collection indexed by identifiers:

```text
jobId  -> Job
taskId -> Task
```

It is the authoritative state for the lifetime of the process. It is not
durable and is intentionally lost after process termination.

## 5. Main Execution Sequence

```text
Job Manager
  -> Task Planner
  -> DAG Validator
  -> Task Graph
  -> Ready Queue
  -> Scheduler
  -> Worker Thread Pool
  -> Workload Handler
  -> Task State
  -> Task Graph
  -> Ready Queue
  -> Result Aggregator
```

Detailed sequence:

1. The Job Manager creates a job in `CREATED`.
2. The Task Planner creates the job's tasks.
3. The Task Graph is built from task dependencies.
4. The DAG Validator validates the graph.
5. The job changes to `RUNNING`.
6. Initial tasks with no prerequisites enter the ready queue.
7. The Scheduler removes a task and changes it to `RUNNING`.
8. The Scheduler submits the task to the thread pool.
9. The handler processes the task input.
10. On success, the task stores its result and becomes `COMPLETED`.
11. On failure, the attempt count increases and the retry decision is applied.
12. Completed tasks allow eligible dependents to enter the ready queue.
13. The Result Aggregator records successful results.
14. The Job Manager marks the job `COMPLETED` when all required tasks finish.

## 6. Retry Design

Phase 1 uses basic retry tracking:

- `attemptCount` belongs to the task.
- A retry limit belongs to the execution configuration.
- A failed task may return to `PENDING` when the limit is not reached.
- A task becomes terminally `FAILED` when no attempt remains.
- The failure reason from the most recent failed attempt is retained.

Retry policy classification, exponential backoff, jitter, leases, and
fencing tokens are deferred to later stages.

## 7. Concurrency Invariants

The implementation must preserve these invariants:

1. A task is submitted only when it is `PENDING`.
2. A task is submitted at most once while it is `RUNNING`.
3. A task becomes runnable only after all prerequisites are `COMPLETED`.
4. A task result is stored only for the corresponding task.
5. A task's `attemptCount` is updated once per execution attempt.
6. A job becomes `COMPLETED` only after all required tasks complete.
7. A cancelled job does not submit new pending tasks.
8. Graph updates, task state updates, and queue insertion cannot expose an
   invalid intermediate state to another worker thread.

These invariants are more important than a particular locking mechanism. The
implementation should choose the simplest mechanism that preserves them.

## 8. Data Structure Responsibilities

| Structure | Responsibility |
|---|---|
| Map keyed by job ID | Find jobs in memory. |
| Map keyed by task ID | Find tasks and update task state. |
| Adjacency lists | Represent task dependencies and dependents. |
| In-degree or prerequisite counts | Identify when a task becomes runnable. |
| Priority queue | Order runnable tasks. |
| Thread pool | Execute tasks concurrently. |
| Result map | Associate task IDs with successful results. |

The LLD does not yet prescribe the exact Java collection for every structure.
That choice should be made while studying the relevant data structures and
thread-safety guarantees.

## 9. Error and Boundary Conditions

The implementation should define behaviour for:

- Duplicate job or task identifiers.
- A dependency referring to a missing task.
- A cyclic dependency graph.
- A job with no tasks.
- A task handler throwing an exception.
- A task reaching the retry limit.
- A task being cancelled before execution.
- A cancellation request while a task is running.
- A result arriving after the job has become terminal.
- A queue being empty while tasks remain incomplete.

Each condition should have a test before it is considered supported.

## 10. LLD-to-Test Mapping

| Design area | Required test evidence |
|---|---|
| Job and task state | Valid and invalid state transitions. |
| DAG validation | Valid graph, missing dependency, and cycle rejection. |
| Runnable detection | Only dependency-free or fully-unblocked tasks enter the queue. |
| Priority queue | Priority ordering and deterministic tie-breaking. |
| Scheduler | No duplicate concurrent submission. |
| Thread pool | Multiple tasks execute and complete correctly. |
| Retry tracking | Attempt count and retry limit behave correctly. |
| Result aggregation | Parallel result matches the sequential result. |
| Cancellation | Pending work is not newly scheduled after cancellation. |
| Shared state | Concurrent execution preserves the listed invariants. |

## 11. Deliberately Deferred Decisions

The following are not decided by this LLD:

- Java package layout.
- Exact class versus record choices.
- Exact exception hierarchy.
- Exact queue implementation.
- Retry backoff and retryable-error classification.
- Durable persistence schema.
- Network protocol and worker-process API.
- PostgreSQL and RabbitMQ integration.