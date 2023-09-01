-- CreateTable
CREATE TABLE "cooldowns" (
    "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    "key" TEXT NOT NULL,
    "delay" INTEGER NOT NULL,
    "started" BIGINT NOT NULL
);

-- CreateIndex
CREATE UNIQUE INDEX "cooldowns_key_key" ON "cooldowns"("key");
