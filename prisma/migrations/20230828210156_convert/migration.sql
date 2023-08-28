/*
  Warnings:

  - You are about to alter the column `time` on the `users` table. The data in that column could be lost. The data in that column will be cast from `Int` to `BigInt`.

*/
-- RedefineTables
PRAGMA foreign_keys=OFF;
CREATE TABLE "new_users" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "channel" TEXT NOT NULL,
    "user" TEXT NOT NULL,
    "time" BIGINT NOT NULL
);
INSERT INTO "new_users" ("channel", "id", "time", "user") SELECT "channel", "id", "time", "user" FROM "users";
DROP TABLE "users";
ALTER TABLE "new_users" RENAME TO "users";
PRAGMA foreign_key_check;
PRAGMA foreign_keys=ON;
