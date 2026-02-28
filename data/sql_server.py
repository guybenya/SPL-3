#!/usr/bin/env python3
"""
Basic Python Server for STOMP Assignment – Stage 3.3

IMPORTANT:
DO NOT CHANGE the server name or the basic protocol.
Students should EXTEND this server by implementing
the methods below.
"""

import socket
import sys
import threading

# --- NEW CODE ---
import sqlite3
# ----------------

SERVER_NAME = "STOMP_PYTHON_SQL_SERVER"  # DO NOT CHANGE!
DB_FILE = "stomp_server.db"              # DO NOT CHANGE!


def recv_null_terminated(sock: socket.socket) -> str:
    data = b""
    while True:
        chunk = sock.recv(1024)
        if not chunk:
            return ""
        data += chunk
        if b"\0" in data:
            msg, _ = data.split(b"\0", 1)
            return msg.decode("utf-8", errors="replace")

# --- OLD CODE ---
# def init_database():
#     pass
# ----------------

# --- NEW CODE ---
def init_database():
    """
    Creates the necessary tables for the Stomp Server if they do not exist.
    """
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()
    
    # Create Users table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS users (
            username TEXT PRIMARY KEY,
            password TEXT NOT NULL,
            registration_date TEXT
        )
    ''')
    
    # Create Login History table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS login_history (
            username TEXT,
            login_time TEXT,
            logout_time TEXT
        )
    ''')
    
    # Create File Tracking table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS file_tracking (
            username TEXT,
            filename TEXT,
            upload_time TEXT,
            game_channel TEXT
        )
    ''')
    
    conn.commit()
    conn.close()
# ----------------


# --- OLD CODE ---
# def execute_sql_command(sql_command: str) -> str:
#     return "done"
# ----------------

# --- NEW CODE ---
def execute_sql_command(sql_command: str) -> str:
    """
    Executes INSERT / UPDATE / DELETE commands.
    """
    try:
        conn = sqlite3.connect(DB_FILE)
        cursor = conn.cursor()
        cursor.execute(sql_command)
        conn.commit()
        conn.close()
        return "SUCCESS"
    except Exception as e:
        return f"ERROR: {e}"
# ----------------


# --- OLD CODE ---
# def execute_sql_query(sql_query: str) -> str:
#     return "done"
# ----------------

# --- NEW CODE ---
def execute_sql_query(sql_query: str) -> str:
    """
    Executes SELECT queries and formats the output for the Java server.
    The Java Database.java expects results separated by '|'.
    """
    try:
        conn = sqlite3.connect(DB_FILE)
        cursor = conn.cursor()
        cursor.execute(sql_query)
        rows = cursor.fetchall()
        conn.close()
        
        # Format the result with "|" separator as expected by Database.java
        result = "SUCCESS"
        for row in rows:
            # row is a tuple, str(row) converts it to string like "('meni', '2025-01-01')"
            result += f"|{str(row)}"
        return result
    except Exception as e:
        return f"ERROR: {e}"
# ----------------


def handle_client(client_socket: socket.socket, addr):
    print(f"[{SERVER_NAME}] Client connected from {addr}")

    try:
        while True:
            message = recv_null_terminated(client_socket)
            if message == "":
                break

            print(f"[{SERVER_NAME}] Received:")
            print(message)

            # --- OLD CODE ---
            # client_socket.sendall(b"done\0")
            
            # --- NEW CODE ---
            # Determine if the message is a SELECT query or a command (INSERT/UPDATE)
            if message.strip().upper().startswith("SELECT"):
                response = execute_sql_query(message)
            else:
                response = execute_sql_command(message)
                
            # Send the response back to Java, terminated by a null character
            client_socket.sendall((response + '\0').encode('utf-8'))
            # ----------------

    except Exception as e:
        print(f"[{SERVER_NAME}] Error handling client {addr}: {e}")
    finally:
        try:
            client_socket.close()
        except Exception:
            pass
        print(f"[{SERVER_NAME}] Client {addr} disconnected")


def start_server(host="127.0.0.1", port=7778):
    
    # --- NEW CODE ---
    # Initialize database tables before accepting connections
    init_database()
    print(f"[{SERVER_NAME}] Database initialized successfully.")
    # ----------------
    
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    try:
        server_socket.bind((host, port))
        server_socket.listen(5)
        print(f"[{SERVER_NAME}] Server started on {host}:{port}")
        print(f"[{SERVER_NAME}] Waiting for connections...")

        while True:
            client_socket, addr = server_socket.accept()
            t = threading.Thread(
                target=handle_client,
                args=(client_socket, addr),
                daemon=True
            )
            t.start()

    except KeyboardInterrupt:
        print(f"\n[{SERVER_NAME}] Shutting down server...")
    finally:
        try:
            server_socket.close()
        except Exception:
            pass


if __name__ == "__main__":
    port = 7778
    if len(sys.argv) > 1:
        raw_port = sys.argv[1].strip()
        try:
            port = int(raw_port)
        except ValueError:
            print(f"Invalid port '{raw_port}', falling back to default {port}")

    start_server(port=port)