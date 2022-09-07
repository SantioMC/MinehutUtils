import { EmbedBuilder } from 'discord.js';
import { cleanMOTD, getBanner, getPlan, ServerData } from './minehut';

const DISCORD_REGEX = /(https?:\/\/)?discord\.gg\/([\d\w]+)/gi;

export function createEmbed(description: string): EmbedBuilder {
	return new EmbedBuilder().setColor('#19f4b9').setTitle(' ').setDescription(description);
}

export function toEmbed(server: ServerData): EmbedBuilder {
	var startTime: number | null = Math.floor(server.last_online / 1000);
	const creationDate = Math.floor(server.creation / 1000);

	const maxPlayers = server.proxy ? '∞' : server.maxPlayers || 10;
	const description =
		`\`\`\`${cleanMOTD(server.motd)}\`\`\`` +
		`\n📈 **Players:** ${server.playerCount}/${maxPlayers}` +
		`\n📆 **Created:** <t:${creationDate}:R>` +
		`\n📁 **Categories:** ${server.categories.length == 0 ? 'None' : server.categories.join(', ')}`;

	if (!startTime || isNaN(startTime) || new Date(startTime).getTime() == -1)
		startTime = creationDate;

	return createEmbed(
		(server.suspended ? `:warning: This server is currently suspended!\n` : '') + description
	)
		.setTitle(`${server.name} ${server.proxy ? '(Server Network)' : ''}`)
		.setImage(getBanner(server))
		.addFields(
			{
				name: 'Server Status',
				value:
					`Server is \`${server.online ? 'online' : 'offline'}\` ${
						server.online ? '<:yes:659939181056753665>' : '<:no:659939343875702859>'
					}` +
					`\n${server.online ? `Started On:` : `Last Online:`} <t:${startTime}:R>` +
					`\nCreated At: <t:${creationDate}:R>`,
				inline: true
			},
			{
				name: 'Server Plan',
				value:
					`The server is using the \`${getPlan(server)} plan\`` +
					`\nPrice: ${Math.round(server.credits_per_day)} credits/day` +
					`\nIcons Unlocked: ${server.purchased_icons.length}`,
				inline: true
			}
		);
}

export function formatNumber(number: number): string {
	return number.toLocaleString('en-US', { maximumFractionDigits: 2 });
}

export function hideDiscord(body: string, replacement: string): string {
	return body.replace(DISCORD_REGEX, replacement);
}
