import {
	ApplicationCommandOptionType,
	AutocompleteInteraction,
	CommandInteraction
} from 'discord.js';
import { Discord, Slash, SlashOption } from 'discordx';
import { createEmbed, toEmbed } from '../utils/embed';
import { getServerData, getServerNames, ServerData } from '../utils/minehut';

// Every 30 seconds refresh the current server cache.
let cachedServers: string[] = [];
setInterval(() => {
	getServerNames().then((servers) => (cachedServers = servers));
}, 1000 * 30);

@Discord()
export class ServerCommand {
	@Slash({ name: 'server', description: 'View information about a Minehut Server' })
	private async server(
		@SlashOption({
			name: 'server',
			description: 'The name of the server',
			required: true,
			type: ApplicationCommandOptionType.String,
			autocomplete: async (interaction: AutocompleteInteraction) => {
				interaction.respond(
					cachedServers
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

		getServerData(server).then((data: ServerData | null) => {
			if (data == null) {
				return interaction.followUp({
					embeds: [
						createEmbed(`<:no:659939343875702859> The server \`${server}\` could not be found.`)
					]
				});
			}

			interaction.followUp({ embeds: [toEmbed(data)] });
		});
	}
}
