// A simple discord channel logger, used to report deleted messages and edited messages.

import { EmbedBuilder, Guild, TextChannel } from 'discord.js';
import { getGuildConfig } from '../utils/config';

const isEnabled = (guild: Guild) => {
	return !!getGuildConfig(guild.id).channels.logs;
};

export const log = async (guild: Guild, message: EmbedBuilder[]) => {
	if (!isEnabled(guild)) return;

	const channel = await guild.channels.fetch(getGuildConfig(guild.id).channels.logs);
	if (!channel.isTextBased) return;

	try {
		await (channel as TextChannel).send({
			embeds: message
		});
	} catch (e) {
		console.warn('Failed to post a message to the logs channel: ' + e.message);
	}
};
