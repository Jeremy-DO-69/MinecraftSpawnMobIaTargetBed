package com.jdo.CustomMobsSpawnIa.ai;
import com.jdo.modbedwarsmanager.ModBedwarsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.level.block.state.properties.BedPart;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import static com.jdo.modbedwarsmanager.ModBedwarsManager.BedDestroyed;
import static com.jdo.modbedwarsmanager.ModBedwarsManager.currentMode;


public class BreakBlocksToBedGoal extends Goal {

    private BlockPos breakingBlockFront = null;
    private int breakProgressFront = 0;
    private int BREAK_TIME = 40;
    private final Mob mob;
    private final BlockPos targetBed;
    private final BlockPos targetBedOtherHalf;
    private static final Logger LOGGER = LoggerFactory.getLogger("SpawnMobCommand");
    private Player targetBedPlayer = null;
    private Player targetPlayer;

    private void setTargetPlayer(Player player) {
        targetPlayer = player;
    }

    public BreakBlocksToBedGoal(Mob mob, BlockPos targetBed, Player player) {
        this.targetBedPlayer = player;
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.targetBed = targetBed;
        BREAK_TIME = 60 - (int) (((mob.getBbHeight() % 10) + 1) * 10);
        LOGGER.info(" Breaktime of {} = {}", mob.getName() , BREAK_TIME);
        BlockState state = mob.level().getBlockState(targetBed);
        boolean isHead = state.getValue(BedBlock.PART) == BedPart.HEAD;
        this.targetBedOtherHalf = isHead
                ? targetBed.relative(state.getValue(BedBlock.FACING).getOpposite())
                : targetBed.relative(state.getValue(BedBlock.FACING));
    }

    public static final Set<String> EXPLODING_MOBS = Set.of(
            "minecraft:creeper",
            "mutantmonsters:creeper_minion"
    );

    public static final Set<String> STATIC_ALLOWED_BLOCKS = Set.of(
            "fetzisasiandeco:framed_block_fence",
            "minecraft:oak_fence_gate",
            "handcrafted:spruce_corner_trim",
            "fetzisasiandeco:white_roof_slab_long_framed_block",
            "fetzisasiandeco:white_roof_slab_framed_block",
            "minecraft:soul_lantern",
            "minecraft:stripped_spruce_log",
            "fetzisasiandeco:light_gray_roof_block_framed_block",
            "minecraft:stripped_spruce_wood",
            "fetzisasiandeco:light_gray_roof_stairs_long_framed_block",
            "fetzisasiandeco:white_roof_stairs_framed_block",
            "minecraft:barrel",
            "minecraft:ladder",
            "handcrafted:jungle_fancy_bed",
            "minecraft:spruce_trapdoor",
            "minecraft:white_banner",
            "fetzisasiandeco:white_roof_block_framed_block",
            "minecraft:lantern",
            "minecraft:dark_oak_stairs",
            "minecraft:stone_brick_stairs",
            "minecraft:stripped_dark_oak_log",
            "minecraft:cracked_stone_bricks",
            "minecraft:stone_bricks",
            "minecraft:stone_brick_wall",
            "supplementaries:timber_cross_brace",
            "supplementaries:daub_cross_brace",
            "minecraft:stripped_dark_oak_wood",
            "minecraft:red_banner",
            "minecraft:red_bed",
            "supplementaries:stone_tile",
            "minecraft:oak_fence",
            "minecraft:oak_door",
            "supplementaries:daub_frame"
    );

    public static boolean isBlockAllowed(String id) {
        LOGGER.error("isBlockAllowed :" + id);
        return STATIC_ALLOWED_BLOCKS.contains(id);
    }

    @Override
    public void tick() {
        tickBreakingPathv2(mob, targetBed, targetBedOtherHalf);
    }

    @Override
    public boolean canUse() {
        if (targetPlayer != null) {
            if (!targetPlayer.isAlive()) {
                mob.setLastHurtByPlayer(null);
                mob.setLastHurtByMob(null);
                setTargetPlayer(null);
                return true;
            }
            return false;
        } else {
            return true;
        }
    }

    public static BlockPos scan3DBoxInFrontOfMob(Mob mob, Level level, BlockPos targetPos) {
        Vec3 targetCenter = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        Vec3 direction = targetCenter.subtract(mob.position()).normalize();

        int height = (int) Math.ceil(mob.getBbHeight()) - 1;
        int width = (int) Math.ceil(mob.getBbWidth()) > 1 ? (int) Math.ceil(mob.getBbWidth()) / 2 : 0;
        int depth = (int) Math.ceil(mob.getBbWidth());
        Vec3 base = mob.position();

        for (int dz = 1; dz <= depth; dz++) {
            Vec3 forward = base.add(direction.scale(dz));
            for (int dx = -width; dx <= width; dx++) {
                for (int dy = 0; dy <= height; dy++) {
                    Vec3 offset = forward.add(
                            mob.getLookAngle().yRot((float)Math.PI / 2).normalize().scale(dx)
                    ).add(0, dy, 0);

                    BlockPos checkPos = new BlockPos(
                            (int) Math.floor(offset.x),
                            (int) Math.floor(offset.y),
                            (int) Math.floor(offset.z)
                    );

                    BlockState state = level.getBlockState(checkPos);
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    if (!state.isAir() && state.canOcclude() && isBlockAllowed(id.toString())) {
                        return checkPos;
                    }
                }
            }
        }

        return null;
    }

    public void tickBreakingPathv2(Mob mob, BlockPos bedPos , BlockPos bedPosOtherHalf) {
        if (currentMode != ModBedwarsManager.Mode.NOTHING) {
            Level level = mob.level();
            if (mob.getLastAttacker() instanceof Player player) {
                if (player == targetBedPlayer) {
                    if (player.isAlive()) {
                        breakingBlockFront = null;
                        setTargetPlayer(player);
                        mob.setTarget(player);
                    } else {
                        setTargetPlayer(null);
                    }
                }
            }

            if (targetPlayer == null) {
                if (breakingBlockFront == null) {
                    mob.getNavigation().moveTo(bedPos.getX(), bedPos.getY(), bedPos.getZ(), 1.0D);
                } else {
                    mob.getNavigation().stop();
                }

                double margin = 1.9;


                boolean nearBed1 =
                        (Math.abs(mob.getX() - bedPos.getX()) - (mob.getBbWidth())) <= margin &&
                                (Math.abs(mob.getZ() - bedPos.getZ()) - (mob.getBbWidth())) <= margin &&
                                (Math.abs(mob.getY() - bedPos.getY()) - (mob.getBbWidth())) <= margin;

                boolean nearBed2 =
                        (Math.abs(mob.getX() - bedPosOtherHalf.getX()) - (mob.getBbWidth())) <= margin &&
                                (Math.abs(mob.getZ() - bedPosOtherHalf.getZ()) - (mob.getBbWidth())) <= margin &&
                                (Math.abs(mob.getY() - bedPosOtherHalf.getY()) - (mob.getBbWidth())) <= margin;

                if (nearBed1 || nearBed2) {

                    ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
                    if (id != null && EXPLODING_MOBS.contains(id.toString())) {
                        mob.level().explode(mob, mob.getX(), mob.getY(), mob.getZ(), 3.0F, Level.ExplosionInteraction.MOB);
                        mob.discard();
                    } else {
                        level.destroyBlock(bedPos, true, mob);
                        BedDestroyed(targetBedPlayer);
                    }
                }

                if (breakingBlockFront == null) {
                    BlockPos block = scan3DBoxInFrontOfMob(mob, level, bedPos);
                    if (block != null) {
                        breakingBlockFront = block;
                        breakProgressFront = 0;
                    }
                } else {
                    breakProgressFront++;
                    if (breakProgressFront >= BREAK_TIME) {
                        mob.level().destroyBlock(breakingBlockFront, true, mob);
                        breakingBlockFront = null;
                        breakProgressFront = 0;
                    }
                }
            }
        }
    }
}