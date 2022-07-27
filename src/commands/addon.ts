import { channel } from 'diagnostics_channel';
import {
	CommandInteraction,
	SelectMenuInteraction,
	ActionRowBuilder,
	SelectMenuBuilder,
	TextChannel
} from 'discord.js';
import { Discord, SelectMenuComponent, Slash, SlashOption } from 'discordx';
import { client } from '..';
import { createEmbed } from '../utils/embed';
import { Addon, getAddons } from '../utils/market';

@Discord()
export class AddonCommand {
	@Slash('addon', { description: 'Search and view addons on Minehut' })
	private async addon(
		@SlashOption('query', { description: 'The query to search for', required: true })
		query: string,
		interaction: CommandInteraction
	) {
		await interaction.deferReply();

		getAddons(query).then(async (data: Addon[] | null) => {
			if (data == null) {
				return interaction.followUp({
					embeds: [createEmbed(`<:no:659939343875702859> Failed to query addons.`)]
				});
			}

			if (data.length == 0) {
				return interaction.followUp({
					embeds: [createEmbed(`<:no:659939343875702859> No addons found for \`${query}\``)]
				});
			}

			if (data.length == 1) {
				return interaction.followUp({
					embeds: [this.createAddonEmbed(data[0])]
				});
			}

			const addonOptions = data.map((addon: Addon) => {
				return {
					label: addon.title,
					value: addon.title
				};
			});

			const menu: SelectMenuBuilder = new SelectMenuBuilder()
				.addOptions(addonOptions)
				.setMaxValues(1)
				.setMinValues(1)
				.setPlaceholder('Select an addon')
				.setCustomId('addons-menu');

			const buttonRow: ActionRowBuilder = new ActionRowBuilder().addComponents(menu);

			await interaction.followUp({
				content: 'Please select which addon to view',
				// @ts-ignore
				components: [buttonRow]
			});
		});
	}

	@SelectMenuComponent('addons-menu')
	async handle(interaction: SelectMenuInteraction) {
		await interaction.deferReply();

		// Remove select menu from previous interaction
		await client.channels.fetch(interaction.channelId).then((channel) => {
			if (channel instanceof TextChannel) {
				channel.messages.fetch(interaction.message.id).then((message) => {
					message.edit({
						content: 'Please select which addon to view',
						components: []
					});
				});
			}
		});

		const title = interaction.values?.[0];
		getAddons(title, 1).then((data: Addon[] | null) => {
			if (data == null || data.length == 0) {
				return interaction.followUp({
					embeds: [createEmbed(`<:no:659939343875702859> Failed to fetch addon data`)]
				});
			}

			const addon = data[0];
			interaction.followUp({
				embeds: [this.createAddonEmbed(addon)]
			});
		});

		return;
	}

	private createAddonEmbed(addon: Addon) {
		const embed = createEmbed(addon.title);
		embed.setDescription(addon.body);
		embed.setThumbnail(addon.featured_image.url);
		embed.setURL(`https://shop.minehut.com${addon.url}`);
		embed.setFields([
			{ name: 'Price', value: `$${addon.price}` },
			{
				name: 'Compare at',
				value: `$${addon.compare_at_price_min} - $${addon.compare_at_price_max}`
			},
			{ name: 'Vendor', value: addon.vendor },
			{ name: 'Tags', value: addon.tags.join(', ') }
		]);
		return embed;
	}
}
