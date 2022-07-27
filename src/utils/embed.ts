import { EmbedBuilder } from 'discord.js';
import { cleanMOTD, getPlan, ServerData } from './minehut';

export function createEmbed(description: string): EmbedBuilder {
	return new EmbedBuilder().setColor('#19f4b9').setTitle(' ').setDescription(description);
}

export function toEmbed(server: ServerData): EmbedBuilder {
	const startTime = Math.floor(server.last_online / 1000);
	const creationDate = Math.floor(server.creation / 1000);

	const maxPlayers = server.proxy ? '∞' : server.maxPlayers || 10;
	const description =
		`\`\`\`${cleanMOTD(server.motd)}\`\`\`` +
		`\n📈 **Players:** ${server.playerCount}/${maxPlayers}` +
		`\n📆 **Created:** <t:${creationDate}:R>`;

	return createEmbed(
		(server.suspended ? `:warning: This server is currently suspended!\n` : '') + description
	)
		.setTitle(`${server.name} ${server.proxy ? '(Server Network)' : ''}`)
		.addFields(
			{
				name: 'Server Status',
				value:
					`Server is \`${server.online ? 'online' : 'offline'}\` ${server.online ? '✅' : '❌'}` +
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
