# Java Chat Application

## Project Overview

The Java Chat Application is a client-server-based messaging system developed using Java. It allows multiple users to communicate in real time through a graphical user interface (GUI).

The application uses multithreading, socket programming, synchronization, and Java Collections to support multiple simultaneous users.

## Features

- User registration and login authentication
- Secure password hashing
- Multiple users can connect simultaneously
- Public messaging
- Private messaging between users
- Group creation and group messaging
- Date and time displayed with messages
- Message history stored in a file
- Server activity logging
- Java Swing graphical user interface
- Multithreading for handling multiple clients

## Technologies Used

- Java
- Java Swing
- Socket Programming
- Multithreading
- Java Collections Framework
- File Handling
- Git and GitHub

## Project Structure

```text
JavaChatApplication/
├── src/
│   ├── auth/
│   │   └── AuthService.java
│   ├── client/
│   │   └── ChatClient.java
│   ├── gui/
│   │   ├── LoginFrame.java
│   │   └── ChatFrame.java
│   ├── server/
│   │   ├── ChatServer.java
│   │   ├── ClientHandler.java
│   │   └── ServerLogger.java
│   └── storage/
│       └── MessageHistory.java
├── data/
│   ├── users.txt
│   └── messages.txt
├── logs/
│   └── server.log
├── out/
└── README.md
```

## Requirements

- Java Development Kit (JDK)
- Visual Studio Code or another Java IDE
- Terminal

## How to Run

### 1. Clone the repository

```bash
git clone YOUR_GITHUB_REPOSITORY_URL
```

### 2. Navigate to the project directory

```bash
cd JavaChatApplication
```

### 3. Compile the project

```bash
mkdir -p out

javac -d out src/auth/*.java src/server/*.java src/client/*.java src/storage/*.java src/gui/*.java
```

### 4. Start the server

Open a terminal and execute:

```bash
java -cp out server.ChatServer
```

### 5. Start the client GUI

Open another terminal and execute:

```bash
java -cp out gui.LoginFrame
```

Multiple clients can be started in separate terminal windows.

## Chat Commands

| Command | Description |
|---|---|
| `/msg username message` | Send a private message |
| `/create groupname` | Create a group |
| `/join groupname` | Join a group |
| `/group groupname message` | Send a group message |
| `/logout` | Log out |

## Data Storage

- `users.txt` stores registered usernames and password hashes.
- `messages.txt` stores chat messages with timestamps.
- `server.log` stores server activity and connection logs.

## Learning Outcomes

This project demonstrates practical implementation of:

- Client-server architecture
- TCP socket communication
- Multithreading and synchronization
- Java Collections Framework
- GUI development using Swing
- File handling and persistent message storage
- User authentication

## Future Enhancements

- Display online users in the GUI
- Add message history viewing inside the GUI
- Store user and group information in a database
- Add profile pictures and improved chat interface

## Conclusion

The Java Chat Application demonstrates real-time communication between multiple users using Java networking, multithreading, and graphical user interface development.
