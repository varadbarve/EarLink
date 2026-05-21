import asyncio
import json
import websockets
import logging
from service.action_executor import execute_command

logger = logging.getLogger("EarLink")

async def handler(websocket):
    client_address = websocket.remote_address
    logger.info(f"Client connected from {client_address}")
    try:
        async for message in websocket:
            logger.info(f"Received raw message: {message}")
            try:
                data = json.loads(message)
                command = data.get("command")
                if command:
                    execute_command(command)
                else:
                    logger.warning("No command field in received JSON")
            except json.JSONDecodeError:
                logger.error("Failed to parse message as JSON")
            except Exception as e:
                logger.error(f"Error handling message: {e}")
    except websockets.exceptions.ConnectionClosed:
        logger.info(f"Client disconnected: {client_address}")
    except Exception as e:
        logger.error(f"Connection error: {e}")

async def start_server(host, port):
    logger.info(f"Starting WebSocket server on ws://{host}:{port}")
    async with websockets.serve(handler, host, port):
        await asyncio.Future()  # run forever
