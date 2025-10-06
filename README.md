# PlayerWelcomer

**PlayerWelcomer** is a lightweight, high-performance Minecraft plugin designed to enhance the new player experience on your server. It provides customizable first-join welcome messages and a `/welcome` command to allow players to personally greet newcomers, with configurable rewards and cooldowns. Built with clean code principles and optimized for real-time performance, this plugin is ideal for community-driven servers aiming to create a welcoming atmosphere.

## Features

- **First-Join Welcome Message**: Broadcasts a visually appealing, multi-line message to all players when a new player joins the server for the first time. Supports placeholders for player name and unique join count.
- **/welcome Command**: Allows players to send a personalized welcome message to new players within a 60-second window after they join. Includes configurable rewards (via Vault economy) and a cooldown system.
- **Dynamic New Player Detection**: Tracks new players and ensures the `/welcome` command is only usable on players who haven't been welcomed yet.
- **Comprehensive Configuration**: Fully customizable messages, reward amounts, cooldowns, and feature toggles via `config.yml`.
- **Performance Optimized**: Asynchronous execution for all potentially blocking operations (e.g., file I/O, economy transactions) to prevent server lag.
- **Robust and Maintainable**: Follows SOLID principles and clean code practices, with comprehensive error handling and logging.

## Installation

1. **Prerequisites**:

   - Minecraft server running **Spigot/Paper 1.21** or compatible.
   - **Vault** plugin installed for economy integration (required for `/welcome` command rewards).

2. **Steps**:

   - Build the plugin  from source.
   - Place the `PlayerWelcomer.jar` file in your server's `plugins` folder.
   - Restart your server or use `/restart` to load the plugin.
   - The plugin will automatically generate `config.yml` and `data.yml` in the `plugins/PlayerWelcomer` folder.

3. **Verify Installation**:

   - Check the server console for the message: `PlayerWelcomer enabled successfully!`.
   - If Vault is not detected, a warning will appear, and the reward system will be disabled.

## Configuration

The plugin is configured via the `config.yml` file located in `plugins/PlayerWelcomer`. Below is the default configuration with explanations:

```yaml
first-join:
  enabled: true
  message:
    line1: "&8&m===================="
    line2: "&a&lWelcome &e%player_name% &7to the server!"
    line3: "&7First join! &8[&c%unique_join_count%&8]"
    line4: "&8&m===================="

welcome-command:
  enabled: true
  cooldown: 60
  reward-amount: 100.0
  welcome-message: "&aWelcome to the server, &e%target_name%&a! Welcomed by &e%player_name%"
  success-message: "&aYou welcomed a new player and received %reward_amount% coins!"
  no-new-players: "&cThat player has already been welcomed!"
  cooldown-message: "&cPlease wait %seconds% seconds before using this command again!"
```

### Configuration Options

- **first-join.enabled**: Enable or disable the first-join welcome message (`true`/`false`).
- **first-join.message**: Multi-line message broadcast when a new player joins. Supports placeholders:
  - `%player_name%`: The joining player's name.
  - `%unique_join_count%`: The total number of unique players who have joined.
- **welcome-command.enabled**: Enable or disable the `/welcome` command (`true`/`false`).
- **welcome-command.cooldown**: Cooldown in seconds for the `/welcome` command after a successful welcome.
- **welcome-command.reward-amount**: Amount of in-game currency (via Vault) awarded for a successful `/welcome`.
- **welcome-command.welcome-message**: Message broadcast when a player uses `/welcome`. Supports:
  - `%target_name%`: The welcomed player's name.
  - `%player_name%`: The name of the player executing the command.
- **welcome-command.success-message**: Message sent to the player who executed `/welcome`. Supports `%reward_amount%`.
- **welcome-command.no-new-players**: Message when the target has already been welcomed.
- **welcome-command.cooldown-message**: Message when the command is on cooldown. Supports `%seconds%`.

**Note**: Messages support Minecraft color codes (e.g., `&a` for green). Use `&` followed by a color code or formatting code (e.g., `&l` for bold).

## Usage

### First-Join Message

When a new player joins the server, a multi-line welcome message is broadcast to all players if `first-join.enabled` is `true`. Example output:

```
====================
Welcome Steve to the server!
First join! [5]
====================
```

### /welcome Command

Players can use `/welcome <player>` to send a personalized welcome message to a new player within 60 seconds of their join. The command:

- Cannot be used on oneself.
- Only works on new players who haven't been welcomed.
- Applies a cooldown only after a successful welcome.
- Rewards the executor with in-game currency (if Vault is installed).

**Example**:

- Command: `/welcome Steve`
- Broadcast: `Welcome to the server, Steve! Welcomed by Alex`
- Sender receives: `You welcomed a new player and received 100.0 coins!`

**Error Messages**:

- If the target is not found: `Player not found!`
- If the target is not new: `That player has already been welcomed!`
- If the welcome window has expired: `This player can no longer be welcomed!`
- If on cooldown: `Please wait 45 seconds before using this command again!`

## Data Storage

The plugin stores persistent data in `data.yml`:

- **unique-join-count**: Tracks the total number of unique players who have joined.
- **welcomed-players**: List of UUIDs of players who have been welcomed.

Data operations are performed asynchronously to ensure no impact on server performance.


