#!/usr/bin/env python3
"""Keep the web copy of the QBox logo in sync with the Android runtime drawable.

The Android drawable is the single source of truth. The asset copy is what the
local configuration web page serves, so it cannot be a symlink or a resource
alias. CI runs this with --check; contributors run it without arguments after
changing the logo.
"""

from __future__ import annotations

import hashlib
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MASTER = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi" / "ic_logo.png"
COPIES = (ROOT / "app" / "src" / "main" / "assets" / "qbox-logo.png",)


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> int:
    if not MASTER.is_file():
        print(f"missing master logo: {MASTER}", file=sys.stderr)
        return 1

    check = "--check" in sys.argv[1:]
    master_hash = sha256(MASTER)
    failed = False

    for copy in COPIES:
        if check:
            if not copy.is_file() or sha256(copy) != master_hash:
                print(f"out of sync: {copy.relative_to(ROOT)}", file=sys.stderr)
                failed = True
            else:
                print(f"ok: {copy.relative_to(ROOT)}")
        else:
            copy.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(MASTER, copy)
            print(f"updated: {copy.relative_to(ROOT)}")

    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
