"""Tiny HTTP helper shared by scripts/smoke_test.py and scripts/load_demo_data.py (standard library only).

Keeps cookies, never follows redirects (so tests can see them), reads the CSRF token from pages and
signs in through the LOCAL-PROFILE employee-id + OTP form of the internal portal (local profile only: in UAT/prod
that form does not exist and these scripts must not be used).
"""
import http.cookiejar
import re
import urllib.error
import urllib.parse
import urllib.request
import uuid


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


class Client:
    def __init__(self, base):
        self.base = base.rstrip("/")
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar), _NoRedirect)

    def request(self, method, path, data=None, headers=None, raw=None):
        headers = dict(headers or {})
        body = raw
        if data is not None:
            body = urllib.parse.urlencode(data).encode()
            headers.setdefault("Content-Type", "application/x-www-form-urlencoded")
        req = urllib.request.Request(self.base + path, data=body, headers=headers, method=method)
        try:
            with self.opener.open(req, timeout=60) as r:
                return r.status, dict(r.headers), r.read()
        except urllib.error.HTTPError as e:
            return e.code, dict(e.headers), e.read()
        except urllib.error.URLError as e:
            raise SystemExit(f"Cannot reach {self.base}: {e.reason}. Is the app running (scripts/dev-start.sh)?")

    def get(self, path, headers=None):
        return self.request("GET", path, headers=headers)

    def post(self, path, data=None, headers=None):
        return self.request("POST", path, data=data or {}, headers=headers)

    def csrf(self, path):
        status, _, body = self.get(path)
        m = re.search(rb'name="_csrf" value="([^"]+)"', body)
        return m.group(1).decode() if m else None

    def dev_login(self, emp_id, otp="123456"):
        """Employee id + OTP form (local profile). Returns True when /admin/projects is reachable afterwards."""
        token = self.csrf("/login")
        if not token:
            return False
        self.post("/sso/generate-otp", {"empId": emp_id, "_csrf": token})
        token = self.csrf("/otp")
        if not token:
            return False
        self.post("/otp-verify", {"otp": otp, "_csrf": token})
        return self.get("/admin/projects")[0] == 200

    def multipart_post(self, path, fields, files=()):
        """fields: dict of text parts; files: iterable of (field, filename, bytes, content_type)."""
        boundary = "----uco" + uuid.uuid4().hex
        out = []
        for k, v in fields.items():
            out.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode())
        for k, fn, data, ctype in files:
            out.append((f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"; filename="{fn}"\r\n'
                        f'Content-Type: {ctype}\r\n\r\n').encode() + data + b"\r\n")
        out.append(f"--{boundary}--\r\n".encode())
        return self.request("POST", path, raw=b"".join(out),
                            headers={"Content-Type": f"multipart/form-data; boundary={boundary}"})


def location(headers):
    return headers.get("Location") or headers.get("location") or ""
