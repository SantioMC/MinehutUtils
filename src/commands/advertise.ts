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

		const body = embedJoinList(
			`<:minehut:583099471320055819> **${data.name}**`,
			``,
			description,
			``,
			`👤 **${data.playerCount}** players currently online`,
			`Play at \`${data.name_lower}.minehut.gg\``
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

		interaction.reply({
			ephemeral: true,
			embeds: [
				createEmbed(
					`<:yes:659939181056753665> You advertised \`${clean(
						serverName
					)}\`! Check it out :point_right: https://discord.com/channels/${
						interaction.guildId
					}/${serverChannelId}/${message.id}`
				)
			]
		});
	}
}
