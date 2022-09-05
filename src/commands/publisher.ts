import {
	CommandInteraction,
	ApplicationCommandOptionType,
	InteractionReplyOptions
} from 'discord.js';
import { Discord, Slash, SlashOption } from 'discordx';
import { createEmbed, hideDiscord } from '../utils/embed';
import { getImage } from '../utils/fetch';
import { fromHTML } from '../utils/markdown';
import { getHeroImage, getPublisher, Publisher } from '../utils/market';

@Discord()
export class PublisherCommand {
	@Slash({ name: 'publisher', description: 'Search publishers on Minehut Market' })
	private async publisher(
		@SlashOption({
			name: 'slug',
			description: "The publisher's URL slug (ex. minehut)",
			required: true,
			type: ApplicationCommandOptionType.String
		})
		slug: string,
		interaction: CommandInteraction
	) {
		await interaction.deferReply();

		getPublisher(slug).then(async (publisher: Publisher | null) => {
			if (publisher == null) {
				return interaction.followUp({
					embeds: [createEmbed(`<:no:659939343875702859> Failed to find that publisher!`)]
				});
			}

			const link = `https://shop.minehut.com/collections/${publisher.publisherSlug}`;
			var embed = createEmbed(
				fromHTML(
					hideDiscord(
						publisher.description + `\n\n*Click [here](${link}) to learn more!*`,
						`[Product Page](${link})`
					)
				)
			).setTitle(publisher.publisherName);

			var data: InteractionReplyOptions = {
				embeds: [embed]
			};

			const image = await getImage(getHeroImage(publisher));

			if (image != null) {
				data.files = [image.attachment];
				embed.setImage(image.file);
			}

			interaction.followUp(data);
		});
	}
}
