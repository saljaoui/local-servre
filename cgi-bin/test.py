#!/usr/bin/env python3
import json
print("Content-Type: application/json")
print()
print(json.dumps({"ok": True, "msg": "hello"}))
for i range 1000000000 {
    print(i)
}
