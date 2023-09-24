import {
	ApplicationCommandOptionType,
	AutocompleteInteraction,
	CommandInteraction
} from 'discord.js';
import { Discord, Slash, SlashOption } from 'discordx';
import { clean, createEmbed, toEmbed } from '../utils/embed';
import { getServerData, getServerNames, ServerData } from '../utils/minehut';

// Decorators cannot access `this`
let serverCache: {
	lastUpdate: number;
	names: string[];
} = { lastUpdate: 0, names: [] };

@Discord()
export class ServerCommand {
	constructor() {
		this.updateCache();
	}

	@Slash({ name: 'server', description: 'View information about a Minehut Server' })
	private async server(
		@SlashOption({
			name: 'server',
			description: 'The name of the server',
			required: true,
			type: ApplicationCommandOptionType.String,
			autocomplete: async (interaction: AutocompleteInteraction) => {
				interaction.respond(
					serverCache.names
						.filter((name) => name.startsWith(interaction.options.getFocused()))
						.slice(0, 25)
						.map((name) => {
							return {
								name,
								value: name
							};
						})
				);
			}
		})
		server: string,
		interaction: CommandInteraction
	) {
		await interaction.deferReply();

		this.updateCache();

		getServerData(server).then((data: ServerData | null) => {
			if (data == null) {
				return interaction.followUp({
					embeds: [
						createEmbed(
							`${process.env.FAIL_EMOJI || ''} The server \`${clean(server)}\` could not be found.`
						)
					]
				});
			}

			interaction.followUp({ embeds: [toEmbed(data)] });
		});
	}

	private updateCache() {
		if (Date.now() - serverCache.lastUpdate < 30_000) {
			return;
		}

		getServerNames().then(
			(names) =>
				(serverCache = {
					lastUpdate: Date.now(),
					names
				})
		);
	}
}
