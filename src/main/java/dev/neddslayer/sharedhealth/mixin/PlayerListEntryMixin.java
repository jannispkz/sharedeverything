package dev.neddslayer.sharedhealth.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class PlayerListEntryMixin {

    @Inject(method = "getPlayerListName", at = @At("HEAD"), cancellable = true)
    private void addCoordinatesToTabList(CallbackInfoReturnable<Text> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        String dimension = "";
        if (player.getWorld().getRegistryKey().getValue().getPath().equals("the_nether")) {
            dimension = " §c[N]§r";
        } else if (player.getWorld().getRegistryKey().getValue().getPath().equals("the_end")) {
            dimension = " §5[E]§r";
        }

        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();

        Text coordinatesText = Text.literal(String.format(" §7(%d, %d, %d)%s", x, y, z, dimension));
        cir.setReturnValue(((MutableText) player.getName()).append(coordinatesText));
    }
}