# 🪐 AliienCommunityQuests

A high-performance, fully asynchronous community quest system built for modern Minecraft servers. Engineered from the ground up to support massive player bases, real-time objective tracking, and flawless Folia compatibility.

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Folia Supported](https://img.shields.io/badge/Platform-Folia_|_Paper-brightgreen.svg)](https://papermc.io/)
[![Support](https://img.shields.io/badge/Discord-Join_Server-7289DA.svg)](https://discord.gg/K7RKrWBaV7)

---

## ✨ Features

* **Smart Rotation Engine:** An automated background task seamlessly tracks quest expirations, purges old data, and generates fresh objectives from the configuration pool without any admin intervention.
* **Modern Database Infrastructure:** Powered by HikariCP for connection pooling. Natively supports **H2, SQLite, MySQL, and MariaDB**. Every single read and write query is executed asynchronously.
* **Dynamic Graphical Interface:** A fully paginated GUI that natively supports custom model data, expanding list placeholders, and real-time progress tracking, all protected by strict shift-click and dupe-protection locks.
* **Several Objective Options:** Hooks into vanilla events such as block break, entity kill, player fish, etc, as well as into events from other plugins (check the documentation for more details on this)

---

## 🚀 Getting Started

### Prerequisites
* **Java 21** or higher.
* **Paper / Folia 1.21** or higher.

### Installation
1. Drop the `AliienCommunityQuests.jar` into your server's `plugins` folder.
2. Start the server to generate the default configuration files.
3. Configure your database credentials in `settings.yml`.
4. Define your custom objectives and rewards in `quests.yml`.
5. Restart the server or run `/cq admin reload` to apply changes!

---

## 📚 Documentation

Detailed guides on creating custom quests, configuring rewards, formatting the GUI menus, and setting up multi-server MySQL databases can be found on our official Wiki.

👉 **[Read the Official Wiki Here](https://aliien.gitbook.io/aliien-docs)**

---

## 💻 Commands & Permissions

Powered by Aikar's Command Framework (ACF) for lightning-fast command execution and dynamic tab-completion.

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/cq` | `aliien.communityquests.menu` | Opens the main active quests menu. |
| `/cq admin reload` | `aliien.communityquests.admin.reload` | Reloads configurations and messages without restarting. |
| `/cq admin reset <id>` | `aliien.communityquests.admin.reset` | Forcefully wipes an active quest and triggers a new one. |

*Note: Granting `aliien.communityquests.admin` provides access to all admin subcommands, and `aliien.communityquests.*` grants master access to the entire plugin.*

---

## 💬 Support

Need help configuring your quests or setting up your database? Join our community!

👉 **[Join the Aliien's Plugins Discord Server](https://discord.gg/K7RKrWBaV7)**

---
*Built with [AliienCore](https://github.com/aliien15/AliienCore)*