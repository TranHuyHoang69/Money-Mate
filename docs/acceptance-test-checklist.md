# Acceptance Test Checklist

Use this checklist before demo or graduation defense.

## Authentication And User Data

- Register or sign in with Google/email.
- Sign out and verify private data is no longer shown.
- Sign in with another account and verify data is isolated.

## Transactions

- Add an income transaction.
- Add an expense transaction.
- Edit amount, category, date, and note.
- Delete a transaction from home/detail screen.
- Open a transaction from home, detail list, and statistics.

## Categories

- Add a custom income category.
- Add a custom expense category.
- Edit category title/color/icon.
- Delete a custom category and verify transaction screens still work.

## Budgets

- Create a budget for one category in the current month.
- Edit the same category budget and verify it updates instead of duplicating.
- Delete a budget and confirm it disappears locally and after app restart.
- Add expenses over the budget and verify notification appears.

## Reminders

- Create a one-time reminder.
- Create daily, weekly, monthly reminders.
- Toggle reminder on/off.
- Edit reminder title/note/time/repeat.
- Reboot device or emulator and verify active reminders are rescheduled.

## Recurring Transactions

- Create daily recurring expense and verify next run date.
- Create monthly recurring income and verify next run date.
- Toggle recurring transaction off and verify it no longer generates expenses.
- Edit amount/category/note and verify the edit screen pre-fills data.
- Delete recurring transaction and verify it does not reappear after sync.
- Navigate from recurring screen to home repeatedly and verify no duplicate transaction is created.

## Statistics

- Filter by current month and previous month.
- Tap a category with one transaction and verify detail opens directly.
- Tap a category with multiple transactions and verify transaction list opens.
- Verify totals match transaction list.

## Offline And Sync

- Turn off network and add local data where supported.
- Turn network on and verify Firestore sync.
- Delete data then wait for sync and verify deleted data does not reappear.

## Security

- Enable PIN.
- Change PIN.
- Delete PIN.
- Enable biometric unlock where device supports it.
- Force close and reopen app to verify lock screen behavior.
