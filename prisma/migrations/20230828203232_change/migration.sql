/*
  Warnings:

  - You are about to drop the column `guild` on the `UserCooldown` table. All the data in the column will be lost.
  - Added the required column `channel` to the `UserCooldown` table without a default value. This is not possible if the table is not empty.

*/
-- RedefineTables
PRAGMA foreign_keys=OFF;
CREATE TABLE "new_UserCooldown" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "channel" TEXT NOT NULL,
    "user" TEXT NOT NULL,
    "time" INTEGER NOT NULL
);
INSERT INTO "new_UserCooldown" ("id", "time", "user") SELECT "id", "time", "user" FROM "UserCooldown";
DROP TABLE "UserCooldown";
ALTER TABLE "new_UserCooldown" RENAME TO "UserCooldown";
PRAGMA foreign_key_check;
PRAGMA foreign_keys=ON;
