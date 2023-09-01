import { CommandInteraction, ApplicationCommandOptionType, User } from 'discord.js';
import { Discord, Slash, SlashGroup, SlashOption } from 'discordx';
import * as cooldown from '../services/cooldown';
import { createEmbed, embedJoinList } from '../utils/embed';
import { config } from '..';
import { getGuildConfig } from '../utils/config';
import ms from 'ms';
import { getServerData } from '../utils/minehut';

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
	@Slash({
		name: 'server',
		description: 'Reset the server ad cooldown for a specific user or server'
	})
	@SlashGroup('reset', 'cooldown')
	private async resetServer(
		@SlashOption({
			description: 'The user to reset the cooldown of',
			name: 'user',
			type: ApplicationCommandOptionType.User,
			required: false
		})
		user: User | null,

		@SlashOption({
			description: 'The server to reset the cooldown of',
			name: 'server',
			type: ApplicationCommandOptionType.String,
			required: false
		})
		server: string | null,

		interaction: CommandInteraction
	) {
		if (!interaction.guild) return;
		const reset: string[] = [];

		if (user) {
			const key = cooldown.generateKey(interaction.guild, 'advertise', 'user', user.id);
			await cooldown.clearCooldown(key);
			reset.push(`<@${user.id}>`);
		}

		if (server) {
			const serverData = await getServerData(server);
			if (!serverData)
				return interaction.reply({
					ephemeral: true,
					embeds: [
						createEmbed(`<:no:659939343875702859> The server \`${server}\` could not be found.`)
					]
				});

			const key = cooldown.generateKey(interaction.guild, 'advertise', 'server', serverData._id);
			await cooldown.clearCooldown(key);
			reset.push(`the server \`${server}\``);
		}

		if (!server && !user) {
			return interaction.reply({
				ephemeral: true,
				embeds: [
					createEmbed(
						`<:no:659939343875702859> You must specify either a user and/or a server to reset the cooldown of`
					)
				]
			});
		}

		interaction.reply({
			embeds: [
				createEmbed(
					`<:yes:659939344192868109> Reset the server ad cooldown for ${reset.join(' and ')}`
				)
			]
		});
	}

	@Slash({ name: 'marketplace', description: 'Reset the marketplace cooldown for a specific user' })
	@SlashGroup('reset', 'cooldown')
	private async resetMarketplace(
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

		[['marketplace'], ['marketplace', 'offer'], ['marketplace', 'request']].forEach(async (key) => {
			const cooldownKey = cooldown.generateKey(interaction.guild, ...key, user.id);
			await cooldown.clearCooldown(cooldownKey);
		});

		interaction.reply({
			embeds: [
				createEmbed(`<:yes:659939344192868109> Reset the marketplace cooldown for <@${user.id}>`)
			]
		});
	}

	@Slash({ name: 'info', description: 'See general information about cooldowns' })
	@SlashGroup('cooldown')
	private async info(interaction: CommandInteraction) {
		if (!interaction.guild) return;

		const guildConfig = getGuildConfig(interaction.guildId);

		const serverCooldown = await cooldown.getCooldown(
			cooldown.generateKey(interaction.guild, 'advertise', 'user', interaction.user.id)
		);

		const marketplaceCooldown = await cooldown.getCooldown(
			cooldown.generateKey(interaction.guild, 'marketplace', interaction.user.id)
		);

		const cooldowns = {
			servers: ms(ms(config.settings.servers.cooldown), { long: true }),
			marketplace: ms(ms(config.settings.marketplace.cooldown), { long: true })
		};

		const userCooldowns = {
			servers: serverCooldown ? ms(serverCooldown, { long: true }) : 'None',
			marketplace: marketplaceCooldown ? ms(marketplaceCooldown, { long: true }) : 'None'
		};

		let body = embedJoinList(
			`:stopwatch: **Cooldowns**`,
			``,
			`<#${guildConfig.channels.servers}> - Every ${cooldowns.servers}`,
			`<#${guildConfig.channels.marketplace}> - Every ${cooldowns.marketplace}`,
			``,
			`:hourglass: **Current Cooldown**`,
			``,
			`<#${guildConfig.channels.servers}> - ${userCooldowns.servers}`,
			`<#${guildConfig.channels.marketplace}> - ${userCooldowns.marketplace}`
		);

		interaction.reply({
			ephemeral: true,
			embeds: [createEmbed(body)]
		});
	}
}
