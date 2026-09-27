# OfflinePay — Build Progress Tracker

## What "MVP" means for this project
MVP = Minimum Viable Product — the smallest version of the app that actually works and proves the core idea: a user can send money online, send money while "offline" (queued locally, higher fee), and have it sync and apply correctly once back online. Nothing extra until this works.

---

## PHASE 1: MVP for Monday demo (Thu → Mon)

### Thursday (today)
- [ ] `Account` entity (id, name, balance)
- [ ] `Transaction` entity (sender, receiver, amount, fee, status: CONFIRMED / PENDING_SYNC / FAILED)
- [ ] `AccountRepository` and `TransactionRepository`
- [ ] Basic service to create an account and check balance
- [ ] Feature 1: Online transfer — deduct sender, credit receiver, apply normal fee
- [ ] Confirm you can create 2 accounts and transfer money between them

### Friday
- [ ] Feature 2: Offline mode toggle
- [ ] When "offline," transaction saves as PENDING_SYNC with higher fee, balances not yet updated
- [ ] Confirm you can simulate "going offline" and see a queued transaction

### Saturday
- [ ] Feature 3: Sync service
- [ ] Method that finds all PENDING_SYNC transactions and applies them to the ledger
- [ ] Confirm flipping back "online" + running sync actually applies queued transactions

### Sunday
- [ ] Feature 4: Balance validation at sync time
- [ ] Re-check sender's real balance before applying a queued transaction; mark FAILED if insufficient
- [ ] Run through the full flow end-to-end at least 3 times
- [ ] Practice explaining the flow out loud for the demo

### Monday — DEMO DAY
- [ ] Show a normal online transfer
- [ ] Show an offline transfer being queued
- [ ] Show sync applying it once back online
- [ ] Show a failed sync due to insufficient balance (bonus — shows depth)

---

## PHASE 2: After the demo (remaining days of the 2-week window)

- [ ] Write basic unit tests for transfer + sync logic
- [ ] Write a `Dockerfile` for the app
- [ ] Write `docker-compose.yml` running 2–3 instances (simulating separate offline "agents")
- [ ] Demo: stop one container's network, queue transactions, bring it back, trigger sync
- [ ] (Stretch, only if ahead of schedule) Deploy the same setup with Minikube/Kubernetes instead of Compose
- [ ] Deploy to Render/Railway
- [ ] Finalize README
- [ ] Clean up commit history
- [ ] Final dry run of full defense explanation

---

## The one hard rule
By Monday, Features 1–4 must work. That's the actual MVP. Docker, Kubernetes, and deployment (Phase 2) are what make it impressive — not what makes it pass.
