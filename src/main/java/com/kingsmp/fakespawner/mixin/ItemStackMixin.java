package com.kingsmp.fakespawner.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void fakespawner$renameCopperBlock(CallbackInfoReturnable<Text> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.isOf(Items.COPPER_BLOCK)) {
            cir.setReturnValue(Text.literal("Lồng triệu hồi Skeleton").formatted(Formatting.AQUA));
        }
    }
}
