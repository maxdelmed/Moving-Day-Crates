# Moving Day Crates

Pack a piece of furniture with everything inside it into a crate and unpack it in your new home.

## What you can do

- Pack a piece of furniture into a crate with everything stored inside.
- Carry the crate to your new place and unpack it there.
- Pack two-block pieces like wardrobes as one item.
- Turn the furniture before you unpack it, and see what's inside a crate from its tooltip.

## Requirements

- Minecraft 1.20.1
- Forge 47.x

Install on both the client and the server.

## Works with

- Backpacked, Sophisticated Backpacks, Traveler's Backpack and Carry On: their items are recognised as portable storage and can't be packed inside furniture.
- Supplementaries: sacks, safes and lunch baskets count as portable storage too.
- Macaw's Furniture, Handcrafted and Cluttered: their drawers, cupboards, wardrobes, counters, desks, nightstands and kitchen cabinets can be packed.
- Furniture mods: add their blocks to the `movingcrates:packable` tag with a datapack.

## Crafting

Moving Crate (makes two):

```
P S P      P = any planks
P   P      S = string
P P P
```

## Usage

- Hold a Moving Crate and right-click a chest, barrel, furnace or other supported block. You get a Packed Crate with the block and its contents inside.
- Right-click with the Packed Crate to unpack it where you want it. It keeps everything in the same slots.
- It faces you by default. Sneak and use the crate in the air to turn it (left, away, right) before unpacking.
- The crate's tooltip shows who packed it, how many items are inside and the four most common ones.
- Packed crates have no inventory, so hoppers and pipes can't touch what's inside.
- Half of a double chest packs on its own. Beds, doors and other two-block furniture, including Cluttered's two-block wardrobes, cabinets and dressers, pack as one.
- Packing and unpacking respect land protection.

Furniture holding shulker boxes, bundles, backpacks, other crates or any item with its own inventory can't be packed until those are taken out.

## Supported blocks

Chests, trapped chests, barrels, furnaces, blast furnaces, smokers, chiseled bookshelves, lecterns, jukeboxes, decorated pots, beds, and anything in `forge:chests` or `forge:barrels`.

When installed, storage furniture from Macaw's Furniture, Handcrafted and Cluttered works too.

Both lists are tags you can change with a datapack:

- `movingcrates:packable` (blocks): what can be packed
- `movingcrates:portable_storage` (items): what can't be inside packed furniture. Backpacked, Sophisticated Backpacks, Traveler's Backpack and Carry On are already listed.

```json
{
  "replace": false,
  "values": [
    { "id": "somefurnituremod:oak_drawer", "required": false }
  ]
}
```

Keep `"required": false` on modded entries, otherwise one missing id breaks the whole tag.

## Duplication safety

Each crate only holds a shipment id; the furniture is stored on the server and can be unpacked once. Copies of a crate don't work. If the server rolls back after a crash and the original furniture is back in place, the crate won't unpack.

## Commands

Operators (level 2):

- `/movingcrates list`: crates in transit
- `/movingcrates info <id>`
- `/movingcrates reissue <id>`: replacement crate for a shipment still in transit
- `/movingcrates reopen <id>`: reopens an unpacked shipment (check the furniture isn't already standing at the destination first)

## Configuration

`world/serverconfig/movingcrates-server.toml`

| Option | Default | Description |
| --- | --- | --- |
| `maxContentBytes` | 262144 | Largest saved size of one packed block |
| `recoveryDays` | 7 | Days a finished shipment can still be reopened |

In single player you can also change these in game: Mods, pick the mod, then Config.

## Known issues

- Furniture bigger than two blocks isn't supported.

## Building

Java 17:

```
gradlew build
```

The jar ends up in `build/libs`.

## Changelog

### 1.1.0

- Storage furniture from Macaw's Furniture, Handcrafted and Cluttered can be packed
- Supplementaries sacks, safes and lunch baskets count as portable storage
- Two-block furniture from Cluttered packs as one piece
- Choose which way furniture faces before unpacking
- The crate lists the main items inside
- Added a Spanish translation
- Settings can be changed in game from the Mods menu
- Info pages in JEI for the blocks and items
- Added advancements

### 1.0.0

- Initial release

## License

MIT
