import {
	ApplicationCommandOptionType,
	AutocompleteInteraction,
	Channel,
	CommandInteraction
} from 'discord.js';
import { Discord, Slash, SlashGroup, SlashOption } from 'discordx';
import { createEmbed, embedJoinList } from '../utils/embed';
import { prisma } from '..';
import ms from 'ms';

@Discord()
@SlashGroup({
	description: 'Manage channel settings',
	name: 'channels',
	defaultMemberPermissions: 'ManageChannels'
})
@SlashGroup({
	description: 'Manage channel cooldowns',
	name: 'cooldown',
	root: 'channels'
})
export class ChannelsCommand {
	@Slash({ description: 'Get all channel cooldowns in this guild' })
	@SlashGroup('cooldown', 'channels')
	private async list(interaction: CommandInteraction) {
		const cooldowns = await prisma.channels.findMany({
			where: { guild: interaction.guildId, delay: { not: 0 } }
		});

		if (cooldowns.length == 0)
			return interaction.reply({
				embeds: [
					createEmbed(
						'<:no:659939343875702859> There are no channels with a cooldown in this guild'
					).setColor('#ff0000')
				]
			});

		const embed = createEmbed(
			embedJoinList(
				`📋 Channel Cooldowns\n`,
				...cooldowns.map((channel) => `<#${channel.channel}>: \`${format(channel.delay)}\``)
			)
		);

		interaction.reply({ embeds: [embed] });
	}

	@Slash({ description: "Set a channel's cooldown" })
	@SlashGroup('cooldown', 'channels')
	private async set(
		@SlashOption({
			name: 'channel',
			description: 'Which channel to modify',
			required: true,
			type: ApplicationCommandOptionType.Channel
		})
		channel: Channel,

		@SlashOption({
			name: 'delay',
			description: 'How long to set the cooldown for',
			required: true,
			type: ApplicationCommandOptionType.String,
			autocomplete: (interaction: AutocompleteInteraction) => {
				const focused = interaction.options.getFocused() || '';
				if (focused != '' && !isNaN(Number(focused))) {
					interaction.respond(
						['secs', 'mins', 'hours'].map((unit) => {
							return {
								name: `${focused}${unit}`,
								value: `${focused}${unit}`
							};
						})
					);
				} else interaction.respond([]);
			}
		})
		delay: string,

		interaction: CommandInteraction
	) {
		if (interaction.guild == null) return;

		if (!channel.isTextBased)
			return interaction.reply({
				embeds: [
					createEmbed(
						`<:no:659939343875702859> Channel <#${channel.id}> is not a text channel`
					).setColor('#ff0000')
				]
			});

		const delay_ms = ms(delay);
		if (delay_ms == undefined || delay_ms < 0)
			return interaction.reply({
				embeds: [
					createEmbed(`<:no:659939343875702859> Invalid delay \`${delay}\``).setColor('#ff0000')
				]
			});

		// Get channel data or create it
		await prisma.channels.upsert({
			where: { channel: interaction.channelId },
			create: { channel: interaction.channelId, guild: interaction.guildId, delay: delay_ms },
			update: { delay: delay_ms }
		});

		interaction.reply({
			embeds: [
				createEmbed(
					`<:yes:659939181056753665> Set the cooldown for <#${channel.id}> to \`${format(
						delay_ms
					)}\``
				)
			]
		});
	}

	@Slash({ description: 'Reset a channel back to default settings' })
	@SlashGroup('channels')
	private async reset(
		@SlashOption({
			name: 'channel',
			description: 'Which channel to modify',
			required: true,
			type: ApplicationCommandOptionType.Channel
		})
		channel: Channel,
		interaction: CommandInteraction
	) {
		if (interaction.guild == null) return;

		if (!channel.isTextBased)
			return interaction.reply({
				embeds: [
					createEmbed(
						`<:no:659939343875702859> Channel <#${channel.id}> is not a text channel`
					).setColor('#ff0000')
				]
			});

		// Check if the channel exists
		const exists = await prisma.channels.findFirst({
			where: { channel: interaction.channelId }
		});

		if (!exists)
			return interaction.reply({
				embeds: [
					createEmbed(
						`<:no:659939343875702859> There were no settings found for <#${channel.id}>!`
					).setColor('#ff0000')
				]
			});

		await prisma.channels.delete({ where: { channel: interaction.channelId } });

		interaction.reply({
			embeds: [createEmbed(`<:yes:659939181056753665> Reset all settings for <#${channel.id}>`)]
		});
	}
}

const format = (delay: number): string => {
	return ms(delay, { long: true });
};
