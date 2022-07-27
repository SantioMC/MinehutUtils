export function fromHTML(markdown: string): string {
	let text = decodeURI(markdown);
	text = text.replaceAll(/<p>(.*?)<\/p>/gi, '$1\n'); // Handle <p> tags
	text = text.replaceAll(/<(br|hr) ?\/?>/gi, '\n'); // Handle <br> tags
	text = text.replaceAll(/<code>(.*?)<\/code>/gi, '`$1`\n'); // Handle <code> tags
	text = text.replaceAll(/<a( href="([\S ]+)")?>([\S ]+)<\/a>/gi, '[$3]($2)'); // Handle <a> tags
	text = text.replaceAll(/<[^>]*>/gi, '');
	return text;
}
