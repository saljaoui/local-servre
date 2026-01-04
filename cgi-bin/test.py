#!/usr/bin/env python3
import json
print("Content-Type: application/json")
print()
print(json.dumps({"ok": True, "msg": "hello"}))
