#!/usr/bin/env python3
"""
Finds a view in a `uiautomator dump` XML tree and prints its tap-center coordinates.

Usage: uiautomator_bounds.py <dump.xml> --id <resource-id-suffix> [--index N]
       uiautomator_bounds.py <dump.xml> --text <exact-text> [--index N]

Resolution-independent by design (works off the view hierarchy's own reported
bounds, not hardcoded pixel positions), so it survives different emulator
screen sizes/densities. Exits 1 with nothing on stdout if no match is found,
so the caller can retry/poll.
"""
import argparse
import re
import sys
import xml.etree.ElementTree as ET

BOUNDS_RE = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("dump_path")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--id", help="Suffix to match against resource-id, e.g. 'loginButton'")
    group.add_argument("--text", help="Exact text to match")
    parser.add_argument("--index", type=int, default=0, help="0-based match index when several match")
    args = parser.parse_args()

    try:
        tree = ET.parse(args.dump_path)
    except ET.ParseError:
        return 1

    matches = []
    for node in tree.getroot().iter("node"):
        rid = node.attrib.get("resource-id", "")
        text = node.attrib.get("text", "")
        if args.id and rid.endswith(f"id/{args.id}"):
            matches.append(node)
        elif args.text and text == args.text:
            matches.append(node)

    if args.index >= len(matches):
        return 1

    bounds = matches[args.index].attrib.get("bounds", "")
    m = BOUNDS_RE.match(bounds)
    if not m:
        return 1

    x1, y1, x2, y2 = (int(v) for v in m.groups())
    print(f"{(x1 + x2) // 2} {(y1 + y2) // 2}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
