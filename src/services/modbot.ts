// Provides a makeshift way of getting an evaluation against user input on discord's auto moderation

import { AutoModerationRule, Guild } from 'discord.js';

export const isFiltered = async (guild: Guild, message: string): Promise<boolean> => {
	try {
		const automod = guild.autoModerationRules;
		const rules = Array.from((await automod.fetch()).values());
		return rules.some((rule) => evaluateRule(rule, message));
	} catch (e) {
		if (e.message == 'Missing Permissions') return false;
		throw e;
	}
};

const evaluateRule = (rule: AutoModerationRule, message: string): boolean => {
	const metadata = rule.triggerMetadata;

	const regex = metadata.regexPatterns.map((pattern) => new RegExp(pattern, 'i'));
	const blockedWords = metadata.keywordFilter.map((w) => w.toLowerCase());
	const allowedWords = metadata.allowList.map((w) => w.toLowerCase());

	// check if any words are blocked
	const words = message.split(' ').map((word) => word.toLowerCase().trim());
	for (const word of words) {
		for (const blockedWord of blockedWords) {
			if (word.includes(blockedWord) && !allowedWords.includes(blockedWord)) return true;
		}
	}

	// check if any regex patterns match
	const matches = regex
		// get matches
		.map((regex) => message.match(regex))
		.filter((matches) => matches != null)
		// check if any words are blocked
		.filter((matches) => {
			for (const match of matches) {
				if (!allowedWords.includes(match.toLowerCase())) return true;
			}
		});

	return false;
};
