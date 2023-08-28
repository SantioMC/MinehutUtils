/*
  Warnings:

  - You are about to drop the `ChannelCooldown` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `UserCooldown` table. If the table is not empty, all the data it contains will be lost.

*/
-- DropTable
PRAGMA foreign_keys=off;
DROP TABLE "ChannelCooldown";
PRAGMA foreign_keys=on;

-- DropTable
PRAGMA foreign_keys=off;
DROP TABLE "UserCooldown";
PRAGMA foreign_keys=on;

-- CreateTable
CREATE TABLE "channels" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "guild" TEXT NOT NULL,
    "channel" TEXT NOT NULL,
    "delay" INTEGER NOT NULL
);

-- CreateTable
CREATE TABLE "users" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "channel" TEXT NOT NULL,
    "user" TEXT NOT NULL,
    "time" INTEGER NOT NULL
);

-- CreateIndex
CREATE UNIQUE INDEX "channels_channel_key" ON "channels"("channel");
