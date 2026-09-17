package at.petra_k.hexcasting;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import at.petra_k.hexcasting.common.lib.hex.HexActionRegistry;
import at.petra_k.hexcasting.common.capability.HexCapabilities;
import at.petra_k.hexcasting.common.block.TileEntityConjured;
import at.petra_k.hexcasting.common.block.TileEntityImpetus;
import at.petra_k.hexcasting.common.block.TileEntitySlate;
import at.petra_k.hexcasting.common.block.TileEntityAkashicBookshelf;
import at.petra_k.hexcasting.common.block.TileEntityAkashicRecord;
import at.petra_k.hexcasting.common.network.MsgCastParticlesS2C;
import at.petra_k.hexcasting.common.network.MsgCastingDataS2C;
import at.petra_k.hexcasting.common.network.MsgCastingPatternS2C;
import at.petra_k.hexcasting.common.network.MsgClearCastingPatternsS2C;
import at.petra_k.hexcasting.common.network.MsgPerWorldPatternsS2C;
import at.petra_k.hexcasting.common.network.MsgStaffCastResultS2C;
import at.petra_k.hexcasting.common.network.MsgStaffProgramS2C;
import at.petra_k.hexcasting.interop.inline.HexInline;
import at.petrak.paucal.api.PaucalAPI;

/** 1.12.2 Forge entry point for the Hex Casting port. */
@Mod(modid = HexCasting.MOD_ID, name = HexCasting.NAME, version = HexCasting.VERSION,
    dependencies = "required-after:patchouli;required-after:jei;required-after:baubles;required-after:forgelin")
public final class HexCasting {
    public static final String MOD_ID = "hexcasting";
    public static final String NAME = "Hex Casting";
    public static final String VERSION = "0.1.0-1.12.2";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerTileEntity(TileEntityConjured.class,
            new net.minecraft.util.ResourceLocation(MOD_ID, "conjured"));
        GameRegistry.registerTileEntity(TileEntitySlate.class,
            new net.minecraft.util.ResourceLocation(MOD_ID, "slate"));
        GameRegistry.registerTileEntity(TileEntityImpetus.class,
            new net.minecraft.util.ResourceLocation(MOD_ID, "impetus"));
        GameRegistry.registerTileEntity(TileEntityAkashicBookshelf.class,
            new net.minecraft.util.ResourceLocation(MOD_ID, "akashic_bookshelf"));
        GameRegistry.registerTileEntity(TileEntityAkashicRecord.class,
            new net.minecraft.util.ResourceLocation(MOD_ID, "akashic_record"));
        HexCapabilities.register();
        HexActionRegistry.bootstrap();
        PaucalAPI.init();
        // Register the complete message set before any world/player event can
        // send a synchronization packet.  Previously only the staff GUI's
        // client-to-server message was registered, so login, orbit, particle,
        // and authoritative GUI snapshots were silently unavailable on a
        // fresh client connection.
        at.petra_k.hexcasting.common.network.MsgStaffPatternC2S.register();
        MsgCastingPatternS2C.register();
        MsgClearCastingPatternsS2C.register();
        MsgCastParticlesS2C.register();
        MsgCastingDataS2C.register();
        MsgPerWorldPatternsS2C.register();
        MsgStaffProgramS2C.register();
        MsgStaffCastResultS2C.register();
        HexInline.init();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Gameplay and integration handlers will be populated during the port.
    }
}
