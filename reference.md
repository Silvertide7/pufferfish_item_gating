# Farmer's Delight × Project MMO

> **Requires** [Farmer's Delight](https://www.curseforge.com/minecraft/mc-mods/farmers-delight) and [Project MMO](https://www.curseforge.com/minecraft/mc-mods/project-mmo).

***

## What it does

Bridges Project MMO into Farmer's Delight cooking, so your players actually earn XP when they use the Stove, Cooking Pot, Skillet, and Cutting Board.

| Block                       |Event                            |
| --------------------------- |-------------------------------- |
| Stove, Cooking Pot, Skillet |<code>SMELTED</code> <em>(or <code>SMELT</code>, configurable)</em> |
| Cutting Board               |<code>CRAFT</code>               |

Note: Cooking with the skillet in hand next to a heat source always emits the SMELT event.

This should also work with most Farmer's Delight expansion mods, as long as their recipes use Farmer's Delight cooking blocks or extend it's classes when implementing the blocks.

***

## How to use it

Add XP for the appropriate event on any food item these blocks produce, and you're done:

*   **MC 1.20.1**, and **MC 1.21.1 before v1.1.0** — use `SMELT`
*   **MC 1.21.1 from v1.1.0 onward** — use `SMELTED` (or `SMELT` if configured, v1.2.0+)
*   **Cutting Board recipes** — use `CRAFT`

### Important: XP is based on the _result_, not the ingredients

Vanilla furnaces and campfires award XP from the **raw** input (e.g. raw beef → cooked steak gives XP for raw beef). Farmer's Delight cooking blocks in this mod award XP from the **finished food item** instead (the steak).

Why? Most Farmer's Delight recipes use several ingredients, so anchoring XP to the result keeps configuration simple and predictable.

**What this means for you:**

*   To give XP for both vanilla cooking and Farmer's Delight cooking, configure XP on **both the raw item and the cooked item**.
*   Want to nudge players toward Farmer's Delight blocks? Give the cooked item _more_ XP than the raw one.
*   Want parity? Set both to the same values.

***

## Compatibility & notes

*   Should be compatible with all other mods. If you find a conflict, please open an issue on GitHub.
*   **Not getting XP?** Double-check your Project MMO configuration first — that's where the XP values live.
*   This mod uses **mixins** into Farmer's Delight. A future Farmer's Delight update could in theory break them. If that happens, fixes will land quickly; in the meantime, just hold off on updating Farmer's Delight. Updates are infrequent (once or twice per MC version), so this is unlikely — especially on 1.20.1.

***

## Modpack policy

Free to use in any **non-commercial** modpack. Please link back to this page. Do not redistribute on other sites or include in for-profit packs.
