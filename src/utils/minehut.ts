import { EmbedBuilder } from 'discord.js';
import fetch from 'node-fetch';
import { describe } from 'node:test';
import { createEmbed } from './embed';

const BASE_URL = `https://api.minehut.com`;

export function cleanMOTD(motd: string): string {
	return motd
		.replace(/[&§][A-F0-9rklmno]/gi, ' ') // Clean off color codes
		.replace(/```/g, '') // Clean off code blocks
		.replace(/\n /g, '\n') // Fix newline spacing
		.replace(/^\s+/g, ''); // Get rid of first two spaces
}

export async function getServerData(server: string): Promise<ServerData | null> {
	const data = await fetch(`${BASE_URL}/server/${server}?byName=true`).then((res) => res.json());
	if (data.ok == false) return null;
	return data.server as ServerData;
}

export async function getNetworkStats(): Promise<NetworkStats | null> {
	const data = await fetch(`${BASE_URL}/network/simple_stats`).then((res) => res.json());
	return data as NetworkStats;
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

// Interfaces
export interface NetworkStats {
	player_count: number;
	server_count: number;
	server_max: number;
	ram_count: number;
	ram_max: number;
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
