package mcjty.aquamunda.blocks.desalination;

import io.netty.buffer.ByteBuf;
import mcjty.aquamunda.AquaMunda;
import mcjty.aquamunda.network.InfoPacketClient;
import mcjty.aquamunda.varia.BlockTools;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

public class TankContentsInfoPacketClient implements InfoPacketClient {

    private BlockPos pos;
    private int contents;

    public TankContentsInfoPacketClient() {
    }

    public TankContentsInfoPacketClient(BlockPos pos, int contents) {
        this.pos = pos;
        this.contents = contents;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        contents = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeInt(contents);
    }

    @Override
    public void onMessageClient(EntityPlayer player) {
        BlockTools.getTE(DesalinationTankTE.class, AquaMunda.proxy.getClientWorld(), pos)
                .ifPresent(p -> p.setContents(contents));
    }
}
