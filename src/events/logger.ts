import { ArgsOf, Discord, On } from 'discordx';
import { client } from '..';
import { EmbedBuilder, TextChannel } from 'discord.js';
import { embedJoinList } from '../utils/embed';
import * as logger from '../services/logger';

@Discord()
class LoggerEvent {
	@On({ event: 'messageDelete' })
	async onMessageDelete([message]: ArgsOf<'messageDelete'>) {
		if (message.author.id != client.user.id) return;
		if (message.embeds.length < 1 || !message.embeds[0]) return;
		const channel: TextChannel = (await message.guild.channels.fetch(
			message.channelId
		)) as TextChannel;

		// Recreate the embeds
		let embeds = message.embeds.map((embed) => {
			return EmbedBuilder.from(embed).setColor('#ff0000');
		});

		// See if we can fetch the actual author
		let author = message.mentions.users.first() || null;
		const executor = message.interaction?.user;
		if (!author && executor) author = executor;
		if (!author) author = message.author;

		embeds[0].setAuthor({
			iconURL: author.displayAvatarURL(),
			name: `Bot Message Deleted in #${channel.name}`
		});

		// Embed some extra data at the bottom
		embeds[0].setFooter({
			text: embedJoinList(
				`Created By: ${author.username} (${author.id})`,
				`Message ID: ${message.id}`,
				`Channel ID: ${message.channelId}`
			)
		});

		logger.log(message.guild, embeds);
	}
}
