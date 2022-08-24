import { timedFetch } from './fetch';

const MINECRAFT_BEDROCK_VERSION_LIST = 'https://minecraft.fandom.com/wiki/Protocol_version';
const VERSION_REGEX = /Bedrock Edition v([\d.]+)/gi;

// If anyone knows a better way for fetching the latest bedrock version, please let me know.
export async function getBedrockVersion(): Promise<string | null> {
	const req = await timedFetch(MINECRAFT_BEDROCK_VERSION_LIST);
	if (req == null) return null;

	const page = await req.text();
	if (page == null) return null;

	const search = VERSION_REGEX.exec(page);
	if (search == null) return null;

	return search[1];
}
