package mcjty.aquamunda.blocks.customblocks;

import io.netty.buffer.ByteBuf;
import mcjty.aquamunda.network.InfoPacketClient;
import net.minecraft.entity.player.EntityPlayer;

public class FarmLandMoistnessPacketClient implements InfoPacketClient {

    private int level;

    public FarmLandMoistnessPacketClient() {
    }

    public FarmLandMoistnessPacketClient(int level) {
        this.level = level;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        level = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(level);
    }

    @Override
    public void onMessageClient(EntityPlayer player) {
        CustomFarmLand.clientLevel = level;
    }
}
