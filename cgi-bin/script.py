
#!/usr/bin/env python3
import os
import sys
import html
from urllib.parse import parse_qs

MS_PER_CHAR = 50
COUNT_SPACES = True

TEMPLATE_PATH = os.path.join(os.path.dirname(__file__), "output_cgi.html")

def read_params():
    method = os.environ.get("REQUEST_METHOD", "GET").upper()

    if method == "GET":
        qs = os.environ.get("QUERY_STRING", "")
        return parse_qs(qs)

    if method == "POST":
        try:
            length = int(os.environ.get("CONTENT_LENGTH", "0"))
        except ValueError:
            length = 0
        body = sys.stdin.read(length) if length > 0 else ""
        return parse_qs(body)

    return {}

def load_template():
    try:
        with open(TEMPLATE_PATH, "r", encoding="utf-8") as f:
            return f.read()
    except Exception:
        return "<html><body><h1>CGI Result</h1><p>{{MESSAGE}}</p></body></html>"

params = read_params()
msg = params.get("msg", [""])[0]
msg = msg.replace("\r\n", "\n")

count_source = msg if COUNT_SPACES else "".join(msg.split())
x = len(count_source)

time_ms = x * MS_PER_CHAR
time_s = time_ms / 1000.0

safe_msg = html.escape(msg) if msg else "—"

tpl = load_template()
page = (tpl
        .replace("{{MESSAGE}}", safe_msg)
        .replace("{{COUNT}}", str(x))
        .replace("{{TIME_MS}}", str(time_ms))
        .replace("{{TIME_S}}", f"{time_s:.2f}")
)

print("Content-Type: text/html; charset=utf-8")
print()
print(page)
