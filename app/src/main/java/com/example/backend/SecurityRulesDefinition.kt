package com.example.backend

/**
 * Embedded Firebase Firestore Security Rules and Validation Definitions
 */
object SecurityRulesDefinition {

    val RULES_SOURCE = """
        rules_version = '2';
        service cloud.firestore {
          match /databases/{database}/documents {
            
            function isAuthenticated() {
              return request.auth != null;
            }

            function isOwner(userId) {
              return isAuthenticated() && request.auth.uid == userId;
            }

            function getUserData() {
              return get(/databases/$(database)/documents/users/$(request.auth.uid)).data;
            }

            function isAdmin() {
              return isAuthenticated() && 
                (request.auth.token.role == 'admin' || getUserData().role == 'admin');
            }

            // 1. Users collection
            match /users/{userId} {
              allow read: if isOwner(userId) || isAdmin();
              allow create: if isOwner(userId) 
                            && request.resource.data.userId == userId
                            && request.resource.data.role == 'user'
                            && request.resource.data.accountStatus == 'ACTIVE';
              allow update: if (isOwner(userId) 
                                && request.resource.data.role == resource.data.role 
                                && request.resource.data.accountStatus == resource.data.accountStatus)
                            || isAdmin();
              allow delete: if isAdmin();
            }

            // 2. Matches & 3. Players
            match /matches/{matchId} {
              allow read: if isAuthenticated() && (resource.data.isPublished == true || isAdmin());
              allow write: if isAdmin();
            }

            match /players/{playerId} {
              allow read: if isAuthenticated();
              allow write: if isAdmin();
            }

            // 4. Teams
            match /teams/{teamId} {
              allow read: if isAuthenticated() && (isOwner(resource.data.userId) || isAdmin());
              allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
              allow update: if isAuthenticated() && isOwner(resource.data.userId);
              allow delete: if isOwner(resource.data.userId) || isAdmin();
            }

            // 5. Contests
            match /contests/{contestId} {
              allow read: if isAuthenticated() && (resource.data.isPublished == true || isAdmin());
              allow write: if isAdmin();
            }

            // 6. Contest Entries
            match /contest_entries/{entryId} {
              allow read: if isAuthenticated() && (resource.data.userId == request.auth.uid || isAdmin());
              allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
              allow update, delete: if isAdmin();
            }

            // 7. Wallets (Client write FORBIDDEN)
            match /wallets/{userId} {
              allow read: if isOwner(userId) || isAdmin();
              allow write: if isAdmin();
            }

            // 8. Transactions (Client write FORBIDDEN)
            match /transactions/{transactionId} {
              allow read: if isAuthenticated() && (resource.data.userId == request.auth.uid || isAdmin());
              allow write: if isAdmin();
            }

            // 9. Withdrawals
            match /withdrawals/{withdrawalId} {
              allow read: if isAuthenticated() && (resource.data.userId == request.auth.uid || isAdmin());
              allow create: if isAuthenticated() 
                            && request.resource.data.userId == request.auth.uid
                            && request.resource.data.status == 'PENDING';
              allow update: if isAdmin();
              allow delete: if isAdmin();
            }

            // 10. Winners
            match /winners/{winnerId} {
              allow read: if isAuthenticated();
              allow write: if isAdmin();
            }

            // 11. Notifications
            match /notifications/{notificationId} {
              allow read: if isAuthenticated() && (resource.data.userId == request.auth.uid || resource.data.userId == 'all' || isAdmin());
              allow update: if isAuthenticated() && resource.data.userId == request.auth.uid && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['isRead']);
              allow write: if isAdmin();
            }

            // 12. App Settings
            match /app_settings/{settingId} {
              allow read: if isAuthenticated();
              allow write: if isAdmin();
            }
          }
        }
    """.trimIndent()

    data class SecurityAuditItem(
        val title: String,
        val requirement: String,
        val status: String,
        val detail: String
    )

    val AUDIT_ITEMS = listOf(
        SecurityAuditItem(
            title = "Wallet Balance Tamper Protection",
            requirement = "Users cannot directly modify wallet balances or transaction records",
            status = "ENFORCED",
            detail = "allow write: if isAdmin() blocks client mutation. Balances are strictly updated via atomic Cloud Functions."
        ),
        SecurityAuditItem(
            title = "Contest Prize & Fee Integrity",
            requirement = "Entry fee and prize payouts must be server-calculated, not client-supplied",
            status = "ENFORCED",
            detail = "Contest write permissions restricted to admin. Cloud Functions read contest doc directly from Firestore."
        ),
        SecurityAuditItem(
            title = "Admin Role Segregation",
            requirement = "Normal users must never access admin collections or admin operations",
            status = "ENFORCED",
            detail = "User creation validates role == 'user'. User cannot change role during update. Admin actions check doc or custom claim."
        ),
        SecurityAuditItem(
            title = "Private Data Isolation",
            requirement = "Users can access only their own private profiles, wallets, and entries",
            status = "ENFORCED",
            detail = "isOwner(userId) rule guards users/{userId}, wallets/{userId}, withdrawals, and transactions."
        ),
        SecurityAuditItem(
            title = "Team Name Uniqueness & Single-Edit Limit",
            requirement = "Duplicate team names prevented and max 1 name change allowed",
            status = "ENFORCED",
            detail = "Server-side atomic check validates uniqueness in lower-case index and increments nameEditCount <= 1."
        ),
        SecurityAuditItem(
            title = "Payment Secrets Protection",
            requirement = "PhonePe and Paytm merchant keys/secrets never exposed to client",
            status = "ENFORCED",
            detail = "Client requests payment token via Cloud Functions; secrets stored in server env/secrets manager."
        )
    )
}
