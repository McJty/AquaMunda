package mcjty.aquamunda.blocks.cuttingboard;

import mcjty.aquamunda.blocks.generic.GenericBlockWithTE;
import mcjty.aquamunda.sound.ISoundProducer;
import mcjty.immcraft.api.handles.HandleSelector;
import mcjty.immcraft.api.handles.IInterfaceHandle;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class CuttingBoardBlock extends GenericBlockWithTE<CuttingBoardTE> implements ISoundProducer {

    public static final AxisAlignedBB BOARD_AABB = new AxisAlignedBB(0.1D, 0.0D, 0.05D, 0.90D, 0.1D, 0.95D);

    public static final String SEL_INPUT1 = "i1";
    public static final String SEL_INPUT2 = "i2";
    public static final String SEL_INPUT3 = "i3";
    public static final String SEL_OUTPUT = "o";


    public CuttingBoardBlock() {
        super(Material.IRON, "cutting_board", CuttingBoardTE.class);
        setHardness(2.0f);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("pickaxe", 0);

        float boundsdx = .2f;
        float boundsdz = .3f;
        addSelector(createSelector(SEL_INPUT1, boundsdx, boundsdz, 0, 1));
        addSelector(createSelector(SEL_INPUT2, boundsdx, boundsdz, 2, 1));
        addSelector(createSelector(SEL_INPUT3, boundsdx, boundsdz, 1, 2));
        addSelector(createSelector(SEL_OUTPUT, boundsdx, boundsdz, 3, 1.5f));
    }

    private HandleSelector createSelector(String id, float boundsdx, float boundsdz, float x, float y) {
        return new HandleSelector(id, new AxisAlignedBB(boundsdx * x + .1f, 0.15f, boundsdz * y + .1f, boundsdx * (x + 1) + .1f, 0.25f, boundsdz * (y + 1) + .1f));
    }

    @Override
    public void initModel() {
        super.initModel();
        CuttingBoardTESR.register();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos) {
        return new AxisAlignedBB(0, 0, 0, 0, 0, 0);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BOARD_AABB;
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        super.addProbeInfo(mode, probeInfo, player, world, blockState, data);
        TileEntity te = world.getTileEntity(data.getPos());
        if (te instanceof CuttingBoardTE) {
            CuttingBoardTE cuttingBoardTE = (CuttingBoardTE) te;

            IInterfaceHandle selectedHandle = cuttingBoardTE.getHandle(player);
            if (selectedHandle != null) {
                ItemStack currentStack = selectedHandle.getCurrentStack(te);
                if (!currentStack.isEmpty()) {
                    probeInfo.text(TextFormatting.GREEN + currentStack.getDisplayName() + " (" + currentStack.getCount() + ")");
                }
            }

            int chopCounter = cuttingBoardTE.getChopCounter();
            int maxChopCounter = cuttingBoardTE.getMaxChopCounter();
            if (chopCounter >= 0) {
                probeInfo.progress(chopCounter, maxChopCounter);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        return false;
    }

    @Override
    public boolean isBlockNormalCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }
}

