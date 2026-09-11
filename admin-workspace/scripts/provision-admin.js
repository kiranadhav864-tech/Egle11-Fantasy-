/**
 * Egle11 Fantasy Cricket - Secure Initial Admin Provisioning Script
 * 
 * Usage:
 *   node scripts/provision-admin.js <ADMIN_UID> <EMAIL> <PHONE>
 * 
 * Example:
 *   node scripts/provision-admin.js usr_admin_001 admin@egle11.com +919876543210
 * 
 * Security:
 * - Runs exclusively in trusted server environment.
 * - Uses Firebase Admin SDK service account key.
 * - Never bundle this script or service account credentials into the client APK!
 */

const admin = require("firebase-admin");
const path = require("path");

const serviceAccountPath = process.env.GOOGLE_APPLICATION_CREDENTIALS || 
  path.join(__dirname, "..", "serviceAccountKey.json");

try {
  const serviceAccount = require(serviceAccountPath);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: process.env.FIREBASE_PROJECT_ID || serviceAccount.project_id
  });
} catch (err) {
  // If service account file not found, try default application credentials
  try {
    admin.initializeApp();
  } catch (initErr) {
    console.error("❌ Failed to initialize Firebase Admin SDK:", initErr.message);
    console.error("👉 Please set GOOGLE_APPLICATION_CREDENTIALS pointing to your Firebase Service Account JSON file.");
    process.exit(1);
  }
}

const db = admin.firestore();
const auth = admin.auth();

async function provisionAdmin() {
  const args = process.argv.slice(2);
  const adminUid = args[0] || process.env.FIRST_ADMIN_UID;
  const adminEmail = args[1] || process.env.FIRST_ADMIN_EMAIL || "admin@egle11.com";
  const adminPhone = args[2] || process.env.FIRST_ADMIN_PHONE || "+919876543210";

  if (!adminUid) {
    console.error("❌ Error: Target Admin UID is required.");
    console.log("Usage: node scripts/provision-admin.js <ADMIN_UID> [EMAIL] [PHONE]");
    process.exit(1);
  }

  console.log(`\n🚀 Initializing First Admin Provisioning for UID: ${adminUid}...`);

  try {
    // 1. Set Custom User Claims on Firebase Auth
    await auth.setCustomUserClaims(adminUid, {
      admin: true,
      role: "admin",
      tier: "SUPER_ADMIN"
    });
    console.log(`✅ Firebase Auth Custom Claims set: { admin: true, role: 'admin' }`);

    // 2. Add entry to protected admin_users collection
    await db.collection("admin_users").doc(adminUid).set({
      adminId: adminUid,
      email: adminEmail,
      phone: adminPhone,
      role: "SUPER_ADMIN",
      isActive: true,
      permissions: [
        "MATCH_MANAGEMENT",
        "CONTEST_MANAGEMENT",
        "WITHDRAWAL_APPROVAL",
        "WALLET_ADJUSTMENT",
        "PRIZE_SETTLEMENT",
        "SETTINGS_CONFIG",
        "USER_BLOCKING"
      ],
      provisionedAt: admin.firestore.FieldValue.serverTimestamp(),
      provisionedVia: "CLI_BOOTSTRAP"
    }, { merge: true });
    console.log(`✅ Firestore record created in protected 'admin_users' collection`);

    // 3. Upsert user document
    await db.collection("users").doc(adminUid).set({
      userId: adminUid,
      name: "Egle11 Super Administrator",
      email: adminEmail,
      mobileNumber: adminPhone,
      role: "admin",
      accountStatus: "ACTIVE",
      isAge18Plus: true,
      selectedState: "Maharashtra",
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
    console.log(`✅ User profile updated with role = 'admin'`);

    console.log(`\n🎉 Admin provisioning COMPLETE! User '${adminUid}' can now access the Admin Console with full privileges.\n`);
  } catch (error) {
    console.error("❌ Provisioning failed:", error);
    process.exit(1);
  }
}

provisionAdmin();
