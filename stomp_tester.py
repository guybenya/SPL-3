import socket
import time

# Helper function to format and send a STOMP frame
def send_frame(sock, command, headers=None, body=""):
    # [cite_start]A STOMP frame starts with a command [cite: 31]
    frame = command + "\n"
    if headers:
        for key, value in headers.items():
            # [cite_start]Headers are in key:value format terminated by newline [cite: 33]
            frame += f"{key}:{value}\n"
    
    # [cite_start]A blank line indicates end of headers [cite: 42]
    frame += "\n" + body
    
    # [cite_start]Every frame must be terminated by the null character '\0' [cite: 43]
    sock.sendall((frame + '\0').encode('utf-8'))
    print(f">>> SENT {command}")

# Helper function to receive and print a STOMP frame
def receive_frame(sock):
    data = b""
    while b'\x00' not in data:
        chunk = sock.recv(1024)
        if not chunk: break
        data += chunk
    if data:
        # Decode and remove the null character for printing
        decoded = data.decode('utf-8').replace('\0', '')
        print(f"<<< RECEIVED:\n{decoded}\n" + "="*20)
        return decoded
    return None

def run_test():
    host = '127.0.0.1'
    port = 7777 # Make sure this matches the port you give the server
    
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.connect((host, port))
            print(f"Connected to server at {host}:{port}")

            # [cite_start]1. SEND CONNECT [cite: 51-57]
            send_frame(s, "CONNECT", {
                "accept-version": "1.2",
                "host": "stomp.cs.bgu.ac.il",
                "login": "guy",
                "passcode": "spl2026"
            })
            receive_frame(s) # Expect: CONNECTED

            # [cite_start]2. SEND SUBSCRIBE [cite: 141-144]
            # [cite_start]We add a receipt header to get confirmation [cite: 178]
            send_frame(s, "SUBSCRIBE", {
                "destination": "/topic/world_cup",
                "id": "sub-001",
                "receipt": "73"
            })
            receive_frame(s) # Expect: RECEIPT for 73

            # [cite_start]3. SEND MESSAGE [cite: 131-133]
            send_frame(s, "SEND", {"destination": "/topic/world_cup"}, "Goal for Argentina!")
            # [cite_start]Since we are subscribed, the server should broadcast it back to us [cite: 138]
            receive_frame(s) # Expect: MESSAGE

            # [cite_start]4. DISCONNECT [cite: 163-164]
            # [cite_start]Disconnect MUST contain a receipt header [cite: 179]
            send_frame(s, "DISCONNECT", {"receipt": "99"})
            receive_frame(s) # Expect: RECEIPT for 99
            
            print("Test finished successfully!")

    except Exception as e:
        print(f"Error during test: {e}")

if __name__ == "__main__":
    run_test()