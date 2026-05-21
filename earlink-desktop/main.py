import asyncio
import logging
import socket
from service.websocket_server import start_server

# Setup Logging with readable formats
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S"
)
logger = logging.getLogger("EarLink")

def get_local_ip():
    try:
        # Get local IP address by connecting dummy socket
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        local_ip = s.getsockname()[0]
        s.close()
        return local_ip
    except Exception:
        return "127.0.0.1"

async def main():
    host = "0.0.0.0"
    port = 8765
    local_ip = get_local_ip()
    
    print("=" * 60)
    print("  EARLINK DESKTOP BRIDGE SERVICE  ")
    print(f"  Local Network IP: {local_ip}")
    print(f"  Listening Port  : {port}")
    print("=" * 60)
    
    await start_server(host, port)

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nEarLink Bridge stopped.")
