# Java Messaging + Social Feed Desktop App

A desktop LAN application built with Java Swing, sockets, threads, JDBC, and MySQL. The project combines real-time messaging with a DB-backed social feed in a single desktop client.

## Project Overview

This application lets users register, log in, chat with other users over the local network, and interact with a shared social feed. The chat system is socket-based and updates in real time, while the feed is stored in MySQL and refreshed from the database.

The app is organized as a layered desktop application:

`Swing UI -> Service layer -> DAO / Repository layer -> MySQL database`

The network side is separated from the persistence layer so chat traffic and feed data follow different flows.

## Main Features

- User registration and login
- SHA-256 password hashing
- Real-time public chat
- Private 1-to-1 messaging
- Room-based chat
- Online users list
- Chat history loading for rooms and private conversations
- Create posts in a global feed
- Like and unlike posts
- Add comments under posts
- Auto-refreshing feed with draft-preserving behavior
- Desktop Swing interface with separate Chat and Feed views

## Tech Stack

- Java 21+ for the application runtime and language features
- Java Swing for the desktop UI
- Java Sockets for client/server chat communication
- Java Threads for continuous read/write handling in chat
- JDBC for database access
- MySQL 8+ for users, messages, posts, likes, and comments
- SHA-256 via MessageDigest for password hashing

## How the Technologies Are Used

- Swing builds the login screen, chat tabs, user list, and feed cards.
- Sockets keep the chat room, private chat, and online users list synchronized in real time.
- Threads separate socket reading and writing so the UI stays responsive.
- JDBC stores and retrieves authentication data, chat history, posts, likes, and comments.
- MySQL acts as the persistence layer for user accounts and the social feed.
- SHA-256 protects passwords before they are written to the database.

## Architecture

The codebase follows a strict separation of concerns:

- UI components handle display and user interaction.
- Service classes contain application rules and orchestration.
- DAO classes encapsulate SQL and database access.
- Network classes manage socket communication.
- Utility classes hold shared helpers such as password hashing.

## Database Tables

The project uses these core tables in the default database `messaging_app`:

- `users` for registered accounts
- `messages` for chat history
- `posts` for feed posts
- `post_likes` for reactions
- `post_comments` for comments

## Setup

### Prerequisites

- JDK 17 or newer
- MySQL 8 or newer
- MySQL JDBC driver in `lib/`

### Steps

1. Configure database credentials in `src/database/DatabaseConfig.java`.
2. Make sure the MySQL JDBC JAR is inside `lib/`.
3. Compile the project:

```bash
mkdir -p out
javac -cp "lib/*" -d out $(find src -name "*.java")
```

4. Start the chat server:

```bash
java -cp "out:lib/*" app.Main server 5000
```

5. Start the desktop client:

```bash
java -cp "out:lib/*" app.Main
```

## Usage

- Register or log in from the login screen.
- Use the Chat panel to send public, room, or private messages.
- Click a user to open a private conversation.
- Join a room by entering its name and selecting Join Room.
- Use the Feed panel to create posts, like posts, and add comments.

## Behavior Notes

- Chat is real-time and socket-based.
- Feed data comes from the database and is independent from chat.
- Feed refresh pauses while the user is typing.
- Comment drafts and expanded comment sections are preserved during refresh.

## Troubleshooting

- If login fails, check the username, password, and hashed value stored in `users`.
- If chat does not connect, confirm the server is running and the host/port match.
- If posting fails, verify the database connection, schema, and JDBC driver.

## Extensibility Ideas

- Media attachments in posts
- Notifications
- User profiles
- Post pagination
- Room membership persistence
- Message delivery receipts
