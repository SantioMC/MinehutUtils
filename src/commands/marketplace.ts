import {
	CommandInteraction,
	ActionRowBuilder,
	ModalBuilder,
	TextInputBuilder,
	TextInputStyle,
	ModalSubmitInteraction,
	Channel,
	TextChannel,
	StringSelectMenuBuilder,
	StringSelectMenuInteraction,
	StringSelectMenuComponent
} from 'discord.js';
import { Discord, SelectMenuComponent, ModalComponent, Slash } from 'discordx';
import { createEmbed, embedJoinList } from '../utils/embed';
import * as modbot from '../services/modbot';
import * as cooldown from '../services/cooldown';
import ms from 'ms';
import { config } from '..';
import { getGuildConfig } from '../utils/config';

const ongoingInteractions: Map<string, CommandInteraction> = new Map();

@Discord()
export class MarketplaceCommand {
	@Slash({ name: 'marketplace', description: 'Request or offer services' })
	private async advertise(interaction: CommandInteraction) {
		if (ongoingInteractions.has(interaction.user.id)) {
			// Close the previous interaction
			const prevInteraction = ongoingInteractions.get(interaction.user.id);
			prevInteraction.editReply({
				content: 'This interaction has been closed',
				components: []
			});
		}

		const menu: StringSelectMenuBuilder = new StringSelectMenuBuilder()
			.addOptions([
				{
					label: 'Requesting',
					value: 'request',
					emoji: '💭'
				},
				{
					label: 'Offering',
					value: 'offer',
					emoji: '📢'
				}
			])
			.setMaxValues(1)
			.setMinValues(1)
			.setPlaceholder('Select one')
			.setCustomId('minehut-marketplace-service');

		ongoingInteractions.set(interaction.user.id, interaction);
		await interaction.reply({
			content: 'Are you looking to offer or request?',
			components: [new ActionRowBuilder<StringSelectMenuBuilder>().addComponents(menu)],
			ephemeral: true
		});

		// auto close after a minute
		setTimeout(() => {
			if (ongoingInteractions.has(interaction.user.id)) {
				const prevInteraction = ongoingInteractions.get(interaction.user.id);
				if (prevInteraction != interaction) return;

				prevInteraction.editReply({
					content: 'This interaction has been automatically closed',
					components: []
				});

				ongoingInteractions.delete(interaction.user.id);
			}
		}, 5 * 60 * 1000);
	}

	@SelectMenuComponent({ id: 'minehut-marketplace-service' })
	async handleSelectMenu(interaction: StringSelectMenuInteraction) {
		const commandInteraction = ongoingInteractions.get(interaction.user.id);
		if (!commandInteraction) return;

		// Check if we're on cooldown
		const delay = ms(config.settings.servers.cooldown);
		let textDuration = delay < 1000 ? `1 second` : ms(delay, { long: true });
		if (textDuration == '1 day') textDuration = '24 hours';

		const userKey = cooldown.generateKey(
			commandInteraction.guild,
			`marketplace`,
			interaction.user.id
		);
		const userCooldown = await cooldown.isOnCooldown(userKey);

		if (userCooldown)
			return commandInteraction.editReply({
				content: '',
				embeds: [
					createEmbed(
						`<:no:659939343875702859> You have already posted a listing in the last ${textDuration}.`
					)
				],
				components: []
			});

		// Check if the specific service is on cooldown
		if (config.settings.marketplace.seperateCooldown) {
			const serviceKey = cooldown.generateKey(
				commandInteraction.guild,
				`marketplace`,
				interaction.values[0].toLowerCase(),
				interaction.user.id
			);
			const serviceCooldown = await cooldown.isOnCooldown(serviceKey);

			if (serviceCooldown)
				return commandInteraction.editReply({
					content: '',
					embeds: [
						createEmbed(
							`<:no:659939343875702859> You have already posted a listing for a \`${interaction.values[0]}\` in the last ${textDuration}.`
						)
					],
					components: []
				});
		}

		const service = interaction.values[0];
		const modal = new ModalBuilder()
			.setTitle('Create a marketplace listing!')
			.setCustomId('minehut-marketplace-' + service);

		const title = new TextInputBuilder()
			.setCustomId('title')
			.setLabel('Shortly describe your listing')
			.setStyle(TextInputStyle.Short);

		const description = new TextInputBuilder()
			.setCustomId('description')
			.setLabel('Talk about your listing!')
			.setStyle(TextInputStyle.Paragraph);

		modal.addComponents(
			new ActionRowBuilder<TextInputBuilder>().addComponents(title),
			new ActionRowBuilder<TextInputBuilder>().addComponents(description)
		);

		interaction.showModal(modal);

		commandInteraction.editReply({
			content: ':paintbrush: Waiting for you to style your listing...',
			components: []
		});
	}

	// There's likely a better way to do this, but I'm not sure how to do it, please make a PR if you know how
	@ModalComponent({ id: 'minehut-marketplace-request' })
	async handleRequestModal(interaction: ModalSubmitInteraction) {
		const [title, description] = ['title', 'description'].map((id) =>
			interaction.fields.getTextInputValue(id)
		);

		send(interaction, 'request', title, description);
	}

	@ModalComponent({ id: 'minehut-marketplace-offer' })
	async handleOfferModal(interaction: ModalSubmitInteraction) {
		const [title, description] = ['title', 'description'].map((id) =>
			interaction.fields.getTextInputValue(id)
		);

		send(interaction, 'offer', title, description);
	}
}

type service = 'request' | 'offer';
const send = async (
	interaction: ModalSubmitInteraction,
	service: service,
	title: string,
	description: string
) => {
	const userKey = cooldown.generateKey(interaction.guild, `marketplace`, interaction.user.id);

	const commandInteraction = ongoingInteractions.get(interaction.user.id);
	if (!commandInteraction) return;

	// Verify description length
	const serviceType = service == 'request' ? 'Requesting' : 'Offering';
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
		`<:minehut:583099471320055819> **${serviceType}** | ${title}`,
		``,
		description,
		``,
		`*Listing posted by <@${interaction.user.id}>*`
	);

	const marketplaceChannelid = getGuildConfig(interaction.guildId).channels.marketplace;
	const channel = (await interaction.guild.channels.fetch(marketplaceChannelid)) as Channel;

	if (!channel.isTextBased)
		return interaction.reply({
			ephemeral: true,
			embeds: [
				createEmbed(
					`<:no:659939343875702859> Marketplace channel is not a text channel. Please contact a moderator.`
				)
			]
		});

	const delay = ms(config.settings.marketplace.cooldown);

	if (config.settings.marketplace.seperateCooldown) {
		const serviceKey = cooldown.generateKey(
			interaction.guild,
			`marketplace`,
			service,
			interaction.user.id
		);
		await cooldown.setPersistentCooldown(serviceKey, delay);
	} else await cooldown.setPersistentCooldown(userKey, delay);

	const message = await (channel as TextChannel).send({
		embeds: [createEmbed(body)]
	});

	commandInteraction.editReply({
		content: '',
		embeds: [
			createEmbed(
				`<:yes:659939344192868109> Successfully posted your listing! Check it out :point_right: ${message.url}`
			)
		],
		components: []
	});

	ongoingInteractions.delete(interaction.user.id);
	await interaction.deferUpdate();
};
