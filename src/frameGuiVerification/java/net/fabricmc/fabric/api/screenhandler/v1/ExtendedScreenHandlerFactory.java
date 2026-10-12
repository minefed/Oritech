package net.fabricmc.fabric.api.screenhandler.v1;
import net.minecraft.entity.player.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
public interface ExtendedScreenHandlerFactory {
    void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf);
    ScreenHandler createMenu(int id, PlayerInventory inventory, PlayerEntity player);
    Text getDisplayName();
}
