package mcjty.aquamunda.compat.immcraft;

import mcjty.aquamunda.api.IHoseConnector;
import mcjty.immcraft.api.cable.*;
import mcjty.immcraft.api.multiblock.IMultiBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

public class LiquidCableHandler implements ICableHandler {

    @Override
    public ICable getCable(World world, ICableSubType subType, int id) {
        return (ICable) ImmersiveCraftHandler.hoseNetwork.getOrCreateMultiBlock(id);
    }

    @Override
    public void tick(IBundle bundle, ICableSection section) {

        // @todo take fluid pressure into account
        // @todo fix height difference calculation to be based on height of tank/connector and not on end points of hose.

        TileEntity bundleTE = bundle.getTileEntity();

        int id = section.getId();

        ICable hose = getHose(bundleTE, id);
        if (hose.getPath().isEmpty()) {
            return;     // Safety
        }
        BlockPos first = hose.getPath().get(0);
        if (!first.equals(bundleTE.getPos())) {
            // We only let the first hosemultiblock TE in the path do the work.
            return;
        }

        BlockPos last = hose.getPath().get(((IMultiBlock)hose).getBlockCount() - 1);
        World worldObj = bundleTE.getWorld();
        Optional<IBundle> oBundle = ImmersiveCraftHandler.immersiveCraft.getBundle(worldObj, last);
        if (!oBundle.isPresent()) {
            return;
        }
        IBundle otherBundle = oBundle.get();

        ICableConnector connector1 = getCableConnector(section, worldObj, null);
        if (connector1 == null) {
            return;
        }

        ICableSection otherSection = otherBundle.findSection(section.getType(), section.getSubType(), id);
        if (otherSection == null) {
            return;
        }

        ICableConnector connector2 = getCableConnector(otherSection, worldObj, connector1);
        if (connector2 == null) {
            return;
        }


        IHoseConnector con1 = (IHoseConnector) connector1;
        IHoseConnector con2 = (IHoseConnector) connector2;

        if (con1.getSupportedFluid() != null && con2.getSupportedFluid() != null && con1.getSupportedFluid() != con2.getSupportedFluid()) {
            // Do nothing. The fluids are not equivalent
            return;
        }
        int y1 = first.getY();
        int y2 = last.getY();
        if (y1 == y2) {
            // Same height so try to balance the two containers.
            float pct1 = con1.getFilledPercentage();
            float pct2 = con2.getFilledPercentage();
            if (pct1 > pct2) {
                int available = con2.getEmptyLiquidLeft();
                int extracted = con1.extract(min3(con1.getMaxExtractPerTick(), con2.getMaxInsertPerTick(), available));
                con2.insert(con1.getSupportedFluid(), extracted);
            } else if (pct2 > pct1) {
                int available = con1.getEmptyLiquidLeft();
                int extracted = con2.extract(min3(con2.getMaxExtractPerTick(), con1.getMaxInsertPerTick(), available));
                con1.insert(con2.getSupportedFluid(), extracted);
            }
        } else if (y1 > y2) {
            int available = con2.getEmptyLiquidLeft();
            int extracted = con1.extract(min3(con1.getMaxExtractPerTick(), con2.getMaxInsertPerTick(), available));
            con2.insert(con1.getSupportedFluid(), extracted);
        } else {
            int available = con1.getEmptyLiquidLeft();
            int extracted = con2.extract(min3(con2.getMaxExtractPerTick(), con1.getMaxInsertPerTick(), available));
            con1.insert(con2.getSupportedFluid(), extracted);
        }
    }

    @Override
    public String getNetworkName(ICableSubType subType) {
        return ImmersiveCraftHandler.AQUA_MUNDA_HOSES;
    }

    private ICableConnector getCableConnector(ICableSection section, World worldObj, ICableConnector prevCable) {
        ICableConnector cable = section.getConnector(worldObj, 0);
        if (cable == null || cable == prevCable) {
            cable = section.getConnector(worldObj, 1);
        }
        return cable;
    }

    public ICable getHose(TileEntity bundleTE, int id) {
        if (id == -1) {
            return null;
        }
        return (ICable) ImmersiveCraftHandler.hoseNetwork.getOrCreateMultiBlock(id);
    }

    private static int min3(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }

}
