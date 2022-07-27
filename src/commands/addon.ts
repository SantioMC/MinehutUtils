import {
	CommandInteraction,
	SelectMenuInteraction,
	ActionRowBuilder,
	SelectMenuBuilder,
	TextChannel,
	ApplicationCommandOptionType,
	AutocompleteInteraction
} from 'discord.js';
import { Discord, SelectMenuComponent, Slash, SlashOption } from 'discordx';
import { client } from '..';
import { createEmbed } from '../utils/embed';
import { Addon, encodeBody, getAddons } from '../utils/market';

@Discord()
export class AddonCommand {
	@Slash('addon', { description: 'Search and view addons on Minehut' })
	private async addon(
		@SlashOption('query', {
			description: 'The query to search for',
			required: true,
			type: ApplicationCommandOptionType.String,
			autocomplete: async (interaction: AutocompleteInteraction) => {
				const query: string = interaction.options.getFocused();
				if (!query) return interaction.respond([]);

				getAddons(query, 4).then((data: Addon[] | null) => {
					if (data == null) return interaction.respond([]);
					interaction.respond(
						data.map((addon) => {
							return {
								name: addon.title,
								value: addon.title
							};
						})
					);
				});
			}
		})
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
		const embed = createEmbed(encodeBody(addon));
		embed.setTitle(addon.title);
		embed.setImage(addon.featured_image.url);
		embed.setURL(`https://shop.minehut.com${addon.url}`);
		embed.setFields([
			{ name: 'Price', value: `$${addon.price}`, inline: true },
			{ name: 'Vendor', value: addon.vendor, inline: true },
			{ name: 'Category', value: addon.type, inline: true }
		]);
		return embed;
	}
}
