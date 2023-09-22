import {
	ActionRowBuilder,
	ApplicationCommandOptionType,
	AutocompleteInteraction,
	CommandInteraction,
	StringSelectMenuBuilder,
	StringSelectMenuInteraction,
	TextChannel
} from 'discord.js';
import { Discord, SelectMenuComponent, Slash, SlashOption } from 'discordx';
import { client } from '..';
import { clean, createEmbed } from '../utils/embed';
import { Plugin, searchPlugins } from '../utils/spigot';

@Discord()
export class PluginCommand {
	@Slash({ name: 'plugin', description: 'Search and view plugins available on Minehut' })
	private async plugin(
		@SlashOption({
			name: 'query',
			description: 'The query to search for',
			required: true,
			type: ApplicationCommandOptionType.String,
			autocomplete: async (interaction: AutocompleteInteraction) => {
				const query: string = interaction.options.getFocused() || '';

				searchPlugins(query).then((data: Plugin[] | null) => {
					if (data == null) return interaction.respond([]);

					interaction.respond(
						data.map((plugin) => {
							return {
								name: plugin.name,
								value: plugin.name
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

		searchPlugins(query).then(async (data: Plugin[] | null) => {
			if (data == null) {
				return interaction.followUp({
					embeds: [
						createEmbed(
							`${process.env.FAIL_EMOJI || ''} Failed to query plugins. Please try again.`
						)
					]
				});
			}

			if (data.length == 0) {
				return interaction.followUp({
					embeds: [
						createEmbed(`${process.env.FAIL_EMOJI || ''} No plugins found for \`${clean(query)}\``)
					]
				});
			}

			if (data.length == 1 || data[0].name.toLowerCase() == query.toLowerCase()) {
				return interaction.followUp({
					embeds: [this.createPluginEmbed(data[0])]
				});
			}

			const pluginOptions = data.map((plugin: Plugin) => {
				return {
					label: plugin.name,
					value: plugin.name
				};
			});

			const menu: StringSelectMenuBuilder = new StringSelectMenuBuilder()
				.addOptions(pluginOptions)
				.setMaxValues(1)
				.setMinValues(1)
				.setPlaceholder('Select an plugin')
				.setCustomId('plugins-menu');

			await interaction.followUp({
				content: 'Please select which plugin to view',
				components: [new ActionRowBuilder<StringSelectMenuBuilder>().addComponents(menu)]
			});
		});
	}

	@SelectMenuComponent({ id: 'plugins-menu' })
	async handle(interaction: StringSelectMenuInteraction) {
		await interaction.deferReply();

		// Remove select menu from previous interaction
		await client.channels.fetch(interaction.channelId).then((channel) => {
			if (channel instanceof TextChannel) {
				channel.messages.fetch(interaction.message.id).then((message) => {
					message.edit({
						content: 'Please select which plugin to view',
						components: []
					});
				});
			}
		});

		const title = interaction.values?.[0];
		searchPlugins(title).then((data: Plugin[] | null) => {
			if (data == null || data.length == 0) {
				return interaction.followUp({
					embeds: [createEmbed(`${process.env.FAIL_EMOJI || ''} Failed to fetch plugin data`)]
				});
			}

			const plugin = data[0];
			interaction.followUp({
				embeds: [this.createPluginEmbed(plugin)]
			});
		});
		return;
	}

	private createPluginEmbed(plugin: Plugin) {
		const embed = createEmbed(plugin.tag);
		embed.setTitle(plugin.name + (plugin.premium ? ' (Paid)' : ''));
		embed.setThumbnail(`https://www.spigotmc.org/${plugin.icon.url}`);
		embed.setURL(
			`https://www.spigotmc.org/resources/${encodeURIComponent(plugin.name)}.${plugin.id}`
		);
		embed.setFields([
			{
				name: 'Rating',
				value:
					`This resource has ${plugin.rating.count} reviews, and ${plugin.likes} likes!\n` +
					this.getReviews(plugin),
				inline: true
			},
			{
				name: 'Versions Supported',
				value: `This resource supports minecraft versions \`${this.getVersionString(
					plugin.testedVersions
				)}\``,
				inline: true
			},
			{
				name: 'Release Date',
				value:
					`This resource was created on ${new Date(
						plugin.releaseDate * 1000
					).toLocaleDateString()}, it has since then been downloaded a total of \`${
						plugin.downloads
					}\` times.` +
					`\n\n[*Learn More*](https://www.spigotmc.org/resources/${encodeURIComponent(
						plugin.name
					)}.${plugin.id})`,
				inline: false
			}
		]);
		return embed;
	}

	private getReviews(plugin: Plugin): string {
		const stars = Math.round(plugin.rating.average);
		return '```fix\n' + '★'.repeat(stars) + '☆'.repeat(5 - stars) + '\n```';
	}

	private getVersionString(versions) {
		if (versions.length == 0) return 'unknown';
		else if (versions.length == 1) return versions[0];
		else return versions[0] + ' — ' + versions[versions.length - 1];
	}
}
