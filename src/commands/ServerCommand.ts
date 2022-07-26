import { CommandInteraction } from 'discord.js';
import { Discord, Slash, SlashOption } from 'discordx';

@Discord()
class ServerCommand {
	@Slash('server')
	server(
		@SlashOption('server', { description: 'Minehut Server Name' })
		x: string,

		interaction: CommandInteraction
	) {
		interaction.reply('Hi');
	}
}
