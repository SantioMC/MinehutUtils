import { timedFetch } from './fetch';

const MINECRAFT_BEDROCK_VERSION_LIST = 'https://wiki.vg/Bedrock_Protocol_version_numbers';
const VERSION_REGEX =
	/https:\/\/minecraft\.fandom\.com\/wiki\/Bedrock_Edition_(?:beta_|Preview_)?([\d\.]+)">[\d\.]+<\/a>/gi;

// If anyone knows a better way for fetching the latest bedrock version, please let me know.
export async function getBedrockVersion(): Promise<string | null> {
	const req = await timedFetch(MINECRAFT_BEDROCK_VERSION_LIST);
	if (req == null) return null;

	const page = await req.text();
	if (page == null) return null;

	const search = page.match(VERSION_REGEX);
	if (search == null) return null;

	const versions = search
		.map((v) => (/\>([\d\.]+)\</i.exec(v) || [])[1])
		.sort(compareSemanticVersions);

	return versions[versions.length - 1];
}

// Semantic versioning sorting
// @author https://medium.com/geekculture/sorting-an-array-of-semantic-versions-in-typescript-55d65d411df2
export const compareSemanticVersions = (a: string, b: string) => {
	const a1 = a.split('.');
	const b1 = b.split('.');

	const len = Math.min(a1.length, b1.length);

	for (let i = 0; i < len; i++) {
		const a2 = +a1[i] || 0;
		const b2 = +b1[i] || 0;

		if (a2 !== b2) {
			return a2 > b2 ? 1 : -1;
		}
	}

	return b1.length - a1.length;
};
