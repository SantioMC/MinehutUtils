import fetch, { RequestInfo, RequestInit, Response } from 'node-fetch';
import { AttachmentBuilder } from 'discord.js';

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

export async function getImage(url: string): Promise<ImageAttachable | null> {
	const res = await timedFetch(url);
	if (res == null) return null;

	const buffer = Buffer.from(await res.arrayBuffer());
	const imageBuffer = Buffer.from(buffer.toString('utf8'), 'base64');

	return {
		attachment: new AttachmentBuilder(imageBuffer, { name: 'image.png' }).setName('image.png'),
		name: 'image.png',
		file: `attachment://image.png`
	};
}

export interface ImageAttachable {
	attachment: AttachmentBuilder;
	name: string;
	file: string;
}
