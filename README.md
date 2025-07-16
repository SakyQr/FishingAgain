# FishingAgain

> ⚠️ **BETA VERSION** - This plugin is currently in beta development. Features may be incomplete, unstable, or subject to change. Use at your own risk on production servers.

A comprehensive Minecraft fishing plugin that enhances the default fishing experience with skill checks, custom items, economy integration, and detailed statistics tracking.

## Features

### 🎣 Enhanced Fishing Mechanics
- **Skill Check System**: Interactive fishing with right-click skill checks for more engaging gameplay
- **Custom Fish Items**: Support for custom fishing items with configurable difficulty levels
- **Fishing Status Tracking**: Real-time tracking of player fishing activities

### 📊 Statistics & Records
- **Fish Statistics**: Detailed tracking of caught fish for each player
- **Fishing Records**: Personal best records and achievements
- **Discovery System**: GUI-based fish discovery tracker

### 💰 Economy Integration
- **Vault Support**: Full integration with Vault economy systems
- **Sell Menu**: Dedicated GUI for selling caught fish
- **Configurable Prices**: Customizable fish values and economy settings

### 🎮 User Interface
- **Discovery GUI**: Browse and track discovered fish species
- **Sell Menu**: User-friendly interface for selling fish
- **Multilingual Support**: Localization system for multiple languages

### ⚡ Experience System
- **Fishing Experience**: Gain experience points from successful catches
- **Skill Progression**: Level-based fishing system with benefits
- **Persistent Data**: Player progress saved between sessions

## Dependencies

### Required
- **Spigot/Paper**: Minecraft server implementation
- **Vault**: Economy system integration

### Optional
- Any Vault-compatible economy plugin (EssentialsX, etc.)

## Installation

⚠️ **Beta Warning**: This plugin is in active development. Backup your server before installation.

1. Download the latest beta release of FishingAgain
2. Place the `.jar` file in your server's `plugins` folder
3. Install Vault and a compatible economy plugin
4. Start/restart your server
5. Configure the plugin settings in `config.yml`

### Beta Considerations
- Features may not work as expected
- Configuration format may change between versions
- Player data structure may be updated (backup recommended)
- Some features may be incomplete or disabled

## Configuration

The plugin creates a `config.yml` file with version tracking. The current configuration version is `1.0`.

### Basic Setup
```yaml
config-version: 1.0
# Additional configuration options will be generated
```

## Commands

### Main Command
- `/fishing` - Main fishing command with subcommands

### Available Subcommands
- Access to sell menu functionality
- Discovery GUI navigation
- Experience and statistics management

## Permissions

The plugin integrates with standard Minecraft permission systems. Specific permissions will depend on your server's permission plugin configuration.

## Beta Status & Known Issues

### Current Beta Features
- ✅ Basic skill check system
- ✅ Custom fish item support
- ✅ Vault economy integration
- ✅ Experience tracking
- ✅ Statistics management
- ✅ Discovery GUI framework

### Known Beta Limitations
- 🔄 Some GUI elements may be incomplete
- 🔄 Configuration options may be limited
- 🔄 Error handling may need improvement
- 🔄 Documentation is work-in-progress

### Reporting Issues
This is a beta version - please report any bugs, crashes, or unexpected behavior to help improve the plugin.
