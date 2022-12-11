import { timedFetch } from './fetch';
import { fromHTML } from './markdown';

const BASE_URL = 'https://shop.minehut.com';

const SEARCH_URL = `${BASE_URL}/search/suggest.json?q={QUERY}&resources[type]=product&resources[limit]={LIMIT}`;
const CREDIT_URL = `${BASE_URL}/products/{ADDON}?view=detailed`;

export async function getAddons(query: string, limit?: number): Promise<Addon[] | null> {
	const uri = SEARCH_URL.replace('{QUERY}', query).replace('{LIMIT}', (limit || 6).toString());

	const req = await timedFetch(uri);
	if (req == null) return null;

	const data = await req.json();
	if (data.ok == false) return null;

	const addons: Addon[] = data.resources.results.products.map((res: Addon) => res);
	const specific = addons.filter((addon) => addon.title.toLowerCase() == query.toLowerCase())[0];

	if (specific) {
		const specificReq = await timedFetch(CREDIT_URL.replace('{ADDON}', specific.handle));
		const specificData =
			specificReq == null ? { credit_price: 0, credit_price_sale: 0 } : await specificReq.json();

		return [{ ...specific, ...specificData }];
	}

	return addons;
}

export function encodeBody(addon: Addon): string {
	var original = fromHTML(addon.body);
	var body = original.trim();

	body = original.split('\n').slice(0, 10).join('\n');
	body = body.length < 2000 ? body : body.slice(0, 2000) + '...';

	if (body.trim() == '') body = 'No description available.';
	if (body != original)
		body += `\n\n*Click [here](${BASE_URL}${addon.url}) to learn more about this product!*`;

	return body;
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

	// Addon-Specific
	credit_price?: number;
	credit_price_sale?: number;
}
