package com.kingsmp.fakespawner.screen;

import com.kingsmp.fakespawner.FakeSpawnerClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** A local, chest-style six-row preview screen. It has no server container. */
public final class FakeSpawnerScreen extends Screen {
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 154;
    private static final int SLOT_SIZE = 18;

    private final BlockPos spawnerPos;
    private int left;
    private int top;

    public FakeSpawnerScreen(BlockPos spawnerPos) {
        super(Text.literal("256 Skeleton"));
        this.spawnerPos = spawnerPos.toImmutable();
    }

    @Override
    protected void init() {
        left = (width - GUI_WIDTH) / 2;
        top = (height - GUI_HEIGHT) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        // Chest-like frame and header.
        context.fill(left, top, left + GUI_WIDTH, top + GUI_HEIGHT, 0xFF302923);
        context.fill(left + 2, top + 2, left + GUI_WIDTH - 2, top + 26, 0xFF564638);
        context.drawCenteredTextWithShadow(textRenderer, title, left + GUI_WIDTH / 2, top + 9, 0xFFFFFFFF);
        context.fill(left + 2, top + 28, left + GUI_WIDTH - 2, top + GUI_HEIGHT - 2, 0xFF8B7965);

        for (int slot = 0; slot < 54; slot++) {
            int sx = slotX(slot);
            int sy = slotY(slot);
            context.fill(sx, sy, sx + 17, sy + 17, 0xFF3D3832);
            context.fill(sx + 1, sy + 1, sx + 16, sy + 16, 0xFF81776B);
        }

        // Eight stacks of 64 bones: visual preview only.
        for (int slot = 0; slot < 8; slot++) {
            context.drawItem(new ItemStack(Items.BONE, 64), slotX(slot) + 1, slotY(slot) + 1);
        }

        // Bottom-row controls (45-53).
        drawSlotItem(context, 45, Items.ARROW, 1);
        drawSlotItem(context, 46, Items.GRAY_STAINED_GLASS_PANE, 1);
        drawSlotItem(context, 47, Items.GRAY_STAINED_GLASS_PANE, 1);
        drawSlotItem(context, 48, Items.BUNDLE, 1);
        drawSlotItem(context, 49, Items.EXPERIENCE_BOTTLE, 1);
        drawSlotItem(context, 50, Items.SPAWNER, 1);
        drawSlotItem(context, 51, Items.GRAY_STAINED_GLASS_PANE, 1);
        drawSlotItem(context, 52, Items.GRAY_STAINED_GLASS_PANE, 1);
        drawSlotItem(context, 53, Items.CHISELED_STONE_BRICKS, 1);

        drawShortLabel(context, 45, "Trước", 0xFFFFD45A);
        drawShortLabel(context, 48, "Bán", 0xFFFFB347);
        drawShortLabel(context, 49, "EXP", 0xFF55FF55);
        drawShortLabel(context, 50, "Info", 0xFF55FFFF);
        drawShortLabel(context, 53, "Cài đặt", 0xFFFFFFFF);

        if (isHovered(mouseX, mouseY, 49)) {
            drawTooltip(context, mouseX, mouseY, List.of(
                    Text.literal("NHẬN EXP").formatted(Formatting.GREEN),
                    Text.literal("Bấm vào để nhận toàn bộ EXP").formatted(Formatting.GRAY),
                    Text.literal("ĐANG CÓ: " + FakeSpawnerClient.getLocalExperience() + "/768K")
                            .formatted(Formatting.YELLOW)));
        } else if (isHovered(mouseX, mouseY, 50)) {
            drawTooltip(context, mouseX, mouseY, List.of(
                    Text.literal("THÔNG TIN").formatted(Formatting.AQUA),
                    Text.literal("TỐC ĐỘ SPAWN: 5 - 15 giây").formatted(Formatting.GRAY),
                    Text.literal("ITEM RƠI: 512 - 1024 MỖI LẦN").formatted(Formatting.GRAY),
                    Text.literal("SỐ EXP RƠI: 256 - 1280 MỖI LẦN").formatted(Formatting.GRAY),
                    Text.literal("ĐANG CÓ: " + FakeSpawnerClient.getLocalExperience() + "/768K").formatted(Formatting.YELLOW),
                    Text.literal("Interact with Spawn Egg:").formatted(Formatting.GRAY),
                    Text.literal("Sets Mob Type").formatted(Formatting.GOLD)));
        } else if (isHovered(mouseX, mouseY, 48)) {
            drawTooltip(context, mouseX, mouseY, List.of(
                    Text.literal("BÁN VẬT PHẨM").formatted(Formatting.GOLD),
                    Text.literal("Menu mô phỏng cục bộ").formatted(Formatting.GRAY)));
        } else if (isHovered(mouseX, mouseY, 53)) {
            drawTooltip(context, mouseX, mouseY, List.of(
                    Text.literal("Tùy chỉnh").formatted(Formatting.WHITE),
                    Text.literal("Thiết lập hiển thị local").formatted(Formatting.GRAY)));
        } else if (isHovered(mouseX, mouseY, 45)) {
            drawTooltip(context, mouseX, mouseY, List.of(Text.literal("Trang trước").formatted(Formatting.YELLOW)));
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        int slot = getHoveredSlot((int) click.x(), (int) click.y());
        if (slot == 49) {
            FakeSpawnerClient.collectLocalExperience();
            return true;
        }
        if (slot == 48) {
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("BÁN VẬT PHẨM: giao diện mô phỏng client-side.")
                        .formatted(Formatting.GOLD), true);
            }
            return true;
        }
        if (slot == 45 || slot == 50 || slot == 53) {
            if (client != null && client.player != null) {
                String message = switch (slot) {
                    case 45 -> "Bạn đang ở trang đầu tiên.";
                    case 50 -> "Thông tin Lồng triệu hồi Skeleton đang hiển thị khi rê chuột.";
                    default -> "Tùy chỉnh: đây là giao diện xem trước client-side.";
                };
                client.player.sendMessage(Text.literal(message).formatted(Formatting.YELLOW), true);
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void drawSlotItem(DrawContext context, int slot, net.minecraft.item.Item item, int count) {
        context.drawItem(new ItemStack(item, count), slotX(slot) + 1, slotY(slot) + 1);
    }

    private void drawShortLabel(DrawContext context, int slot, String label, int color) {
        int labelWidth = textRenderer.getWidth(label);
        int x = slotX(slot) + (18 - labelWidth) / 2;
        context.drawText(textRenderer, label, x, slotY(slot) + 18, color, true);
    }

    private void drawTooltip(DrawContext context, int mouseX, int mouseY, List<Text> lines) {
        context.drawTooltip(textRenderer, lines, mouseX, mouseY);
    }

    private int slotX(int slot) {
        return left + 7 + (slot % 9) * SLOT_SIZE;
    }

    private int slotY(int slot) {
        return top + 31 + (slot / 9) * SLOT_SIZE;
    }

    private boolean isHovered(int mouseX, int mouseY, int slot) {
        return getHoveredSlot(mouseX, mouseY) == slot;
    }

    private int getHoveredSlot(int mouseX, int mouseY) {
        for (int slot = 0; slot < 54; slot++) {
            int x = slotX(slot);
            int y = slotY(slot);
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
