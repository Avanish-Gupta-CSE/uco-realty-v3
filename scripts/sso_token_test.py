#!/usr/bin/env python3
"""Checks /entry?Value=<token> of a RUNNING local staff app (scripts/dev-start.sh). The only cipher is AES/GCM/NoPadding; a token made with
the old AES/CBC/PKCS5Padding must be refused.

    python3 scripts/sso_token_test.py

Needs the Python package "cryptography" (only for this test). The key is read from .run/sso.key. Laptop only, not shipped.
"""
import base64, os, sys, urllib.parse, urllib.request
from pathlib import Path
from cryptography.hazmat.primitives import padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

BASE = os.environ.get("INTERNAL_URL", "http://localhost:8081/UcoRealtyStaff")
KEY = (Path(__file__).resolve().parent.parent / ".run" / "sso.key").read_text().strip().encode()
CIPHER = "AES/GCM/NoPadding"


def token(text, cipher):
    if cipher == "AES/GCM/NoPadding":
        iv = os.urandom(12)
        raw = iv + AESGCM(KEY).encrypt(iv, text.encode(), None)
    else:
        iv = os.urandom(16)
        pad = padding.PKCS7(128).padder()
        enc = Cipher(algorithms.AES(KEY), modes.CBC(iv)).encryptor()
        raw = iv + enc.update(pad.update(text.encode()) + pad.finalize()) + enc.finalize()
    return base64.b64encode(raw).decode()


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *a, **k):
        return None


def where(value):
    op = urllib.request.build_opener(NoRedirect)
    try:
        r = op.open(f"{BASE}/entry?Value=" + urllib.parse.quote(value, safe=""))
        return r.status, ""
    except urllib.error.HTTPError as e:
        return e.code, e.headers.get("Location", "")


results = []
def check(name, ok):
    results.append(ok); print(("  [PASS] " if ok else "  [FAIL] ") + name)

print(f"cipher under test: {CIPHER}")
s, loc = where(token("emp_id=52260&sol_id=7010", CIPHER)); check("a valid token for a known employee signs in (302 to /home)", s == 302 and loc.endswith("/home"))
s, loc = where(token("emp_id=99999999&sol_id=7010", CIPHER)); check("an unknown employee is refused (not /home)", s == 302 and not loc.endswith("/home"))
other = "AES/CBC/PKCS5Padding"
s, loc = where(token("emp_id=52260&sol_id=7010", other)); check("a token made with the old AES/CBC cipher is refused", s == 302 and not loc.endswith("/home"))
good = base64.b64decode(token("emp_id=52260&sol_id=7010", CIPHER)); bad = bytearray(good); bad[-1] ^= 1
s, loc = where(base64.b64encode(bytes(bad)).decode()); check("a changed token does not sign in", s == 302 and not loc.endswith("/home"))
s, loc = where("garbage"); check("garbage is refused", s == 302 and not loc.endswith("/home"))
print(f"\n{sum(results)}/{len(results)} checks passed"); sys.exit(0 if all(results) else 1)
