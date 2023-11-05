<br/>
<p align="center">
  <h3 align="center">MinehutUtils</h3>

  <p align="center">
    Open-source community made discord bot for <a href="https://discord.gg/Minehut">discord.gg/Minehut</a>
    <br/><br/>
    <a href="https://github.com/SantioMC/MinehutUtils/issues">Report Bug</a>
    —
    <a href="https://github.com/SantioMC/MinehutUtils/issues">Request Feature</a>

  </p>
</p>

<div align="center">

![Contributors](https://img.shields.io/github/contributors/SantioMC/MinehutUtils?color=dark-green)
![Issues](https://img.shields.io/github/issues/SantioMC/MinehutUtils)
</div>

## Table Of Contents

* [Introduction](#introduction)
* [Getting Started](#getting-started)
    * [Installation](#installation)
    * [Setup](#setup)
      * [Marketplace](#marketplace)
      * [Advertisements](#advertise)
      * [Cooldowns](#cooldowns)
      * [Logs](#logs)
* [Environment Variables](#environment-variables)
* [Contributing](#contributing)

## Introduction

MinehutUtils is a community made discord bot for [discord.gg/Minehut](https://discord.gg/Minehut). 
It is made with [Kotlin](https://kotlinlang.org/), [JDA](https://github.com/DV8FromTheWorld/JDA) 
and [Coffee](https://github.com/SantioMC/Coffee). For the full list of dependencies check out the
[dependency graph](https://github.com/SantioMC/MinehutUtils/network/dependencies).

MinehutUtils is what's currently being used in the [Minehut Official Discord](https://discord.gg/Minehut) and
provides users with the functionality to view server information, network information, and advertise both their
servers and services.

The following is a full list of commands that MinehutUtils provides:

| Command         | Description                                    | Public |
|-----------------|------------------------------------------------|--------|
| `server`        | Displays information about a Minehut Server    | ✅      |
| `network`       | Displays information about the Minehut Network | ✅      |
| `status`        | Calculates the status of core Minehut services | ✅      |
| `advertise`     | Advertises a Minehut server                    | ✅      |
| `marketplace`   | Either request or offer your services          | ✅      |
| `cooldown info` | View server and your cooldown                  | ✅      |
| `cooldown`      | Manage user's cooldowns                        | ❌      |
| `settings`      | Change the bot settings or behaviour           | ❌      |

## Getting Started

### Installation
This guide assumes you've already made a [Discord Bot](https://discord.com/developers/applications)
and retrieved your bot token.

#### Docker
Running the bot with docker is the easiest way to get started and recommended for most users.

1. Pull the docker image from github packages
    ```shell
    docker pull ghcr.io/santiomc/minehututils:nightly
    ```

2. Run the docker image
    ```shell
    docker run -d --rm \
    -e TOKEN=<your bot token> \
   ghcr.io/santiomc/minehututils:nightly
    ```

> Note: Since the bot uses a database to store cooldowns and settings, you'll need to mount a volume
> to store the database. You can do this by adding a mount for `/app` to the docker container.

#### Docker Compose
If you want to run the bot with docker-compose, you can use the predefined docker-compose.yml file
(or create your own).

1. Download the docker-compose.yml file
    ```shell
    curl -O https://raw.githubusercontent.com/SantioMC/MinehutUtils/master/docker-compose.yml
    ```
   
2. Create a `.env` and attach your bot token.
    ```properties
    TOKEN=your bot token
    ```
3. Run the docker-compose.yml file
    ```shell
    docker compose up -d
    ```
   
#### Manual Installation
If you want to run the bot without a container, you can do so by following these steps:

1. Clone the repository
    ```shell
    git clone https://github.com/SantioMC/MinehutUtils
    ```

2. Build the project
    ```shell
    ./gradlew shadowJar
    ```
   
3. Run the jar file
    ```shell
    java -jar build/libs/MinehutUtils-<version>.jar
    ```
   
### Setup

The bot by default should now have most things working, however a bit of configuration is needed
to get both the marketplace and advertisement commands setup, along with a few other settings.
For more advanced configuration, see the [environment variables](#environment-variables)

#### Marketplace
To get the marketplace working, you should first create a channel for it if you haven't already.
Once you have a channel selected, use `/settings marketplace channel:<channel>` to set the channel.

After that, users should be able to post both offers and requests in the channel.

#### Advertise
To get the advertise command working, create a channel for it like before, and run the following command:
`/settings advertise channel:<channel>`

#### Cooldowns
Setting a cooldown for either the advertise or marketplace command is easy. Simply run the following command:
```
/settings cooldown advertise cooldown:<time>
/settings cooldown marketplace cooldown:<time>
```

You're able to put any short-form time value in the cooldown, for example `1d`, `1h` or `30m`.

#### Logs
By default, the bot will log to any channel appropriately named `#logs`, however this channel can be
overridden by setting the `LOG_CHANNEL` environment variable. Logging can be disabled by setting this to
any non-existent channel, a good example of this is `LOG_CHANNEL=.` *(Channels can not contain a dot)*.

## Environment Variables
Below are a list of all possible environment variables that can be set to configure the bot.

| Variable    | Description                                   | Default |
|-------------|-----------------------------------------------|---------|
| DB_FILE     | What should the database file be named        | data.db |
| TOKEN       | The bot token                                 |         |
| LOG_CHANNEL | The channel to post logs to *(without the #)* | logs    |

## Contributing
Thanks for everyone who has already contributed, and anyone willing to. This is a community bot and
your contributions are what makes it great. If you aren't able to make a pull request, you can
always open an issue with a bug report or feature request [here on github](https://github.com/SantioMC/MinehutUtils/issues)

### Creating a pull request
1. Fork the repository
2. Clone your fork
    ```shell
    git clone https://github.com/YOUR_USERNAME/MinehutUtils
    ```
3. Create the changes you want to make
4. Commit and push your changes
    ```shell
    git add .
    git commit -m "Your commit message"
    git push
    ```
5. Create a pull request at https://github.com/SantioMC/MinehutUtils/pulls