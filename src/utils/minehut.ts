import Axios from 'axios';
import { compareSemanticVersions, getBedrockVersion } from './minecraft';

const axios = Axios.create({
	baseURL: `https://api.minehut.com`,
	timeout: 5000,
	headers: {
		accept: 'application/json',
		'User-Agent': 'MinehutUtils'
	}
});

const minecraft = Axios.create({
	baseURL: `https://api.mcsrvstat.us`,
	timeout: 5000,
	headers: {
		accept: 'application/json',
		'User-Agent': 'MinehutUtils'
	}
});

export function cleanMOTD(motd: string): string {
	return motd
		.replace(/```/g, '') // Clean off code blocks
		.replace(/\n /g, '\n') // Fix newline spacing
		.replace(/^\s+/g, '') // Get rid of leading whitespace
		.replace(/[§&][0-9a-frmno]/g, '')
		.replace(
			/<\/?(black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white|rainbow)>/gi,
			''
		) // Remove all color tags
		.replace(/<\/?((?:color:)?#[0-9A-F]{6})>/gi, '') // Remove all hex color tags
		.replace(/<\/?((?:gradient|transition)(?::#?[a-z0-9]+)+)>/gi, '') // Remove all gradient tags
		.replace(
			/<\/?(bold|b|italic|em|i|underlined|u|strikethrough|st|obfuscated|obf|reset|color|transition|rainbow|newline)>/gi,
			''
		); // Remove all other tags
}

export function getBanner(server: ServerData) {
	// FEAT: Use canvas to generate a banner image similar to the ones in https://minehut.com/servers
	return null;
}

// Used for the server command to autocomplete names
export async function getServerNames(filter: String | null = null): Promise<string[]> {
	const data = await axios
		.get('/servers')
		.then((res) => res.data)
		.catch(() => null);

	if (!data) return [];

	var servers = (data.servers as ServerData[]).sort((a, b) => a.playerCount - b.maxPlayers);
	if (filter != null)
		servers = servers.filter((server) => server.name.toLowerCase().includes(filter.toLowerCase()));

	return servers.map((server) => server.name);
}

export async function getServerData(server: string): Promise<ServerData | null> {
	let data;

	if (server.length == 24) {
		// We were provided a server ID
		data = await axios
			.get(`/server/${server}`)
			.then((res) => res.data)
			.catch(() => null);
	} else {
		// We were provided a server name
		data = await axios
			.get(`/server/${server}?byName=true`)
			.then((res) => res.data)
			.catch(() => null);
	}

	if (data == null) return null;
	return data.server as ServerData;
}

export async function getNetworkStats(): Promise<NetworkStats | null> {
	const networkData = await axios
		.get('/network/simple_stats')
		.then((res) => res.data)
		.catch(() => null);

	if (!networkData) return null;

	const playerData = await axios
		.get('/network/players/distribution')
		.then((res) => res.data)
		.catch(() => null);

	if (!playerData) return null;
	return { ...networkData, ...playerData } as NetworkStats;
}

export type ServerPlan =
	| 'FREE'
	| 'CUSTOM'
	| 'DAILY'
	| 'MH20'
	| 'MH35'
	| 'MH75'
	| 'MHUnlimited'
	| 'EXTERNAL';

export function getPlan(server: ServerData): ServerPlan {
	const data = server.server_plan.split('_');
	const plan = data[0] == 'CUSTOM' ? 'CUSTOM' : data[data.length - 1].toUpperCase();

	switch (plan) {
		case 'FREE':
			return 'FREE';
		case 'DAILY':
			return 'DAILY';
		case '2GB':
			return 'MH20';
		case '3GB':
			return 'MH35';
		case '6GB':
			return 'MH75';
		case '10GB':
			return 'MHUnlimited';
		case 'CUSTOM':
			return 'CUSTOM';
		case 'EXTERNAL':
			return 'EXTERNAL';
		default:
			return 'FREE';
	}
}

export async function getMinehutStatus(): Promise<MinehutStatus> {
	// Default status
	let data: MinehutStatus = {
		minecraft_java: 'Working',
		minecraft_bedrock: 'Working',
		minecraft_proxy: 'Working',
		api: 'Working',
		bedrock_version: '?',
		latest_bedrock_version: '?'
	};

	// Check general network information
	getNetworkStats().then((stats: NetworkStats | null) => {
		if (stats == null) return (data.api = 'Offline');

		// Bedrock handling
		if (stats.bedrockTotal < 50) data.minecraft_bedrock = 'Degraded';
		if (stats.bedrockTotal == 0) data.minecraft_bedrock = 'Offline';

		// Java handling
		if (stats.javaTotal < 1000) data.minecraft_java = 'Degraded';
		if (stats.javaTotal == 0) data.minecraft_java = 'Offline';
	});

	// Check to see if proxy is pingable
	const proxy = await minecraft
		.get(`/2/minehut.com`)
		.then((res) => res.data)
		.catch(() => null);

	if (!proxy || !proxy.online) data.minecraft_proxy = 'Offline';

	// Check to see if bedrock proxy is pingable
	const bedrock = await minecraft
		.get(`/bedrock/2/bedrock.minehut.com`)
		.then((res) => res.data)
		.catch(() => null);

	if (!bedrock || !bedrock.online) {
		data.minecraft_bedrock = 'Offline';
		return data;
	}

	// Compare bedrock proxy to latest bedrock version
	const latestVersion = await getBedrockVersion();
	if (latestVersion == null) return data;

	if (!bedrock.debug.query) {
		data.bedrock_version = 'Unknown';
		data.minecraft_bedrock = 'Offline';
	} else if (!bedrock.version) {
		data.bedrock_version = 'Unknown';
		data.minecraft_bedrock = 'Working';
	} else if (compareSemanticVersions(bedrock.version, latestVersion) == -1) {
		data.minecraft_bedrock = 'Outdated';
		data.latest_bedrock_version = latestVersion;
		data.bedrock_version = bedrock.version || '?';
	}

	return data;
}

// Interfaces
export type Status = 'Working' | 'Degraded' | 'Outdated' | 'Offline';

export interface MinehutStatus {
	minecraft_java: Status;
	minecraft_bedrock: Status;
	minecraft_proxy: Status;
	api: Status;
	bedrock_version: string;
	latest_bedrock_version: string;
}

export interface NetworkStats {
	player_count: number;
	server_count: number;
	server_max: number;
	ram_count: number;
	ram_max: number;
	bedrockTotal: number;
	javaTotal: number;
	bedrockLobby: number;
	bedrockPlayerServer: number;
	javaLobby: number;
	javaPlayerServer: number;
}

export interface ServerData {
	_id: string;
	name: string;
	name_lower: string;
	motd: string;

	creation: number;
	last_online: number;

	proxy: boolean;
	connectedServers: string[];

	categories: string[];
	inheritedCategories: string[];
	purchased_icons: string[];
	active_icon: string;
	icon: string;

	plan: string;
	price: number;
	credits_per_day: number;
	activeServerPlan: string;
	rawPlan: string;
	maxPlayers: number;

	visibility: boolean;
	server_plan: string;
	storage_node: string;
	default_banner_image: string;
	default_banner_tint: string;
	owner: string;
	platform: string;
	online: boolean;
	playerCount: number;
	port: number;
	backup_slots: number;
	suspended: boolean;
	server_version_type: string;

	__v: number;
}
