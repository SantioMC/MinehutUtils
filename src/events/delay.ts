import { ArgsOf, Discord, On } from 'discordx';
import { prisma } from '..';
import { createEmbed } from '../utils/embed';
import { Message } from 'discord.js';
import ms from 'ms';

// Only send one message per user every few seconds
const alertCooldown: string[] = [];

@Discord()
class DelayListener {
	@On({ event: 'messageCreate' })
	async onMessage([message]: ArgsOf<'messageCreate'>) {
		if (message.author.bot || message.author.system || !message.guild) return;

		// Get channel data, or stop if there is none.
		const channelData = await prisma.channels.findFirst({
			where: { channel: message.channelId }
		});
		if (!channelData || channelData.delay == 0) return;

		// Get the user's data
		const channelDelay = BigInt(channelData.delay);
		const userData = await prisma.users.findFirst({
			where: { user: message.author.id, channel: message.channelId }
		});

		// If none exist, create one and stop
		if (!userData) {
			return await prisma.users.create({
				data: {
					user: message.author.id,
					channel: message.channelId,
					time: Date.now()
				}
			});
			// If user exists but the cooldown is over, update the time and stop
		} else if (userData.time + channelDelay <= Date.now()) {
			return await prisma.users.update({
				where: { id: userData.id },
				data: { time: Date.now() }
			});
		}

		// User is on cooldown, delete their message
		const channel = await message.channel.fetch();
		const author = message.author;
		await safelyDelete(message);

		if (alertCooldown.includes(message.author.id)) return;

		// Do some math to get the remaining time
		const ends = userData.time + channelDelay;
		const now_bigint = BigInt(Date.now());
		const remaining_ms = Math.max(0, Number(ends - now_bigint));
		const remaining = remaining_ms <= 0 ? 'Less than a second' : ms(remaining_ms);

		// Create the embed
		const body =
			`:stopwatch: You are currently on cooldown!\n` +
			`\n` +
			`Time Remaining: \`${remaining}\`\n` +
			`Channel Cooldown: \`${ms(channelData.delay, { long: true })}\``;

		// Send a message to the user
		alertCooldown.push(message.author.id);

		channel
			.send({
				content: `<@${author.id}>`,
				embeds: [createEmbed(body).setColor('#ff0000')]
			})
			.then((m: Message) => {
				setTimeout(() => {
					safelyDelete(m);
					alertCooldown.splice(alertCooldown.indexOf(message.author.id), 1);
				}, 5000);
			});
	}
}

const safelyDelete = async (message: Message) => {
	try {
		if (message.deletable) await message.delete();
	} catch (_) {}
};
