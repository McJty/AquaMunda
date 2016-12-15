package mcjty.aquamunda.blocks.generic;


import mcjty.aquamunda.AquaMunda;
import mcjty.aquamunda.rendering.BlockRenderHelper;
import mcjty.aquamunda.varia.BlockTools;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.List;

public class GenericBlockWithTE<T extends GenericTE> extends GenericBlock implements ITileEntityProvider {

    private final Class<? extends GenericTE> teClazz;

    public GenericBlockWithTE(Material material, String name, Class<? extends GenericTE> clazz) {
        super(material, name, clazz);
        teClazz = clazz;
    }

    @Override
    protected void register(String name, Class<? extends GenericTE> clazz, Class<? extends ItemBlock> itemBlockClass) {
        super.register(name, clazz, itemBlockClass);
        GameRegistry.registerTileEntity(clazz, AquaMunda.MODID + "_" + name + "TE");
    }

    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currenttip, accessor, config);
        return currenttip;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        try {
            return teClazz.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public T getTE(IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return (T) te;
    }

    public boolean activateBlock(World world, BlockPos pos, EntityPlayer player, EnumFacing side, float sx, float sy, float sz) {
        return getTE(world, pos).onActivate(player, side, worldToBlockSpace(world, pos, side), new Vec3d(sx, sy, sz));
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        BlockTools.getInventory(world, pos).ifPresent(p -> BlockTools.emptyInventoryInWorld(world, pos, state.getBlock(), p));
        super.breakBlock(world, pos, state);
    }
}