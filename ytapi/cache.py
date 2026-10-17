"""Cache mémoire simple (TTL) : évite de relancer une requête YouTube identique."""
import time

_store = {}
TTL = 3600


def get(key):
    item = _store.get(key)
    if not item:
        return None
    val, exp = item
    if time.time() > exp:
        _store.pop(key, None)
        return None
    return val


def set(key, value, ttl=TTL):
    _store[key] = (value, time.time() + ttl)


def clear():
    _store.clear()
