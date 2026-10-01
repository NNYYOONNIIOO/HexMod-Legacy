// Hex Casting Legacy CraftTweaker examples for 1.12.2.
// Copy this file to the instance's scripts/ directory after installing
// CraftTweaker. Every line below is optional content for a test pack.

// ---------------------------------------------------------------------------
// Craft Phial
// ---------------------------------------------------------------------------
// The first input is the item entity thrown into the world. The second input
// is the item held in the casting hand. The output replaces the held input.
mods.hexcasting.CraftPhial.add(
    <minecraft:diamond>,
    <minecraft:emerald>,
    50000,
    <minecraft:gold_ingot>
);

// ---------------------------------------------------------------------------
// Edify
// ---------------------------------------------------------------------------
// The input must have a concrete block form. A later add for the same item
// replaces the earlier entry, just like the built-in table overrides do.
mods.hexcasting.Edify.add(<minecraft:sapling:0>, 100000);

// ---------------------------------------------------------------------------
// Brainsweep / Strip Mind
// ---------------------------------------------------------------------------
// This form accepts every registered zombie, with no entity NBT requirement.
mods.hexcasting.Brainsweep.add(
    <minecraft:obsidian>,
    <entity:minecraft:zombie>,
    75000,
    <minecraft:diamond_block>
);

// Profession 0 + Career 4 is the 1.12.2 fletcher villager career.
// CareerLevel 3 means level 3 or higher. The final argument is a translation
// key supplied by the pack author, so JEI can display a localized profession.
mods.hexcasting.Brainsweep.addWithNbt(
    <minecraft:stone>,
    <entity:minecraft:villager>,
    {Profession: 0, Career: 4, CareerLevel: 3},
    250000,
    <minecraft:gold_block>,
    "my_pack.hexcasting.brainsweep.fletcher"
);

// ---------------------------------------------------------------------------
// Custom media values
// ---------------------------------------------------------------------------
// This value is per item, is shown in the media tooltip, and participates in
// get_media, spell consumption, Craft Phial, and transaction rollback.
// Adding the same item again replaces its previous value.
mods.hexcasting.Media.add(<minecraft:rotten_flesh>, 12000);

// ---------------------------------------------------------------------------
// Removal examples
// ---------------------------------------------------------------------------
// Uncomment only the entries you want to remove. These are intentionally
// disabled so that copying this file leaves the examples available.

// Exact Craft Phial recipe removal:
// mods.hexcasting.CraftPhial.remove(
//     <minecraft:diamond>, <minecraft:emerald>, <minecraft:gold_ingot>
// );
// Remove all Craft Phial recipes using one dropped input:
// mods.hexcasting.CraftPhial.removeInput(<minecraft:diamond>);
// Remove all Craft Phial recipes producing one output:
// mods.hexcasting.CraftPhial.removeOutput(<minecraft:gold_ingot>);

// Remove one Edify input or restore the table to its built-in entries:
// mods.hexcasting.Edify.remove(<minecraft:sapling:0>);
// mods.hexcasting.Edify.removeAll();

// Remove Brainsweep recipes by input, output, or exact recipe key:
// mods.hexcasting.Brainsweep.removeInput(<minecraft:stone>);
// mods.hexcasting.Brainsweep.removeOutput(<minecraft:gold_block>);
// mods.hexcasting.Brainsweep.removeWithNbt(
//     <minecraft:stone>,
//     <entity:minecraft:villager>,
//     {Profession: 0, Career: 4, CareerLevel: 3},
//     250000,
//     <minecraft:gold_block>
// );

// Remove a custom media value only when its current value matches exactly:
// mods.hexcasting.Media.remove(<minecraft:rotten_flesh>, 12000);
