import os
import subprocess
import pyautogui
import logging

logger = logging.getLogger("EarLink")

def volume_mute():
    pyautogui.press('volumemute')

def volume_up():
    pyautogui.press('volumeup')

def volume_down():
    pyautogui.press('volumedown')

def lock_workstation():
    # Lock Windows PC
    os.system("rundll32.exe user32.dll,LockWorkStation")

def run_script(script_path):
    try:
        logger.info(f"Running script: {script_path}")
        subprocess.Popen(script_path, shell=True)
    except Exception as e:
        logger.error(f"Failed to run script: {e}")
