import { EmbedBuilder } from 'discord.js';
import { cleanMOTD, getBanner, getPlan, ServerData } from './minehut';

const DISCORD_REGEX = /(https?:\/\/)?discord\.gg\/([\d\w]+)/gi;

export function createEmbed(description: string): EmbedBuilder {
	return new EmbedBuilder().setColor('#19f4b9').setTitle(' ').setDescription(description);
}

export function toEmbed(server: ServerData): EmbedBuilder {
	let startTime: number | null = Math.floor(server.last_online / 1000);
	const creationDate = Math.floor(server.creation / 1000);

	// Don't display the max players if it's a proxy server.
	const maxPlayers = server.proxy ? undefined : server.maxPlayers || 10;

	const description = embedJoinList(
		`\`\`\`${cleanMOTD(server.motd)}\`\`\``,
		`📈 **Players:** ${server.playerCount}${maxPlayers !== undefined ? `/${maxPlayers}` : ''}`,
		`📆 **Created:** <t:${creationDate}:R>`,
		`📁 **Categories:** ${server.categories.length == 0 ? 'None' : server.categories.join(', ')}`
	);

	if (!startTime || isNaN(startTime) || new Date(startTime).getTime() == -1)
		startTime = creationDate;

	const serverPlan = getPlan(server);

	return createEmbed(
		(server.suspended ? `:warning: This server is currently suspended!\n` : '') + description
	)
		.setTitle(`${server.name} ${server.proxy ? '(Server Network)' : ''}`)
		.setImage(getBanner(server))
		.addFields(
			{
				name: 'Server Status',
				value: embedJoinList(
					`Server is ${
						server.suspended
							? '`suspended` <:no:659939343875702859>'
							: `${server.online ? '`online`' : '`offline`'} ${
									server.online ? '<:yes:659939181056753665>' : '<:no:659939343875702859>'
							  }`
					}`,
					`${server.online ? `Started` : `Last Online`} <t:${startTime}:R>`,
					`Created <t:${creationDate}:R>`
				),
				inline: true
			},
			{
				name: 'Server Plan',
				value: embedJoinList(
					`The server is using ${serverPlan === 'CUSTOM' ? 'a' : 'the'} \`${serverPlan}\` plan`,
					`Price: ${Math.round(server.credits_per_day)} credits/day`,
					`Icons Unlocked: ${server.purchased_icons.length}`
				),
				inline: true
			}
		);
}

export const embedJoinList = (...strs: string[]): string => strs.join('\n');

export function formatNumber(number: number): string {
	return number.toLocaleString('en-US', { maximumFractionDigits: 2 });
}

export function hideDiscord(body: string, replacement: string): string {
	return body.replace(DISCORD_REGEX, replacement);
}
