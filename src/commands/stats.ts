import { CommandInteraction } from 'discord.js';
import { Discord, Slash } from 'discordx';
import { createEmbed, formatNumber } from '../utils/embed';
import { getNetworkStats, NetworkStats } from '../utils/minehut';

@Discord()
export class StatsCommand {
	@Slash('network', { description: 'View statistics about Minehut' })
	private async stats(interaction: CommandInteraction) {
		await interaction.reply({
			embeds: [createEmbed(`<a:typing:664898517738717199> Loading...`)]
		});

		getNetworkStats().then((data: NetworkStats | null) => {
			if (data == null) {
				return interaction.editReply({
					embeds: [createEmbed("<:no:659939343875702859> I wasn't able to fetch network stats!")]
				});
			}

			interaction.editReply({
				embeds: [
					createEmbed(
						`**Players**: ${formatNumber(data.player_count)}` +
							`\n → Java: ${formatNumber(data.javaTotal)} *(Lobby: ${formatNumber(
								data.javaLobby
							)}, Servers: ${formatNumber(data.javaPlayerServer)})*` +
							`\n → Bedrock: ${formatNumber(data.bedrockTotal)} *(Lobby: ${formatNumber(
								data.bedrockLobby
							)}, Servers: ${formatNumber(data.bedrockPlayerServer)})*` +
							`\n` +
							`\n**Servers**: ${data.server_count}/${data.server_max}` +
							`\n**RAM**: ${Math.round(data.ram_count / 1000)}GB / ${data.ram_max}GB`
					).setTitle('📊 Network Stats')
				]
			});
		});
	}
}
