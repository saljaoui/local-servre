#!/usr/bin/env python3
import os
sid = os.environ.get("HTTP_COOKIE", "")
print("Content-Type: text/plain\r\n")
print(f"SID header: {sid}")
