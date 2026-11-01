# Aglet

A project I wanted to build for fun.

## Index

- [Project Definition](#project-definition)
  - [1. Project Overview](#1-project-overview)
  - [2. Example Use Case](#2-example-use-case)
  - [3. Core Terminology](#3-core-terminology)
  - [4. Expected Behaviour](#4-expected-behaviour)
  - [5. Learning Goals](#5-learning-goals)
  - [6. Learning and Implementation Principle](#6-learning-and-implementation-principle)
  - [7. Chosen Language and Technology](#7-chosen-language-and-technology)
  - [8. Project Stages and Learning Resources](#8-project-stages-and-learning-resources)
  - [9. Success Criteria](#9-success-criteria)
  - [10. Non-Goals](#10-non-goals)
  - [11. Final Deliverables](#12-final-deliverables)

## Project Definition

### 1. Project Overview

A fault-tolerant distributed job-processing engine written primarily in Java.

A user submits a large piece of work called a *job*. Aglet divides the job into smaller **tasks**, distributes those tasks across multiple worker processes, tracks their execution, retries failures and combines the results.

The first supported workload will process large CSV transaction files. Document processing and AI inference will be added later as workload plug-ins.

This project has two goals:

1. Build a strong distributed-systems project with reproducible, measurable results.
2. Learn computer science fundamentals by first implementing simplified internals and then using production tools that solve the same problems.

This document defines the project and its intended behaviour. It does not define its HLD, LLD, APIs, database schema or class structure. Those will be designed separately during the learning journey.

---

### 2. Example Use Case

A user submits:

> Validate and analyse a CSV file containing 1,000,000 transaction rows.

Aglet should:

1. Accept and validate the job.
2. Divide the CSV into smaller partitions.
3. Create one task for each partition.
4. Assign tasks to available worker processes.
5. Track pending, running, completed and failed tasks.
6. Detect failed or disconnected workers.
7. Reassign unfinished tasks.
8. Store intermediate results.
9. Combine all task results.
10. Produce a final report and execution metrics.

A later AI workload could be:

> Extract invoice number, vendor, date and amount from 100,000 documents.

The document-processing code would be different from the CSV-processing code, but task scheduling, persistence, retries and failure recovery would remain unchanged.

---

### 3. Core Terminology

#### Job

A job is the complete operation requested by a user.

Examples:

- Analyse a CSV containing one million transactions.
- Validate ten million imported records.
- Extract structured fields from documents.
- Run an AI model over a document collection.

#### Task

A task is one independently executable part of a job.

Examples:

- Process CSV rows 1 to 10,000.
- Validate partition 42.
- Extract fields from documents 500 to 599.

#### Worker

A worker is a separate process that receives tasks, executes workload-specific code and reports results and heartbeats.

Workers may crash, disconnect, become slow or accidentally execute a task more than once.

#### Workload Handler

A workload handler contains the actual business logic.

Initial handlers:

- CSV parsing
- Transaction validation
- Transaction aggregation

Later handlers:

- OCR
- Document extraction
- LLM inference
- Image processing

The Aglet engine manages execution. It does not need to understand what an invoice, transaction or AI model means.

---

### 4. Expected Behaviour

Aglet must:

- Accept a job and return a unique job identifier.
- Divide jobs into smaller tasks.
- Execute tasks across one or more workers.
- Persist job and task state.
- Display pending, running, completed, failed and cancelled states.
- Detect workers that stop sending heartbeats.
- Reassign tasks whose execution lease expires.
- Retry temporary failures with exponential backoff and jitter.
- Move permanently failing tasks into a dead-letter state.
- Prevent stale workers from committing outdated results.
- Support idempotent task processing.
- Apply backpressure when the system is overloaded.
- Allow users to cancel jobs.
- Recover unfinished jobs after coordinator restart.
- Produce deterministic results matching a trusted single-process implementation.
- Expose metrics, traces and structured logs.

Aglet will provide *at-least-once task execution*.

It will not claim exactly-once execution. A task may execute more than once, but idempotency and fenced result commits must prevent incorrect final effects.

---

### 5. Learning Goals

| Subject | Concepts to learn | Application in Aglet |
|---|---|---|
| OOP | Encapsulation, interfaces, composition, SOLID and design patterns | Jobs, tasks, handlers, schedulers, retry policies and storage adapters |
| DSA | DAGs, heaps, hashing, trees, queues and graph traversal | Dependencies, priority scheduling, indexes and task ordering |
| Operating systems | Processes, threads, synchronization, files, memory and scheduling | Workers, thread pools, concurrent queues, locks and resource limits |
| Computer networks | TCP, sockets, framing, RPC, timeouts and backpressure | Coordinator-worker communication |
| DBMS | Pages, B+ trees, buffer pools, WAL, transactions, isolation and indexes | Durable task state and crash recovery |
| System design | Capacity, bottlenecks, caching, queues and failure domains | Designing the complete system |
| Distributed systems | Leases, fencing, idempotency, consistency, replication and consensus | Reliable task execution across machines |
| HLD | Components, boundaries, communication and scaling | Overall architecture |
| LLD | Classes, interfaces, state machines and patterns | Internal code structure |
| Observability | Metrics, logs, traces, SLOs and alerting | Understanding system behaviour |
| Evaluation | Workloads, baselines, latency percentiles and failure tests | Producing honest, reproducible results |

---

### 6. Learning and Implementation Principle

The project will follow this sequence:

> Learn the concept → implement a simplified version → test it → use the production tool → compare both approaches.

Examples:

- Implement an in-memory priority queue before using RabbitMQ.
- Implement a small append-only log before using PostgreSQL WAL-backed persistence.
- Implement a small B+ tree before relying on PostgreSQL indexes.
- Implement a simple TCP client and server before using gRPC.
- Implement basic retry and heartbeat logic manually before using framework abstractions.
- Build a Raft simulator before considering coordinator replication.

These internal implementations are educational. They will not be presented as replacements for PostgreSQL, RabbitMQ or established distributed-systems libraries.

---

### 7. Chosen Language and Technology

#### Primary Language

*Java 21*

Java is selected because it provides:

- Strong OOP support
- Static typing
- Threads and concurrency primitives
- Networking APIs
- Database and messaging libraries
- JVM profiling and observability
- Strong backend employment relevance

C++ may later be used for an optional high-performance worker, but it is not required for the main engine.

#### Core Tooling

- Java 21
- Maven
- JUnit 5
- Testcontainers
- JMH
- GitHub Actions

#### Production-Stage Infrastructure

- gRPC and Protocol Buffers for communication
- PostgreSQL for authoritative job and task state
- RabbitMQ for task-delivery messages
- MinIO for large inputs and results
- OpenTelemetry for instrumentation
- Prometheus for metrics
- Grafana for dashboards
- Docker Compose for local distributed deployment

Kubernetes and a complex frontend are outside the initial scope.

#### RabbitMQ’s Role

RabbitMQ will transport task-assignment messages. It will not be the source of truth.

PostgreSQL will remain authoritative for:

- Job state
- Task state
- Attempts
- Leases
- Result references
- Workflow completion

This means RabbitMQ can lose or duplicate a message without losing or corrupting the job.

---

### 8. Project Stages and Learning Resources

The listed resources are intentionally limited. Study only the relevant sections needed for the current stage.

#### Stage 1 Java OOP and Data Structures

Build:

- Job and task models
- Task state machine
- Workload-handler interface
- In-memory priority queue
- DAG validation
- Single-process execution
- Thread pool

Study:

- Classes, interfaces, records, composition and inheritance
- Heap-based priority queues
- Hash maps
- Graph traversal
- Topological sorting
- Thread safety

Resources:

- [Official Java OOP tutorials](https://dev.java/learn/language/oop/)
- [MIT 6.006 Algorithms](https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/resources/lecture-videos/) — watch only hashing, binary trees, heaps, BFS, DFS and topological sorting
- [Refactoring Guru Design Patterns](https://refactoring.guru/design-patterns) — initially read Strategy, State and Factory Method only

Deliverable:

> A single-process engine that executes a DAG of tasks using multiple threads.

---

#### Stage 2 Operating Systems and Concurrency

Build:

- Worker processes
- Thread pools
- Bounded queues
- Graceful shutdown
- Task cancellation
- Synchronization
- File-backed task journal

Study:

- Processes and threads
- Context switching
- Locks and condition variables
- Producer-consumer problem
- Deadlocks
- File systems
- I/O
- Crash recovery basics

Resources:

- [Operating Systems Three Easy Pieces](https://pages.cs.wisc.edu/~remzi/OSTEP/) — read the Process, Concurrency and Persistence introductions
- Java concurrency sections from [Dev.java](https://dev.java/learn/)

Deliverable:

> A local multi-process version that survives worker-process termination.

---

#### Stage 3 Database Internals

Build simplified versions of:

- Slotted data page
- Buffer pool
- Hash index
- B+ tree
- Append-only log
- Write-ahead log
- Checkpoint and recovery
- Transaction experiment

These may live in a separate learning-labs module.

Study:

- Disk pages
- Buffer management
- B+ trees
- WAL
- ACID
- Isolation levels
- Locks and MVCC
- Query indexes

Resources:

- [CMU 15-445 Database Systems](https://15445.courses.cs.cmu.edu/) — study storage, buffer pools, indexes, transactions, concurrency and recovery
- [PostgreSQL Transaction Isolation](https://www.postgresql.org/docs/current/transaction-iso.html)
- [PostgreSQL WAL](https://www.postgresql.org/docs/current/wal-intro.html)

Deliverable:

> A file-backed Aglet prototype plus isolated implementations of B+ tree and WAL concepts.

The production engine will later use PostgreSQL instead of the educational database.

---

#### Stage 4 Computer Networks

Build:

- Simple TCP coordinator-worker communication
- Message framing
- Request identifiers
- Timeouts
- Heartbeats
- Connection recovery
- Simulated latency and packet loss

Study:

- TCP and UDP
- Sockets
- Packet framing
- Reliability
- Flow and congestion control
- DNS
- RPC
- Timeouts
- Backpressure

Resources:

- [Beej’s Guide to Networking Concepts](https://beej.us/guide/bgnet0/)
- [Stanford CS144](https://web.stanford.edu/class/cs144/) — focus on reliability, TCP and congestion control
- [Beej’s Guide to Network Programming](https://beej.us/guide/bgnet/) — use for socket-level understanding

Deliverable:

> Separate coordinator and worker processes communicating over a manually implemented TCP protocol.

The production version will later replace this protocol with gRPC.

---

#### Stage 5 System Design HLD and LLD

Create separately:

- Functional requirements
- Non-functional requirements
- Capacity assumptions
- High-level architecture
- Component responsibilities
- Job and task state machines
- API and message contracts
- Database schema
- Class and interface design
- Failure-handling rules
- Architecture decision records

Study:

- Requirement clarification
- Capacity estimation
- Component boundaries
- Stateless and stateful services
- Queues and backpressure
- Caching
- Load balancing
- State machines
- SOLID
- Strategy and State patterns

Resources:

- Designing Data-Intensive Applications — selected chapters on data models, replication, partitioning and distributed-system failures
- [AWS Builders Library](https://aws.amazon.com/builders-library/) — read “Timeouts, retries and backoff with jitter”
- [Refactoring Guru](https://refactoring.guru/design-patterns) for the patterns actually used in the LLD

Deliverable:

> Reviewed HLD and LLD documents before production infrastructure is integrated.

---

#### Stage 6 Distributed Systems Reliability

Build:

- Worker registration
- Heartbeats
- Task leases
- Lease expiration
- Fencing tokens
- Idempotency keys
- Retry with exponential backoff and jitter
- Dead-letter handling
- Worker capability matching
- Coordinator restart recovery

Study:

- Partial failure
- Failure detectors
- At-least-once delivery
- Idempotency
- Logical clocks
- Leases
- Replication
- Consistency
- Consensus
- Split brain

Resources:

- [Martin Kleppmann’s Distributed Systems lectures](https://www.youtube.com/watch?v=UEAMfLPZZhE) — focus on RPC, failures, time, replication and consensus
- [MIT 6.5840 Distributed Systems](https://pdos.csail.mit.edu/6.5840/) — use the MapReduce and Raft material as references, not as another complete course
- [Raft paper](https://raft.github.io/raft.pdf) — read when implementing the optional simulator

Deliverable:

> A distributed prototype that recovers from worker and coordinator failures without losing workflow state.

---

#### Stage 7 Production Tools

Replace or complement educational components with:

- gRPC for communication
- PostgreSQL for durable state
- RabbitMQ for message delivery
- MinIO for object storage
- Docker Compose for deployment

Resources:

- [gRPC Java Quick Start](https://grpc.io/docs/languages/java/quickstart/)
- [RabbitMQ Java Tutorials](https://www.rabbitmq.com/tutorials/tutorial-one-java)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/current/)

Deliverable:

> A reproducible multi-service deployment running the CSV workload.

---

#### Stage 8 Scheduling and Backpressure

Implement and compare:

- FIFO scheduling
- Priority scheduling
- Least-loaded worker selection
- Capability-aware scheduling
- Fair-share scheduling
- Batched task assignment
- Queue limits
- Admission control

Study:

- Scheduling algorithms
- Fairness
- Starvation
- Queueing
- Backpressure
- Overload
- Retry storms

Resource:

- [AWS Builders Library](https://aws.amazon.com/builders-library/) — read the articles on retries, overload and dependency isolation

Deliverable:

> A benchmark comparing at least three scheduling strategies.

---

#### Stage 9 Observability and Evaluation

Instrument:

- Metrics
- Structured logs
- Distributed traces
- Queue depth
- Task duration
- Worker utilization
- Retry counts
- Failure counts
- Recovery time
- Database latency

Resources:

- [Google SRE Monitoring Distributed Systems](https://sre.google/resources/book-update/monitoring-distributed-systems/)
- [OpenTelemetry Observability Primer](https://opentelemetry.io/docs/concepts/observability-primer/)
- [Official JMH samples](https://github.com/openjdk/jmh/tree/master/jmh-samples)

Deliverable:

> A Grafana dashboard and reproducible benchmark report.

---

#### Stage 10 AI Workload Extension

Add:

- Document-processing job planner
- OCR or extraction handler
- LLM-inference handler
- Model-aware worker capabilities
- Model caching
- Request batching
- Large-result storage

The existing scheduling, retry, persistence and recovery engine should work without redesign.

Deliverable:

> Process a document collection through the same engine used for CSV jobs.

---

### 9. Success Criteria

#### Functional Correctness

Aglet must:

- Process a generated CSV containing at least 1,000,000 rows.
- Produce the same result as a trusted sequential implementation.
- Execute using one, two, four and eight workers.
- Recover jobs after coordinator restart.
- Recover tasks after worker termination.
- Reject stale results from expired workers.
- Handle duplicate task delivery safely.
- Cancel running jobs.
- Make permanently failing tasks visible.

#### Failure Testing

Test at minimum:

- Worker crash during execution
- Worker disconnection
- Delayed heartbeat
- Duplicate message
- Coordinator restart
- PostgreSQL restart
- RabbitMQ restart
- Slow worker
- Poison task
- Storage failure
- Queue overload
- Network delay and packet loss

Run repeated fault-injection tests. No task may disappear or remain silently stuck.

#### Performance Evaluation

Measure:

- Tasks completed per second
- Total job duration
- p50, p95 and p99 scheduling latency
- Scaling from one to eight workers
- Queue depth
- Worker utilization
- Database operations per task
- Failure-detection time
- Recovery time
- Duplicate-execution count
- Throughput before and after batching
- Behaviour under worker failures

Every benchmark must record:

- Hardware
- Dataset
- Task size
- Worker count
- Configuration
- Test duration
- Number of repetitions

The project succeeds by producing correct and reproducible measurements. Resume numbers will be written only after these benchmarks exist.

#### Engineering Quality

The repository should contain:

- Unit tests
- Integration tests
- Failure tests
- Reproducible Docker deployment
- Architecture decision records
- Benchmark scripts
- Raw benchmark results
- Known limitations
- Technical demonstration
- Clean Git history showing progressive implementation

#### Learning Success

The project is successful only when I can explain:

- Why at-least-once execution was selected
- Why exactly-once execution is difficult
- How idempotency handles duplicates
- Why heartbeats cannot prove a worker is dead
- How leases and fencing tokens work
- Why PostgreSQL owns state instead of RabbitMQ
- How transactions protect task transitions
- How B+ trees and indexes improve scheduler queries
- Where backpressure is applied
- What limits horizontal scaling
- When consensus is required
- Which guarantees Aglet does and does not provide

---

### 10. Non-Goals

The initial project will not:

- Replace PostgreSQL or RabbitMQ
- Rebuild Kafka, Temporal or Ray completely
- Claim exactly-once execution
- Use production consensus in the first release
- Support multiple geographic regions
- Train large AI models
- Begin with Kubernetes
- Build a complex frontend
- Optimize for a meaningless “one million tasks” claim without defining the workload

---

### 11. Final Deliverables

- Java source repository
- CSV transaction workload
- Distributed coordinator and workers
- Educational queue, B+ tree, WAL and TCP implementations
- PostgreSQL and RabbitMQ integration
- Automated failure tests
- Prometheus and Grafana dashboard
- Benchmark harness and results
- Architecture decision records
- HLD and LLD documents created during development
- Technical report and demonstration
- Optional document and LLM-inference workload
- Resume bullets supported by measured results