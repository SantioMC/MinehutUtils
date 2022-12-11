/* A simple util file to interact with spiget.org */

import { timedFetch } from './fetch';

const SEARCH_URL = 'https://api.spiget.org/v2/resources/free';
const QUERY_SEARCH_URL = 'https://api.spiget.org/v2/search/resources';
const SEARCH_FIELDS =
	'fields=id,name,tag,likes,testedVersions,rating,releaseDate,downloads,icon,premium,sourceCodeLink';

export const BASE_URL = 'https://api.spiget.org/v2';
export const HEADERS = {
	// Headers to attach to all requests
	accept: 'application/json',
	'User-Agent': 'MinehutUtils'
};

export async function searchPlugins(query: string): Promise<Plugin[] | null> {
	const info = `?size=10&page=1&sort=-likes`;
	const uri =
		(query == '' ? SEARCH_URL : `${QUERY_SEARCH_URL}/${query}`) + `${info}&${SEARCH_FIELDS}`;

	const req = await timedFetch(uri);
	if (req == null) return null;

	if (!req.ok) return null;
	const data = await req.json();

	return data as Plugin[];
}

export async function getPlugin(id: number): Promise<Plugin | null> {
	const uri = `${BASE_URL}/resources/${id}`;

	const req = await timedFetch(uri);
	if (req == null) return null;

	if (!req.ok) return null;
	const data = await req.json();

	return data as Plugin;
}

// Removed: contributors, file, updateDate, external, links, price, currency, author, category, version, reviews, versions, updates, donationLink
export interface Plugin {
	id: number; //
	name: string; //
	tag: string;
	likes: number; //
	testedVersions: string[]; //
	rating: {
		//
		count: number;
		average: number;
	};
	releaseDate: number; //
	downloads: number;
	icon: {
		//
		url: string;
		data: string;
	};
	premium: boolean; //
	sourceCodeLink: string;
}
