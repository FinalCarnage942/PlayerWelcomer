# PlayerWelcomer

**PlayerWelcomer** is a lightweight, high-performance Minecraft plugin designed to enhance the new player experience on your server. It provides customizable first-join welcome messages and a `/welcome` command to allow players to personally greet newcomers, with configurable rewards and cooldowns. Built with clean code principles and optimized for real-time performance, this plugin supports multiple currency plugins (Vault, PlayerPoints, CoinsEngine) and crate key rewards (Excellent Crates), making it ideal for community-driven servers aiming to create a welcoming atmosphere.

## Features

- **First-Join Welcome Message**: Broadcasts a visually appealing, multi-line message to all players when a new player joins the server for the first time. Supports placeholders for player name and unique join count.
- **/welcome Command**: Allows players to send a personalized welcome message to new players within a 60-second window after they join. Includes configurable rewards (currency or crate keys) and a cooldown system.
- **Dynamic New Player Detection**: Tracks new players and ensures the `/welcome` command is only usable on players who haven't been welcomed yet.
- **Multiple Currency Support**: Integrates with Vault (e.g., EssentialsX), PlayerPoints, and CoinsEngine for flexible currency rewards.
- **Crate Key Rewards**: Supports Excellent Crates for rewarding crate keys.
- **Comprehensive Configuration**: Fully customizable messages, reward types, currency selection, amounts, and cooldowns via `config.yml`.
- **Performance Optimized**: Asynchronous execution for all potentially blocking operations (e.g., file I/O, economy transactions) to prevent server lag.
- **Robust and Maintainable**: Follows SOLID principles and clean code practices, with comprehensive error handling and logging.

## Installation

1. **Prerequisites**:

   - Minecraft server running **Spigot/Paper 1.21** or compatible.
   - **Vault** and an economy plugin (e.g., EssentialsX) for `vault` currency rewards.
   - **PlayerPoints** for `playerpoints` currency rewards.
   - **CoinsEngine** for `coinsengine:<currency_id>` currency rewards.
   - **Excellent Crates** for crate key rewards (optional).

2. **Steps**:

   - Download the latest `PlayerWelcomer.jar` from the releases page or build it from source.
   - Place the `PlayerWelcomer.jar` file in your server's `plugins` folder.
   - Install required dependency plugins (Vault, PlayerPoints, CoinsEngine, or Excellent Crates) based on your reward configuration.
   - Restart your server or use `/reload` to load the plugin.
   - The plugin will automatically generate `config.yml` and `data.yml` in the `plugins/PlayerWelcomer` folder.

3. **Verify Installation**:

   - Check the server console for the message: `PlayerWelcomer enabled successfully!`.
   - If a configured currency plugin (e.g., Vault, PlayerPoints) is not detected, a warning will appear, and rewards may fall back to crate keys or be disabled.

## Configuration

The plugin is configured via the `config.yml` file located in `plugins/PlayerWelcomer`. Below is the default configuration with explanations:

```yaml
# PlayerWelcomer Configuration
# Configure welcome messages and rewards for new players.
# Supports hex color codes (e.g., #FF0000) and legacy Minecraft color codes (e.g., &a).

first-join:
  enabled: true # Enable/disable first-join welcome messages (true/false)
  line_count: 5 # Number of message lines (must match the number of lines below, including empty ones)
  message:
    line1: "#808080&m====================" # Top border (gray, strikethrough)
    line2: "" # Empty line for spacing
    line3: "#00FF00&lWelcome #FFFF00%player_name% #808080to the server! #808080[&f#%unique_join_count%#808080]" # Welcome message with player name and join count
    line4: "" # Empty line for spacing
    line5: "#808080&m====================" # Bottom border (gray, strikethrough)

welcome-command:
  enabled: true # Enable/disable the /welcome command (true/false)
  cooldown: 60 # Cooldown in seconds after a successful /welcome
  # Reward type for the /welcome command. Choose one:
  # - "currency": Gives in-game currency via a selected currency plugin
  # - "crate_key": Gives crate keys via Excellent Crates (requires Excellent Crates plugin)
  reward-type: "currency"
  # Currency type for currency rewards. Choose one:
  # - "vault": Uses Vault with an economy plugin (e.g., EssentialsX for coins)
  # - "playerpoints": Uses PlayerPoints for points
  # - "coinsengine:<currency_id>": Uses CoinsEngine with a specific currency (e.g., "coinsengine:gold")
  reward-currency: "playerpoints"
  # Amount of reward:
  # - For "currency", specifies the amount of money/points (e.g., 100.0 for 100 coins, 10 for 10 points)
  # - For "crate_key", specifies the number of keys (must be a positive integer, e.g., 1)
  reward-amount: 100.0
  # Crate key ID from Excellent Crates (e.g., "Test Key"). Required only if reward-type is "crate_key".
  # Can be omitted if reward-type is "currency".
  crate-key-name: "Test Key"
  welcome-message: "#00FF00Welcome to the server, #FFFF00%target_name%#00FF00! Welcomed by #FFFF00%player_name%" # Broadcast message for /welcome
  # Success message shown to the player who used /welcome. Adapts to reward-type:
  # - For "currency": Shows amount and currency name (e.g., "100 coins", "10 points", "100 Gold")
  # - For "crate_key": Shows amount and key name (e.g., "1 Test Key key(s)")
  success-message: "#00FF00You welcomed a new player and received #ADD8E6%reward_amount% %reward_display%!" # Use %reward_amount% and %reward_display%
  no-new-players: "#FF0000That player has already been welcomed!" # Message when target is not a new player
  cooldown-message: "#FF0000Please wait %seconds% seconds before using this command again!" # Message when on cooldown
```

### Configuration Options

- **first-join.enabled**: Enable or disable the first-join welcome message (`true`/`false`).
- **first-join.line_count**: Total number of message lines, including empty ones. Must match the number of defined lines.
- **first-join.message**: Multi-line message broadcast when a new player joins. Supports:
  - `%player_name%`: The joining player's name.
  - `%unique_join_count%`: The total number of unique players who have joined.
  - Hex color codes (e.g., `#FF0000` for red) and legacy color codes (e.g., `&a` for green, `&m` for strikethrough).
- **welcome-command.enabled**: Enable or disable the `/welcome` command (`true`/`false`).
- **welcome-command.cooldown**: Cooldown in seconds for the `/welcome` command after a successful welcome.
- **welcome-command.reward-type**: Reward type for the `/welcome` command:
  - `"currency"`: Gives in-game currency via the plugin specified in `reward-currency`.
  - `"crate_key"`: Gives crate keys via Excellent Crates.
- **welcome-command.reward-currency**: Currency type for `currency` rewards:
  - `"vault"`: Uses Vault with an economy plugin (e.g., EssentialsX for coins).
  - `"playerpoints"`: Uses PlayerPoints for points.
  - `"coinsengine:<currency_id>"`: Uses CoinsEngine with a specific currency (e.g., `coinsengine:gold`).
- **welcome-command.reward-amount**: Amount of reward:
  - For `"currency"`, specifies the amount of money/points (e.g., `100.0` for coins, `10` for points).
  - For `"crate_key"`, specifies the number of keys (must be a positive integer, e.g., `1`).
- **welcome-command.crate-key-name**: Crate key ID from Excellent Crates (e.g., `Test Key`). Required for `crate_key` rewards, optional for `currency`.
- **welcome-command.welcome-message**: Message broadcast when a player uses `/welcome`. Supports:
  - `%target_name%`: The welcomed player's name.
  - `%player_name%`: The name of the player executing the command.
- **welcome-command.success-message**: Message sent to the player who executed `/welcome`. Supports:
  - `%reward_amount%`: The amount of reward.
  - `%reward_display%`: The currency name (e.g., `coins`, `points`, `Gold`) or key name (e.g., `Test Key key(s)`).
- **welcome-command.no-new-players**: Message when the target has already been welcomed.
- **welcome-command.cooldown-message**: Message when the command is on cooldown. Supports `%seconds%`.

**Note**: Messages support hex color codes (e.g., `#FF0000`) and legacy Minecraft color codes (e.g., `&a` for green, `&l` for bold).

## Usage

### First-Join Message

When a new player joins the server, a multi-line welcome message is broadcast to all players if `first-join.enabled` is `true`. Example output:

```
====================
Welcome Steve to the server! [#5]
====================
```

### /welcome Command

Players can use `/welcome <player>` to send a personalized welcome message to a new player within 60 seconds of their join. The command:

- Cannot be used on oneself.
- Only works on new players who haven't been welcomed.
- Applies a cooldown only after a successful welcome.
- Rewards the executor with currency (via Vault, PlayerPoints, or CoinsEngine) or crate keys (via Excellent Crates).

**Example**:

- Command: `/welcome Steve`
- Broadcast: `Welcome to the server, Steve! Welcomed by Alex`
- Sender receives: `You welcomed a new player and received 100 points!` (if `reward-currency: "playerpoints"`)

**Error Messages**:

- If the target is not found: `Player not found!`
- If the target is not new: `That player has already been welcomed!`
- If the welcome window has expired: `This player can no longer be welcomed!`
- If on cooldown: `Please wait 45 seconds before using this command again!`
- If reward fails: `Failed to give reward. Contact an administrator.`

## Data Storage

The plugin stores persistent data in `data.yml`:

- **unique-join-count**: Tracks the total number of unique players who have joined.
- **welcomed-players**: List of UUIDs of players who have been welcomed.

Data operations are performed asynchronously to ensure no impact on server performance.

## Dependencies

- **Vault** (1.7+): Required for `vault` currency rewards (e.g., with EssentialsX).
- **PlayerPoints** (3.2+): Required for `playerpoints` currency rewards.
- **CoinsEngine** (2.12+): Required for `coinsengine:<currency_id>` currency rewards.
- **Excellent Crates**: Required for `crate_key` rewards (optional).
- **Java 21+**: Required for CoinsEngine compatibility.
