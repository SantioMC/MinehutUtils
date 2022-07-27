import fetch, { RequestInfo, RequestInit, Response } from 'node-fetch';

export async function timedFetch(
	uri: RequestInfo,
	data?: RequestInit,
	timeout?: number
): Promise<Response | null> {
	try {
		const controller = new AbortController();
		setTimeout(() => controller.abort(), timeout || 3000);

		return await fetch(uri, {
			...data,
			// @ts-ignore
			signal: controller.signal
		});
	} catch (_) {
		return null;
	}
}
