package at.petra_k.hexcasting.client;

import at.petra_k.hexcasting.common.lib.HexSounds;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;

/** Looping, listener-relative ambience used while the staff grid is open. */
public final class HexGridSound extends MovingSound {
    private final EntityPlayer player;

    public HexGridSound(EntityPlayer player) {
        super(HexSounds.CASTING_AMBIANCE, SoundCategory.PLAYERS);
        this.player = player;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.25F;
        this.pitch = 1.0F;
        this.attenuationType = ISound.AttenuationType.LINEAR;
        updatePosition();
    }

    @Override
    public void update() {
        if (player == null || player.isDead || player.world == null) {
            donePlaying = true;
            return;
        }
        updatePosition();
    }

    private void updatePosition() {
        Vec3d eyes = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        xPosF = (float) (eyes.x + look.x);
        yPosF = (float) (eyes.y + look.y);
        zPosF = (float) (eyes.z + look.z);
    }
}
