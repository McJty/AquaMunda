package mcjty.aquamunda.sound;

import mcjty.aquamunda.blocks.ModBlocks;
import mcjty.aquamunda.config.GeneralConfiguration;
import net.minecraft.block.Block;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CookerSound extends MovingSound {

    public CookerSound(SoundEvent event, World world, BlockPos pos){
        super(event, SoundCategory.BLOCKS);
        this.world = world;
        this.pos = pos;
        this.xPosF = pos.getX();
        this.yPosF = pos.getY();
        this.zPosF = pos.getZ();
        this.attenuationType = AttenuationType.LINEAR;
        this.repeat = true;
        this.repeatDelay = 0;
        this.sound = event;
    }

    private final World world;
    private final BlockPos pos;
    private final SoundEvent sound;

    @Override
    public void update() {
        Block block = world.getBlockState(pos).getBlock();
        if (block != ModBlocks.cookerBlock) {
            donePlaying = true;
            return;
        }
        volume = GeneralConfiguration.baseCookerVolume;
    }

    protected boolean isSoundType(SoundEvent event){
        return sound == event;
    }

}
