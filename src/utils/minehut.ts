import { timedFetch } from './fetch';

const BASE_URL = `https://api.minehut.com`;
const BANNER_URL = `https://image-service-prd.superleague.com/v1/images/server-banner-images/{BANNER}?size=482x62`;

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

export async function getServerData(server: string): Promise<ServerData | null> {
	const data = await timedFetch(`${BASE_URL}/server/${server}?byName=true`).then(
		(res) => res?.json() || { ok: false }
	);
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

export type ServerPlan = 'FREE' | 'DAILY' | 'MH20' | 'MH35' | 'MH75' | 'MHUnlimited';

export function getPlan(server: ServerData): ServerPlan {
	const data = server.server_plan.split('_');
	const plan = data.length == 2 ? data[1].toUpperCase() : 'FREE';

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
		default:
			return 'FREE';
	}
}

export async function getMinehutStatus(): Promise<MinehutStatus> {
	let data: MinehutStatus = {
		minecraft_java: 'Working',
		minecraft_bedrock: 'Working',
		minecraft_proxy: 'Working',
		api: 'Working'
	};

	getNetworkStats().then((stats: NetworkStats | null) => {
		if (stats == null) return (data.api = 'Offline');

		if (stats.bedrockTotal < 50) data.minecraft_bedrock = 'Degraded';
		if (stats.bedrockTotal == 0) data.minecraft_bedrock = 'Offline';

		if (stats.javaTotal < 1000) data.minecraft_java = 'Degraded';
		if (stats.javaTotal == 0) data.minecraft_java = 'Offline';
	});

	const proxy = await timedFetch(`https://mcapi.us/server/status?ip=minehut.com`).then(
		(res) => res?.json() || { ok: false }
	);
	if (proxy.ok == false || !proxy.online) data.minecraft_proxy = 'Offline';

	return data;
}

// Interfaces
export type Status = 'Working' | 'Degraded' | 'Offline';

export interface MinehutStatus {
	minecraft_java: Status;
	minecraft_bedrock: Status;
	minecraft_proxy: Status;
	api: Status;
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
