import { timedFetch } from './fetch';

const BASE_URL = 'https://shop.minehut.com';
const SEARCH_URL = `${BASE_URL}/search/suggest.json?q={QUERY}&resources[type]=product&resources[limit]={LIMIT}`;

export async function getAddons(query: string, limit?: number): Promise<Addon[] | null> {
	const uri = SEARCH_URL.replace('{QUERY}', query).replace('{LIMIT}', (limit || 6).toString());

	const req = await timedFetch(uri);
	if (req == null) return null;

	const data = await req.json();
	if (data.ok == false) return null;

	const addons: Addon[] = data.resources.results.products.map((res: any) => res as Addon);
	const specific = addons.filter((addon) => addon.title.toLowerCase() == query.toLowerCase())[0];

	if (specific) return [specific];
	return addons;
}

// Interfaces
export interface Addon {
	available: boolean;
	body: string;
	compare_at_price_max: string;
	compare_at_price_min: string;
	handle: string;
	id: number;
	image: string;
	price: string;
	price_max: string;
	price_min: string;
	tags: string[];
	title: string;
	type: string;
	url: string;
	variants: any[];
	vendor: string;
	featured_image: {
		alt: string;
		aspect_ratio: number;
		height: number;
		url: string;
		width: number;
	};
}
