-- CreateTable
CREATE TABLE "ChannelCooldown" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "guild" TEXT NOT NULL,
    "channel" TEXT NOT NULL,
    "delay" INTEGER NOT NULL
);

-- CreateTable
CREATE TABLE "UserCooldown" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "guild" TEXT NOT NULL,
    "user" TEXT NOT NULL,
    "time" INTEGER NOT NULL
);

-- CreateIndex
CREATE UNIQUE INDEX "ChannelCooldown_channel_key" ON "ChannelCooldown"("channel");
