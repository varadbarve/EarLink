import logging
import os
import json
from actions import discord, spotify, system

logger = logging.getLogger("EarLink")

def load_scripts_config():
    # Resolve path relative to action_executor.py directory
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    config_path = os.path.join(base_dir, "config", "scripts.json")
    if not os.path.exists(config_path):
        logger.warning(f"Scripts configuration file not found at {config_path}")
        return {}
    try:
        with open(config_path, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        logger.error(f"Failed to read scripts configuration: {e}")
        return {}

def execute_command(command_str):
    command = command_str.lower().strip()
    logger.info(f"Executing command: {command}")
    
    if command == "mute_discord":
        discord.mute_discord()
    elif command == "play_pause":
        spotify.play_pause()
    elif command == "next_track":
        spotify.next_track()
    elif command == "prev_track":
        spotify.prev_track()
    elif command == "volume_up":
        system.volume_up()
    elif command == "volume_down":
        system.volume_down()
    elif command == "volume_mute":
        system.volume_mute()
    elif command == "lock_pc":
        system.lock_workstation()
    elif command.startswith("run_script:"):
        script_id = command_str[len("run_script:"):].strip()
        scripts = load_scripts_config()
        script_command = scripts.get(script_id) or scripts.get(script_id.lower())
        if script_command:
            logger.info(f"Resolved script ID '{script_id}' to: {script_command}")
            system.run_script(script_command)
        else:
            logger.warning(f"Script ID '{script_id}' is not registered in config/scripts.json or is invalid. Execution blocked.")
    else:
        logger.warning(f"Unknown desktop command received: {command_str}")
