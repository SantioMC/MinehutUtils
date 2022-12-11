# Minehut Utils Bot

This is a bot that provides useful commands for Minehut.

Commands that are included are listed below:
|command|description|
|:---|:---|
|addon|Gets information about an addon|
|server|Gets information about a server|
|network|Shows you network information|
|status|Checks status of different services in Minehut|

## Running the bot

Running the bot is very simple, just pull the docker image and run it with your token.

```bash
docker pull ghcr.io/santiomc/minehututils:master

docker run \
   --name minehututils \
   -e TOKEN=<YOUR_TOKEN> \
   -d \
   --restart always \
   ghcr.io/santiomc/minehututils:master
```
