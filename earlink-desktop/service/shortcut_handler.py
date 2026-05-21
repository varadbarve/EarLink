import pyautogui
import logging

logger = logging.getLogger("EarLink")

def press_shortcut(keys):
    """
    Simulates pressing a combination of keys.
    keys: list of keys, e.g. ['ctrl', 'shift', 'm']
    """
    try:
        logger.info(f"Simulating shortcut: {' + '.join(keys)}")
        pyautogui.hotkey(*keys)
    except Exception as e:
        logger.error(f"Failed to press shortcut: {e}")
