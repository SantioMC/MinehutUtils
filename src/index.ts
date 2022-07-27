import 'reflect-metadata';

import { importx } from '@discordx/importer';
import { Client } from 'discordx';

require('dotenv').config();

export const client = new Client({
	intents: [],
	silent: false
});

client.on('ready', async () => {
	await client.clearApplicationCommands();
	await client.initApplicationCommands();

	console.log('> Bot online, logged in as: ' + client.user!!.tag);
});

client.on('interactionCreate', (interaction) => {
	client.executeInteraction(interaction);
});

async function start() {
	await importx(__dirname + '/commands/*.{js,ts}');
	client.login(process.env.TOKEN!!);
}

start();
