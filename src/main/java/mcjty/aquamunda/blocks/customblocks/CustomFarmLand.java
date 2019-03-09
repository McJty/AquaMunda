package mcjty.aquamunda.blocks.customblocks;

import mcjty.aquamunda.blocks.ModBlocks;
import mcjty.aquamunda.blocks.sprinkler.SprinklerTE;
import mcjty.aquamunda.chunkdata.GameData;
import mcjty.aquamunda.compat.top.TOPInfoProvider;
import mcjty.aquamunda.config.GeneralConfiguration;
import mcjty.aquamunda.environment.EnvironmentData;
import mcjty.aquamunda.environment.FarmlandOverhaulType;
import mcjty.aquamunda.network.PacketGetInfoFromServer;
import mcjty.aquamunda.network.AMPacketHandler;
import mcjty.aquamunda.waila.WailaProvider;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

public class CustomFarmLand extends BlockFarmland implements WailaProvider, TOPInfoProvider {

    public CustomFarmLand() {
        super();
        this.setHardness(0.6F);
        this.setSoundType(SoundType.GROUND);
        this.setUnlocalizedName("farmland");
        setRegistryName("farmland");
    }

    @SideOnly(Side.CLIENT)
    public void initModel() {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(Blocks.FARMLAND.getRegistryName(), "inventory"));
    }

    private static long lastUpdateTime = 0;
    public static int clientLevel = 0;

    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        long time = System.currentTimeMillis();
        if ((time - lastUpdateTime) > 200) {
            lastUpdateTime = time;
            AMPacketHandler.INSTANCE.sendToServer(new PacketGetInfoFromServer(new FarmLandMoistnessPacketServer(accessor.getPosition())));
        }

        if (clientLevel == -1) {
            currenttip.add(TextFormatting.YELLOW + "No fresh water nearby!");
        } else {
            currenttip.add(TextFormatting.GREEN + "Moistness: " + (clientLevel * 100 / SprinklerTE.MAX_MOISTNESS) + "%");
        }
        return currenttip;
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        if (!freshWaterNearby(world, data.getPos())) {
            probeInfo.text(TextFormatting.YELLOW + "No fresh water nearby!");
        } else {
            EnvironmentData environment = EnvironmentData.getEnvironmentData(player.getEntityWorld());
            byte level = environment.getData().get(player.getEntityWorld().provider.getDimension(), data.getPos());
            probeInfo.text(TextFormatting.GREEN + "Moistness: " + (level * 100 / SprinklerTE.MAX_MOISTNESS) + "%");
        }
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            EnvironmentData environment = EnvironmentData.getEnvironmentData(world);
            if (environment.getData().set(world.provider.getDimension(), pos, (byte) 0)) {
                environment.save();
            }
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        handleRain(world, pos, random);
        handleSprinkler(world, pos, random);

        if (!freshWaterNearby(world, pos) && !world.isRainingAt(pos.up())) {
            int l = state.getBlock().getMetaFromState(state);
            if (l > 0) {
                world.setBlockState(pos, state.getBlock().getStateFromMeta(l - 1), 2);
            } else if (!isASuitablePlant(world, pos)) {
                world.setBlockState(pos, Blocks.DIRT.getDefaultState());
            }
        } else {
            world.setBlockState(pos, state.getBlock().getStateFromMeta(7), 2);
        }
    }

    private void handleRain(World world, BlockPos pos, Random random) {
        if (world.isRaining()) {
            if (world.canBlockSeeSky(pos.up())) {
                EnvironmentData environmentData = EnvironmentData.getEnvironmentData(world);
                GameData data = environmentData.getData();
                byte moistness = data.get(world.provider.getDimension(), pos);
                if (moistness < SprinklerTE.MAX_MOISTNESS) {
                    moistness += 2;
                    if (moistness > SprinklerTE.MAX_MOISTNESS) {
                        moistness = SprinklerTE.MAX_MOISTNESS;
                    }
                    data.set(world.provider.getDimension(), pos, moistness);
                    environmentData.save();
                }
            }
        }
    }

    private void handleSprinkler(World world, BlockPos pos, Random random) {
        EnvironmentData environmentData = EnvironmentData.getEnvironmentData(world);
        GameData data = environmentData.getData();
        byte moistness = data.get(world.provider.getDimension(), pos);
        if (moistness == 0) {
            if (GeneralConfiguration.farmlandOverhaulType.get() == FarmlandOverhaulType.HARSH) {
                if (random.nextInt(8) == 2) {
                    killPlant(world, pos);
                }
            }
        } else {
            moistness--;
            data.set(world.provider.getDimension(), pos, moistness);
            environmentData.save();
        }
    }

    private void killPlant(World world, BlockPos pos) {
        Block block = world.getBlockState(pos.up()).getBlock();
        if (block == Blocks.CARROTS) {
            world.setBlockState(pos.up(), ModBlocks.deadCarrot.getDefaultState(), 3);
        } else if (block == Blocks.WHEAT) {
            world.setBlockState(pos.up(), ModBlocks.deadWheat.getDefaultState(), 3);
        } else if (block instanceof IGrowable) {
            world.setBlockState(pos.up(), ModBlocks.deadWheat.getDefaultState(), 3);
        }
    }

    private boolean isASuitablePlant(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos.up());
        Block block = state.getBlock();
        return block instanceof IPlantable && canSustainPlant(state, world, pos, EnumFacing.UP, (IPlantable) block);
    }

    @Override
    public boolean canSustainPlant(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing direction, IPlantable plantable) {
        IBlockState plant = plantable.getPlant(world, pos.offset(direction));
        net.minecraftforge.common.EnumPlantType plantType = plantable.getPlantType(world, pos.offset(direction));

        switch (plantType) {
            case Crop:
            case Plains:
                return true;
            case Desert:
            case Nether:
            case Cave:
            case Water:
            case Beach:
            default:
                return false;
        }
    }

    public static boolean freshWaterNearby(World world, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        for (int l = x - 4; l <= x + 4; ++l) {
            for (int i1 = y; i1 <= y + 1; ++i1) {
                for (int j1 = z - 4; j1 <= z + 4; ++j1) {
                    Block block = world.getBlockState(new BlockPos(l, i1, j1)).getBlock();
                    if (GeneralConfiguration.farmlandOverhaulType.get() == FarmlandOverhaulType.NONE) {
                        if (block == ModBlocks.blockFreshWater || block == Blocks.WATER || block == Blocks.FLOWING_WATER) {
                            return true;
                        }
                    }
                    if (block == ModBlocks.blockFreshWater) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
