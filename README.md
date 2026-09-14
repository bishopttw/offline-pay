# OfflinePay — An Offline-First Banking Transaction System

## Overview
OfflinePay is a simulated banking platform built to solve a real-world problem: people without a stable mobile data connection are locked out of digital banking. This project demonstrates an **offline-first architecture**, where a user can still initiate transactions with no internet connection. These transactions are queued locally (with a higher fee applied) and automatically synced to the central ledger once connectivity returns.

This project is built as part of the MMS4 Semester Project at NIIT, and is designed to directly apply the semester's focus areas — **Docker and Kubernetes** — to a real architectural problem rather than using them as an afterthought.

> **Note:** This project simulates "offline" behavior for demonstration purposes. It does not integrate with real telecom infrastructure (e.g. SMS/USSD) — a toggle is used to simulate a loss of connectivity, and the app's local queuing/sync logic behaves exactly as it would in a real offline-first system.

## Problem Statement
Millions of people in low-connectivity areas are unable to access digital financial services simply because they lack a stable internet connection. OfflinePay solves this by allowing transactions to be initiated offline, recorded locally, and reconciled with the central system once a connection is available — similar in principle to how mobile money agents operate in areas with unreliable network access.

## Core Features (MVP)
1. **Send money online** — standard transfer between accounts at the normal transaction fee.
2. **Send money offline** — a transaction can still be initiated with no internet connection; a higher fee is applied.
3. **Auto-sync when back online** — offline transactions are queued locally and automatically pushed to the central system once connectivity returns.
4. **Balance check before confirming** — before an offline transaction is finalized, the system re-validates that the sender still has sufficient funds.
5. **Multi-instance simulation** — multiple containerized instances of the app (via Docker) simulate separate offline "agent" nodes, demonstrating the full offline → sync flow visually.

## Tech Stack
- **Backend:** Java, Spring Boot
- **Frontend:** Thymeleaf (server-rendered HTML/CSS)
- **Database:** H2
- **Infrastructure:** Docker, Docker Compose (Kubernetes/Minikube for multi-node orchestration, time permitting)

## Project Status
🚧 In active development —.

## Author
*(Bishop)*
