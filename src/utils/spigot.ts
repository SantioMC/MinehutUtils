/* A simple util file to interact with spiget.org */

import Axios from 'axios';

const axios = Axios.create({
	baseURL: 'https://api.spiget.org/v2',
	timeout: 5000,
	headers: {
		accept: 'application/json',
		'User-Agent': 'MinehutUtils'
	}
});

const SEARCH_FIELDS =
	'fields=id,name,tag,likes,testedVersions,rating,releaseDate,downloads,icon,premium,sourceCodeLink';

export const HEADERS = {
	// Headers to attach to all requests
	accept: 'application/json',
	'User-Agent': 'MinehutUtils'
};

export async function searchPlugins(query: string): Promise<Plugin[] | null> {
	const info = `?size=10&page=1&sort=-likes`;

	const data = await axios
		.get(query == '' ? '/resources/free' : `/search/resources/${query}${info}&${SEARCH_FIELDS}`)
		.then((res) => res.data)
		.catch(() => null);

	if (!data) return null;
	return data as Plugin[];
}

export async function getPlugin(id: number): Promise<Plugin | null> {
	const data = await axios
		.get('/resources/${id}')
		.then((res) => res.data)
		.catch(() => null);

	if (!data) return null;
	return data as Plugin;
}

// Removed: contributors, file, updateDate, external, links, price, currency, author, category, version, reviews, versions, updates, donationLink
export interface Plugin {
	id: number;
	name: string;
	tag: string;
	likes: number;
	testedVersions: string[];
	rating: {
		//
		count: number;
		average: number;
	};
	releaseDate: number;
	downloads: number;
	icon: {
		//
		url: string;
		data: string;
	};
	premium: boolean;
	sourceCodeLink: string;
}
