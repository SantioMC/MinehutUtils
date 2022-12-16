import { timedFetch } from './fetch';
import { compareSemanticVersions, getBedrockVersion } from './minecraft';

const BASE_URL = `https://api.minehut.com`;

export function cleanMOTD(motd: string): string {
	return motd
		.replace(/[&§][A-F0-9rklmno]/gi, ' ') // Clean off color codes
		.replace(/```/g, '') // Clean off code blocks
		.replace(/\n /g, '\n') // Fix newline spacing
		.replace(/^\s+/g, ''); // Get rid of leading whitespace
}

export function getBanner(server: ServerData) {
	// FEAT: Use canvas to generate a banner image similar to the ones in https://minehut.com/servers
	return null;
}

// Used for the server command to autocomplete names
export async function getServerNames(filter: String | null = null): Promise<string[]> {
	const req = await timedFetch(`${BASE_URL}/servers`);
	if (req == null) return [];

	const data = await req.json();
	if (data == null) return [];

	var servers = (data.servers as ServerData[]).sort((a, b) => a.playerCount - b.maxPlayers);
	if (filter != null)
		servers = servers.filter((server) => server.name.toLowerCase().includes(filter.toLowerCase()));

	return servers.map((server) => server.name);
}

export async function getServerData(server: string): Promise<ServerData | null> {
	var uri;

	if (server.length == 24) {
		// We were provided a server ID
		uri = `${BASE_URL}/server/${server}`;
	} else {
		// We were provided a server name
		uri = `${BASE_URL}/server/${server}?byName=true`;
	}

	var data = await timedFetch(uri).then((res) => res?.json() || { ok: false });
	if (data.ok == false) return null;

	return data.server as ServerData;
}

export async function getNetworkStats(): Promise<NetworkStats | null> {
	const networkData = await timedFetch(`${BASE_URL}/network/simple_stats`).then(
		(res) => res?.json() || { ok: false }
	);
	if (networkData.ok == false) return null;

	const playerData = await timedFetch(`${BASE_URL}/network/players/distribution`).then(
		(res) => res?.json() || { ok: false }
	);
	if (playerData.ok == false) return null;

	return { ...networkData, ...playerData } as NetworkStats;
}

export type ServerPlan = 'FREE' | 'CUSTOM' | 'DAILY' | 'MH20' | 'MH35' | 'MH75' | 'MHUnlimited';

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
	const proxy = await timedFetch(`https://mcapi.us/server/status?ip=minehut.com`).then(
		(res) => res?.json() || { ok: false }
	);
	if (proxy.ok == false || !proxy.online) data.minecraft_proxy = 'Offline';

	// Check to see if bedrock proxy is pingable
	const bedrock = await timedFetch(`https://api.mcsrvstat.us/bedrock/2/bedrock.minehut.com`).then(
		(res) => res?.json() || { ok: false }
	);
	if (bedrock.ok == false || !bedrock.online) data.minecraft_bedrock = 'Offline';

	// Compare bedrock proxy to latest bedrock version
	const latestVersion = await getBedrockVersion();
	if (latestVersion == null) return data;

	if (!bedrock.version) {
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
