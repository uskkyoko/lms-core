"""Record status codes until killed. Used as evidence for failover and rollouts."""

import sys
import time
import urllib.error
import urllib.request

url, path = sys.argv[1], sys.argv[2]

with open(path, "w", encoding="utf-8") as handle:
    handle.write("epoch,code,latency\n")
    while True:
        start = time.time()
        code = "000"
        try:
            with urllib.request.urlopen(url, timeout=2) as response:
                code = str(response.status)
                response.read()
        except urllib.error.HTTPError as exc:
            code = str(exc.code)
        except Exception:
            code = "000"
        handle.write(f"{start:.3f},{code},{time.time() - start:.3f}\n")
        handle.flush()
        time.sleep(0.25)
