"""Print the longest gap between successful responses in a probe log."""

import sys

path = sys.argv[1]
rows = []
with open(path, encoding="utf-8") as handle:
    next(handle)
    for line in handle:
        epoch, code, _latency = line.strip().split(",")
        rows.append((float(epoch), code))

if not rows:
    print("no samples")
    sys.exit(1)

gaps = []
gap_start = None
last_ok = None
failures = 0
for epoch, code in rows:
    if code == "200":
        if gap_start is not None:
            gaps.append(epoch - gap_start)
            gap_start = None
        last_ok = epoch
    else:
        failures += 1
        if gap_start is None:
            gap_start = last_ok if last_ok is not None else epoch

if gap_start is not None:
    gaps.append(rows[-1][0] - gap_start)

print(f"samples={len(rows)} non_200={failures}")
if gaps:
    print(f"gaps={len(gaps)} longest_gap_seconds={max(gaps):.3f}")
elif failures and last_ok is None:
    print("longest_gap_seconds=all requests failed")
else:
    print("longest_gap_seconds=0")
