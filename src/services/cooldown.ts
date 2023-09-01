import { Guild } from 'discord.js';
import { prisma } from '..';

type key = string;
export const generateKey = (guild: Guild, ...scopes: string[]) => `${guild.id}-${scopes.join('-')}`;

export const setPersistentCooldown = async (key: key, cooldown: number) => {
	await prisma.cooldowns.upsert({
		where: {
			key
		},
		update: {
			started: Date.now(),
			delay: cooldown
		},
		create: {
			key,
			started: Date.now(),
			delay: cooldown
		}
	});
};

export const getCooldown = async (key: key): Promise<number | null> => {
	const cooldown = await prisma.cooldowns.findUnique({
		where: {
			key
		}
	});

	if (cooldown == null) return null;

	const elapsed = Number(BigInt(Date.now()) - cooldown.started);
	const remaining = cooldown.delay - elapsed;

	if (remaining <= 0) return null;
	return remaining;
};

export const clearCooldown = async (key: key) => {
	await prisma.cooldowns.delete({
		where: {
			key
		}
	});
};

export const isOnCooldown = async (key: string): Promise<boolean> => {
	const cooldown = await getCooldown(key);
	return cooldown != null;
};
