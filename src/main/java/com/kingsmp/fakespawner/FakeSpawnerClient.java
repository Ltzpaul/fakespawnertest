package com.kingsmp.fakespawner;

import com.kingsmp.fakespawner.screen.FakeSpawnerScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client-only cosmetic implementation. No packets are sent by the fake stacking
 * and reward UI, and no server-side inventory or economy state is modified.
 */
public final class FakeSpawnerClient implements ClientModInitializer {
    public static final String MOD_ID = "fakespawner";
    public static final String DISPLAY_NAME = "Lồng triệu hồi Skeleton";

    private static final Map<BlockPos, Integer> LOCAL_STACKS = new HashMap<>();
    private static final Map<BlockPos, SkeletonEntity> PREVIEW_SKELETONS = new HashMap<>();
    private static final int INITIAL_LOCAL_STACK = 64;
    private static final int SCAN_RADIUS_XZ = 8;
    private static final int SCAN_RADIUS_Y = 4;

    private static int localExperience = 512;
    private static long rewardHudExpiresAt = 0L;
    private static int scanCooldown = 0;
    private static long localWorldTime = 0L;
    private static ClientWorld lastWorld = null;

    @Override
    public void onInitializeClient() {
        registerTooltip();
        registerBlockInteractions();
        registerClientTick();
        registerRewardHud();
    }

    private static void registerTooltip() {
ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            if (stack.isOf(Items.COPPER_BLOCK)) {
                lines.add(Text.literal("Interact with Spawn Egg:")
                        .formatted(Formatting.GRAY)
                        .styled(style -> style.withItalic(false)));
                lines.add(Text.literal("Sets Mob Type")
                        .formatted(Formatting.GOLD)
                        .styled(style -> style.withItalic(false)));
            }
        });
    }

    private static void registerBlockInteractions() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient() || hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos().toImmutable();
            if (!world.getBlockState(pos).isOf(Blocks.COPPER_BLOCK)) {
                return ActionResult.PASS;
            }

            ItemStack held = player.getStackInHand(hand);
            if (held.isOf(Items.COPPER_BLOCK)) {
                int amountAdded = Math.max(1, held.getCount());
                int total = LOCAL_STACKS.getOrDefault(pos, INITIAL_LOCAL_STACK) + amountAdded;
                LOCAL_STACKS.put(pos, total);

                Text message = Text.empty()
                        .append(Text.literal("Bạn đã đặt thêm [").formatted(Formatting.YELLOW))
                        .append(Text.literal(Integer.toString(amountAdded)).formatted(Formatting.GREEN))
                        .append(Text.literal("] Lồng triệu hồi Skeleton, tổng [").formatted(Formatting.YELLOW))
                        .append(Text.literal(Integer.toString(total)).formatted(Formatting.GREEN))
                        .append(Text.literal("]").formatted(Formatting.YELLOW));
                player.sendMessage(message, true);
                world.playSound(player, pos, SoundEvents.BLOCK_METAL_PLACE,
                        SoundCategory.BLOCKS, 1.0f, 0.9f + world.random.nextFloat() * 0.2f);
                // FAIL cancels the normal interaction and does not send a server packet.
                return ActionResult.FAIL;
            }

            if (held.isEmpty() || held.isIn(ItemTags.PICKAXES)) {
                MinecraftClient.getInstance().setScreen(new FakeSpawnerScreen(pos));
                // The chest-like menu is local UI only; prevent the block interaction packet.
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient() || hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            if (!world.getBlockState(pos).isOf(Blocks.COPPER_BLOCK)) {
                return ActionResult.PASS;
            }
            if (!player.getMainHandStack().isSuitableFor(world.getBlockState(pos))) {
                return ActionResult.PASS;
            }

            int count = LOCAL_STACKS.getOrDefault(pos.toImmutable(), INITIAL_LOCAL_STACK);
            Text message = Text.empty()
                    .append(Text.literal("Bạn đã đập [").formatted(Formatting.YELLOW))
                    .append(Text.literal(Integer.toString(count)).formatted(Formatting.GREEN))
                    .append(Text.literal("] Lồng triệu hồi Skeleton, Còn lại 0").formatted(Formatting.YELLOW));
            player.sendMessage(message, true);
            LOCAL_STACKS.remove(pos.toImmutable());
            removePreviewSkeleton(pos.toImmutable());
            // Let vanilla/server handle the actual copper block break and its normal drop.
            return ActionResult.PASS;
        });
    }

    private static void registerClientTick() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) {
                clearPreviewsForWorldChange(null);
                return;
            }

            if (lastWorld != client.world) {
                clearPreviewsForWorldChange(client.world);
            }

            localWorldTime++;
            if (--scanCooldown <= 0) {
                scanCooldown = 8;
                scanNearbyCopperBlocks(client);
            }
            tickPreviewSkeletons(client.world);
        });
    }

    private static void clearPreviewsForWorldChange(ClientWorld newWorld) {
        if (lastWorld != null) {
            for (SkeletonEntity skeleton : PREVIEW_SKELETONS.values()) {
                if (!skeleton.isRemoved()) {
                    lastWorld.removeEntity(skeleton.getId(), Entity.RemovalReason.DISCARDED);
                }
            }
        }
        PREVIEW_SKELETONS.clear();
        LOCAL_STACKS.clear();
        lastWorld = newWorld;
    }

    private static void scanNearbyCopperBlocks(MinecraftClient client) {
        ClientWorld world = client.world;
        BlockPos origin = client.player.getBlockPos();
        Set<BlockPos> seen = new HashSet<>();

        for (int dx = -SCAN_RADIUS_XZ; dx <= SCAN_RADIUS_XZ; dx++) {
            for (int dy = -SCAN_RADIUS_Y; dy <= SCAN_RADIUS_Y; dy++) {
                for (int dz = -SCAN_RADIUS_XZ; dz <= SCAN_RADIUS_XZ; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
                        continue;
                    }
                    if (world.getBlockState(pos).isOf(Blocks.COPPER_BLOCK)) {
                        BlockPos stablePos = pos.toImmutable();
                        seen.add(stablePos);
                        PREVIEW_SKELETONS.computeIfAbsent(stablePos, p -> createPreviewSkeleton(world, p));
                    }
                }
            }
        }

        List<BlockPos> stale = new ArrayList<>();
        for (BlockPos pos : PREVIEW_SKELETONS.keySet()) {
            if (!seen.contains(pos)) {
                stale.add(pos);
            }
        }
        for (BlockPos pos : stale) {
            // Keep the local stack count while the player moves out of render range.
            removePreviewSkeleton(pos);
        }
    }

    private static SkeletonEntity createPreviewSkeleton(ClientWorld world, BlockPos pos) {
        SkeletonEntity skeleton = new SkeletonEntity(EntityType.SKELETON, world);
        skeleton.setId(-100_000 - Math.floorMod(pos.hashCode(), 1_000_000_000));
        skeleton.setPosition(pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5);
        skeleton.setNoGravity(true);
        skeleton.setAiDisabled(true);
        skeleton.setSilent(true);
        skeleton.setInvulnerable(true);
        skeleton.setCustomNameVisible(false);

        EntityAttributeInstance scale = skeleton.getAttributeInstance(EntityAttributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(0.35);
        }

        world.addEntity(skeleton);
        return skeleton;
    }

    private static void tickPreviewSkeletons(ClientWorld world) {
        if (PREVIEW_SKELETONS.isEmpty()) {
            return;
        }
        Random random = world.random;
        for (Map.Entry<BlockPos, SkeletonEntity> entry : PREVIEW_SKELETONS.entrySet()) {
            BlockPos pos = entry.getKey();
            SkeletonEntity skeleton = entry.getValue();
            if (skeleton.isRemoved()) {
                continue;
            }

            float yaw = (float) ((localWorldTime * 2.2 + Math.floorMod(pos.hashCode(), 360)) % 360.0);
            skeleton.setYaw(yaw);
            skeleton.setHeadYaw(yaw);
            skeleton.setBodyYaw(yaw);
skeleton.setPosition(pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5);

if (localWorldTime % 5 == 0) {
                double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.55;
                double y = pos.getY() + 0.45 + random.nextDouble() * 0.55;
                double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.55;
                world.addParticleClient(ParticleTypes.SMOKE, x, y, z,
                        (random.nextDouble() - 0.5) * 0.015, 0.025, (random.nextDouble() - 0.5) * 0.015);
                if (localWorldTime % 10 == 0) {
                    world.addParticleClient(ParticleTypes.FLAME, x, y, z,
                            (random.nextDouble() - 0.5) * 0.01, 0.02, (random.nextDouble() - 0.5) * 0.01);
                }
            }
        }
    }

    public static void removePreviewSkeleton(BlockPos pos) {
        if (skeleton != null && !skeleton.isRemoved() && lastWorld != null) {
            lastWorld.removeEntity(skeleton.getId(), Entity.RemovalReason.DISCARDED);
        }
    }

    public static int getLocalStackCount(BlockPos pos) {
        return LOCAL_STACKS.getOrDefault(pos, INITIAL_LOCAL_STACK);
    }

    public static int getLocalExperience() {
        return localExperience;
    }

    public static void collectLocalExperience() {
        int gained = localExperience;
        if (gained <= 0) {
            return;
        }
        localExperience = 0;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            client.player.sendMessage(Text.literal("+EXP " + gained).formatted(Formatting.GREEN), true);
            client.world.playSound(client.player, client.player.getBlockPos(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.8f, 1.15f);
            client.world.playSound(client.player, client.player.getBlockPos(),
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.35f, 1.2f);
        }
        rewardHudExpiresAt = System.currentTimeMillis() + 14_000L;
    }

    private static void registerRewardHud() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (System.currentTimeMillis() > rewardHudExpiresAt) {
                return;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) {
                return;
            }
            int x = 8;
            int y = Math.max(8, drawContext.getScaledWindowHeight() - 86);
            drawContext.drawText(client.textRenderer,
                    Text.literal("+$ 53.76K").formatted(Formatting.GREEN), x, y, 0xFF55FF55, true);
            drawContext.drawText(client.textRenderer,
                    Text.literal("+EXP 512").formatted(Formatting.GREEN), x, y + 12, 0xFF55FF55, true);
        });
    }

}
