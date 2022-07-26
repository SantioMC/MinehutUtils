import * as discordx from 'discordx';

require('dotenv').config();

const client = new discordx.Client({
	intents: [],
	silent: false
});

client.on('ready', async () => {
	console.log('> Bot online, logged in as: ' + client.user!!.tag);

	await importx(__dirname + '/commands/**/*.{js,ts}');
	await client.initApplicationCommands();
});

client.on('interactionCreate', (interaction) => {
	client.executeInteraction(interaction);
});

client.login(process.env.TOKEN!!);
function importx(arg0: string) {
	throw new Error('Function not implemented.');
}
