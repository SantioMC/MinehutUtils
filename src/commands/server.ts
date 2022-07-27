import { CommandInteraction } from 'discord.js';
import { Discord, Slash, SlashOption } from 'discordx';
import { createEmbed, toEmbed } from '../utils/embed';
import { getServerData, ServerData } from '../utils/minehut';

@Discord()
export class ServerCommand {
	@Slash('server', { description: 'View information about a Minehut Server' })
	private async server(
		@SlashOption('server', { description: 'The name of the server', required: true })
		server: string,
		interaction: CommandInteraction
	) {
		await interaction.reply({
			embeds: [createEmbed(`<a:typing:664898517738717199> Fetching server data...`)]
		});

		getServerData(server).then((data: ServerData | null) => {
			if (data == null) {
				return interaction.editReply({
					embeds: [createEmbed(`<:no:659939343875702859> The server \`${server}\` could not be found.`)]
				});
			}

			interaction.editReply({ embeds: [toEmbed(data)] });
		});
	}
}
