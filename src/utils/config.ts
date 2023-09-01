import configFile from 'src/../../config.json';

export const config = configFile;

export const getGuildConfig = (guild: string) => {
	return config.guilds.find((g) => g.guild == guild);
};
