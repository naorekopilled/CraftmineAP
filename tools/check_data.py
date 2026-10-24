#!/usr/bin/env python3
"""Sanity checks for the dumped craftmine.json. usage: check_data.py <path>"""
import json
import sys

TABS = {"feats", "mines", "story", "unlocks"}


def check(data: dict) -> None:
    ingredients = data["ingredients"]
    assert len(ingredients) >= 80, f"{len(ingredients)} ingredients"
    assert any(i["unlock_mode"] == "ALWAYS_UNLOCKED" for i in ingredients)
    ids = {i["id"] for i in ingredients}
    for i in ingredients:
        assert set(i["unlocked_after"]) <= ids, i["id"]

    unlocks = data["unlocks"]
    assert len(unlocks) >= 150, f"{len(unlocks)} unlocks"
    unlock_ids = {u["id"] for u in unlocks}
    for u in unlocks:
        assert u["parent"] is None or u["parent"] in unlock_ids, u["id"]
        assert set(u["disables"]) <= unlock_ids, u["id"]

    assert len(data["special_mines"]) == 9
    for m in data["special_mines"]:
        assert set(m["required_effects"]) <= ids, m["id"]

    advancements = data["advancements"]
    assert all(a["id"].split("/")[0] in TABS for a in advancements)
    assert not any(a["id"].endswith("/root") for a in advancements)
    assert len(advancements) == 51, f"{len(advancements)} advancements"

    for lst in (ingredients, unlocks, data["special_mines"], advancements):
        keys = [x["id"] for x in lst]
        assert keys == sorted(keys), "lists must be sorted by id"
        assert all(x["name"] for x in lst), "every entry needs its English name"


if __name__ == "__main__":
    with open(sys.argv[1]) as f:
        check(json.load(f))
    print("OK")
