import { CommandInteraction, ApplicationCommandOptionType, User } from 'discord.js';
import { Discord, Slash, SlashGroup, SlashOption } from 'discordx';
import * as cooldown from '../services/cooldown';
import { createEmbed } from '../utils/embed';

@Discord()
@SlashGroup({
	description: 'Manage cooldowns for commands',
	name: 'cooldown',
	defaultMemberPermissions: 'ManageMessages'
})
@SlashGroup({
	description: 'Reset cooldowns for commands',
	name: 'reset',
	root: 'cooldown'
})
export class CooldownCommand {
	@Slash({ name: 'server', description: 'Reset the server ad cooldown for a specific user' })
	@SlashGroup('reset', 'cooldown')
	private async reset(
		@SlashOption({
			description: 'The user to reset the cooldown of',
			name: 'user',
			type: ApplicationCommandOptionType.User,
			required: true
		})
		user: User,

		interaction: CommandInteraction
	) {
		if (!interaction.guild) return;

		const key = cooldown.generateKey(interaction.guild, 'advertise', 'user', user.id);
		await cooldown.clearCooldown(key);

		interaction.reply({
			ephemeral: true,
			embeds: [createEmbed(`<:yes:659939344192868109> Reset the cooldown for <@${user.id}>`)]
		});
	}
}
