import {
	CommandInteraction,
	ActionRowBuilder,
	ModalBuilder,
	TextInputBuilder,
	TextInputStyle,
	ModalSubmitInteraction,
	Channel,
	TextChannel
} from 'discord.js';
import { Discord, ModalComponent, Slash } from 'discordx';
import { clean, createEmbed, embedJoinList } from '../utils/embed';
import { getServerData } from '../utils/minehut';
import * as modbot from '../services/modbot';
const config = require('../../config.json');

@Discord()
export class AdvertiseCommand {
	@Slash({ name: 'advertise', description: 'Advertise a server on discord' })
	private async advertise(interaction: CommandInteraction) {
		const modal = new ModalBuilder()
			.setTitle('Advertise your server!')
			.setCustomId('minehut-advertise');

		const serverName = new TextInputBuilder()
			.setCustomId('serverName')
			.setLabel('Minehut Server Name')
			.setStyle(TextInputStyle.Short);

		const description = new TextInputBuilder()
			.setCustomId('description')
			.setLabel('Talk about your server!')
			.setStyle(TextInputStyle.Paragraph);

		modal.addComponents(
			new ActionRowBuilder<TextInputBuilder>().addComponents(serverName),
			new ActionRowBuilder<TextInputBuilder>().addComponents(description)
		);

		interaction.showModal(modal);
	}

	@ModalComponent({ id: 'minehut-advertise' })
	async handle(interaction: ModalSubmitInteraction) {
		const [serverName, description] = ['serverName', 'description'].map((id) =>
			interaction.fields.getTextInputValue(id)
		);

		// Verify server name
		const data = await getServerData(serverName.trim());
		if (data == null)
			return interaction.reply({
				ephemeral: true,
				embeds: [
					createEmbed(`<:no:659939343875702859> Server \`${clean(serverName)}\` could not be found`)
				]
			});

		// Verify description length
		const lines = description.split('\n').length;
		if (lines > 25)
			return interaction.reply({
				ephemeral: true,
				embeds: [
					createEmbed(
						`<:no:659939343875702859> Description is too long, please keep it to 25 lines or under.`
					).setColor('#ff0000')
				]
			});

		// Run description through modbot
		const filtered = await modbot.isFiltered(interaction.guild, description);
		if (filtered) {
			return interaction.reply({
				ephemeral: true,
				embeds: [
					createEmbed(
						`<:no:659939343875702859> Description contains blocked words. Please try again.`
					).setColor('#ff0000')
				]
			});
		}

		const body = embedJoinList(
			`<:minehut:583099471320055819> **${data.name}**`,
			``,
			description,
			``,
			`Play at \`${data.name_lower}.minehut.gg\``,
			`<:bedrock:1101261334684901456> Bedrock: \`${data.name_lower}.bedrock.minehut.gg\``,
			``,
			`*Server advertised by <@${interaction.user.id}>*`
		);

		const serverChannelId = config.channels.servers;
		const channel = (await interaction.guild.channels.fetch(serverChannelId)) as Channel;

		if (!channel.isTextBased)
			return interaction.reply({
				ephemeral: true,
				embeds: [
					createEmbed(
						`<:no:659939343875702859> Server channel is not a text channel. Please contact a moderator.`
					)
				]
			});

		const message = await (channel as TextChannel).send({
			embeds: [createEmbed(body)]
		});

		await interaction.reply({
			ephemeral: true,
			embeds: [
				createEmbed(
					`<:yes:659939344192868109> Successfully posted your server advertisement! Check it out :point_right: ${message.url}`
				)
			]
		});
	}
}
