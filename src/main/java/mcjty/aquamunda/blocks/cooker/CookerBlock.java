package mcjty.aquamunda.blocks.cooker;

import mcjty.aquamunda.blocks.generic.GenericBlockWithTE;
import mcjty.aquamunda.fluid.FluidSetup;
import mcjty.aquamunda.items.ItemDish;
import mcjty.aquamunda.items.ModItems;
import mcjty.aquamunda.sound.ISoundProducer;
import mcjty.immcraft.api.handles.HandleSelector;
import mcjty.immcraft.api.handles.IInterfaceHandle;
import mcjty.lib.varia.FluidTools;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.text.DecimalFormat;

public class CookerBlock extends GenericBlockWithTE<CookerTE> implements ISoundProducer {

    public static final PropertyEnum<EnumContents> CONTENTS = PropertyEnum.create("contents", EnumContents.class, EnumContents.values());
    public static final PropertyEnum<EnumLiquid> LIQUID = PropertyEnum.create("liquid", EnumLiquid.class, EnumLiquid.values());

    public static final AxisAlignedBB COOKER_AABB = new AxisAlignedBB(0.05D, 0.0D, 0.05D, 0.95D, 0.62D, 0.95D);

    public CookerBlock() {
        super(Material.IRON, "cooker", CookerTE.class);
        setHardness(2.0f);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 0);
        addSelector(new HandleSelector("slot", new AxisAlignedBB(.33, .4, .33, .66, 1, .66)));
    }

    @Override
    public void initModel() {
        super.initModel();
        CookerTESR.register();
    }


    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return COOKER_AABB;
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        super.addProbeInfo(mode, probeInfo, player, world, blockState, data);
        TileEntity te = world.getTileEntity(data.getPos());
        if (te instanceof CookerTE) {
            CookerTE cookerTE = (CookerTE) te;

            IInterfaceHandle selectedHandle = cookerTE.getHandle(player);
            if (selectedHandle != null) {
                ItemStack currentStack = selectedHandle.getCurrentStack(te);
                if (!currentStack.isEmpty()) {
                    probeInfo.text(TextFormatting.GREEN + currentStack.getDisplayName() + " (" + currentStack.getCount() + ")");
                }
            }
            DecimalFormat decimalFormat = new DecimalFormat("#.#");

            probeInfo.text(TextFormatting.GREEN + "Filled: " + cookerTE.getFilledPercentage() + "%");
            probeInfo.text(TextFormatting.GREEN + "Temperature: " + decimalFormat.format(cookerTE.getTemperature()));
            int cookTime = cookerTE.getCookTime();
            int maxCookTime = cookerTE.getMaxCookTime();
            if (cookTime >= 0) {
                probeInfo.progress(cookTime, maxCookTime);
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            CookerTE cookerTE = getTE(world, pos);

            ItemStack heldItem = player.getHeldItem(EnumHand.MAIN_HAND);

            if (!cookerTE.getSoup().isEmpty()) {
                if (!heldItem.isEmpty() && heldItem.getItem() == Items.BOWL) {
                    heldItem.splitStack(1);
                    ItemStack dish = new ItemStack(ModItems.dish, 1, ItemDish.getDishMeta(cookerTE.getSoup()));
                    if (player.getHeldItem(EnumHand.MAIN_HAND).isEmpty()) {
                        player.setHeldItem(EnumHand.MAIN_HAND, dish);
                    } else {
                        // @todo generalize
                        if (!player.inventory.addItemStackToInventory(dish)) {
                            EntityItem entityItem = new EntityItem(player.getEntityWorld(), player.posX, player.posY, player.posZ, dish);
                            player.getEntityWorld().spawnEntity(entityItem);
                        }
                    }
                    cookerTE.setSoup("");
                    cookerTE.setAmount(0);
                    player.openContainer.detectAndSendChanges();
                } else {
                    ITextComponent component = new TextComponentString(TextFormatting.YELLOW + "You need a bowl to get the soup out");
                    if (player instanceof EntityPlayer) {
                        player.sendStatusMessage(component, false);
                    } else {
                        player.sendMessage(component);
                    }
                }
                return true;
            }

            if (!heldItem.isEmpty()) {
                if (FluidTools.isEmptyContainer(heldItem)) {
                    ItemStack container = heldItem.copy().splitStack(1);    // Don't modify what the player is holding
                    extractIntoContainer(player, container, cookerTE);
                    return true;
                } else if (FluidTools.isFilledContainer(heldItem)) {
                    fillFromContainer(player, world, cookerTE);
                    return true;
                }
            }
        }
        if (super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ)) {
            return true;
        }
        return true;
    }

    private void fillFromContainer(EntityPlayer player, World world, CookerTE cooker) {
        ItemStack heldItem = player.getHeldItem(EnumHand.MAIN_HAND);
        if (heldItem.isEmpty()) {
            return;
        }
        FluidStack fluidStack = FluidTools.convertBucketToFluid(heldItem);
        if (fluidStack != null) {
            if (fluidStack.getFluid() != FluidSetup.freshWater) {
                return;
            }

            int newAmount = cooker.getAmount() + fluidStack.amount;
            if (newAmount <= CookerTE.MAX_AMOUNT) {
                cooker.setAmount(newAmount);
                if (!player.capabilities.isCreativeMode) {
                    ItemStack emptyContainer = FluidTools.drainContainer(heldItem);
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, emptyContainer);
                }
            }
        }
    }

    private void extractIntoContainer(EntityPlayer player, ItemStack container, CookerTE cooker) {
        if (cooker.getAmount() > 0) {
            FluidStack fluidStack = new FluidStack(FluidSetup.freshWater, 1);
            if (container.isEmpty()) {
                return;
            }
            int capacity = FluidTools.getCapacity(fluidStack, container);
            if (capacity != 0) {
                if (cooker.getAmount() >= capacity) {
                    fluidStack.amount = capacity;
                    ItemStack filledContainer = FluidTools.fillContainer(fluidStack, container);
                    if (!filledContainer.isEmpty()) {
                        cooker.setAmount(cooker.getAmount() - capacity);
                        player.inventory.decrStackSize(player.inventory.currentItem, 1);
                        // @todo generalize
                        if (!player.inventory.addItemStackToInventory(filledContainer)) {
                            EntityItem entityItem = new EntityItem(player.getEntityWorld(), player.posX, player.posY, player.posZ, filledContainer);
                            player.getEntityWorld().spawnEntity(entityItem);
                        }
                        player.openContainer.detectAndSendChanges();
                    } else {
                        // Try to insert the fluid back into the tank
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, filledContainer);
                        cooker.setAmount(cooker.getAmount() + capacity);
                        player.openContainer.detectAndSendChanges();
                    }
                }
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos) {
        return new AxisAlignedBB(0, 0, 0, 0, 0, 0);
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

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof CookerTE) {
            CookerTE cookerTE = (CookerTE) te;
            state = state.withProperty(CONTENTS, cookerTE.getContentsState()).withProperty(LIQUID, cookerTE.getLiquidState());
        }
        return state;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING_HORIZ, CONTENTS, LIQUID);
    }
}

