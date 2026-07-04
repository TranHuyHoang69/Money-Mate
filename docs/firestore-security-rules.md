# Firestore Security Rules

MoneyMate stores user-owned data under:

```text
users/{userId}/{collection}/{documentId}
```

The active rule set enforces:

- Only authenticated users can access Firestore app data.
- A user can read/write only documents under `users/{auth.uid}`.
- If a document contains `userId`, it must match `request.auth.uid`.
- All documents outside the `users/{userId}` tree are denied.

Covered collections:

- `expenses`
- `categories`
- `reminders`
- `budgets`
- `recurringTransactions`
- `security`
- `settings`

Deploy command:

```bash
firebase deploy --only firestore:rules
```
