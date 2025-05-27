package com.jdo.CustomMobsSpawnIa.ai;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.level.block.state.properties.BedPart;
import java.util.EnumSet;


public class BreakBlocksToBedGoal extends Goal {

    private BlockPos breakingBlockFront = null;
    private BlockState stateBlockFront = null;
    private int breakProgressFront = 0;
    private static final int BREAK_TIME = 40;
    private final Mob mob;
    private final BlockPos targetBed;
    private static final Logger LOGGER = LoggerFactory.getLogger("SpawnMobCommand");
    private Player targetPlayer;

    private void setTargetPlayer(Player player) {
        targetPlayer = player;
    }


    public BreakBlocksToBedGoal(Mob mob, int bedId) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.targetBed = getBedPos(bedId);
    }

   private BlockPos getBedPos(int id) {
        BlockPos basePos;

        if (id == 1) {
            basePos = new BlockPos(6, -63, -8);
        } else if (id == 0) {
            basePos = new BlockPos(12, -63, -8);
        } else {
            basePos = mob.blockPosition(); // fallback neutre
        }


        int radius = 4; // petit rayon autour de la coordonnée en dur
        BlockPos closestBed = null;
        double closestDistance = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = basePos.offset(x, y, z);
                    BlockState state = mob.level().getBlockState(checkPos);

                    if (state.getBlock() instanceof BedBlock bedBlock) {

                        double dist = checkPos.distSqr(basePos);
                        if (dist < closestDistance) {
                            closestDistance = dist;
                            closestBed = checkPos;
                        }
                    }
                }
            }
        }

        if (closestBed != null) {
            LOGGER.info("found closestBed");
            return closestBed;
        } else {
            LOGGER.info("found basePos");
            return basePos;
        }
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

    @Override
    public void tick() {
        // Si le joueur ciblé est mort, on revient à l’objectif lit
        if (mob.getLastAttacker() instanceof Player player) {
            if (player.isAlive()) {
                setTargetPlayer(player);
                mob.setTarget(player);
            } else {
                setTargetPlayer(null);
            }
        }
        if (targetPlayer == null) {
            mob.getLookControl().setLookAt(
                    targetBed.getX() + 0.5D,
                    targetBed.getY() + 0.5D,
                    targetBed.getZ() + 0.5D,
                    30.0F,
                    30.0F
            );
            if (breakingBlockFront != null) {
                LOGGER.info("current target {}", breakingBlockFront);
            }
            if (breakingBlockFront == null || stateBlockFront.getBlock() instanceof BedBlock) {
                if (mob.distanceToSqr(targetBed.getX(), targetBed.getY(), targetBed.getZ()) > 2) {
                    LOGGER.info("Move to bed");
                    mob.getNavigation().moveTo(targetBed.getX(), targetBed.getY(), targetBed.getZ(), 1.0D);
                } else {
                    if (mob instanceof Creeper) {
                        if (mob.distanceToSqr(targetBed.getX(), targetBed.getY(), targetBed.getZ()) < 2) {
                            mob.level().explode(mob, mob.getX(), mob.getY(), mob.getZ(), 3.0F, Level.ExplosionInteraction.MOB);
                            mob.discard(); // supprime le creeper
                        }
                    } else {
                        BlockState state = mob.level().getBlockState(targetBed);
                        if (state.getBlock() instanceof BedBlock) {
                            mob.level().destroyBlock(targetBed, true);
                            // 🔄 Détermine l'autre moitié du lit
                            boolean isHead = state.getValue(BedBlock.PART) == BedPart.HEAD;
                            BlockPos otherHalf = isHead
                                    ? targetBed.relative(state.getValue(BedBlock.FACING).getOpposite())
                                    : targetBed.relative(state.getValue(BedBlock.FACING));

                            BlockState otherState = mob.level().getBlockState(otherHalf);
                            if (otherState.getBlock() instanceof BedBlock) {
                                mob.level().destroyBlock(otherHalf, true);
                                LOGGER.info("Lit cassé : {} et {}", targetBed, otherHalf);
                            }
                        }
                    }
                }
            } else {
                mob.getNavigation().stop();
            }

            // Essaye de casser jusqu'à 3 blocs de hauteur devant le mob
            if (breakingBlockFront == null) {
                for (int i = 0; i <= 2; i++) {
                    BlockPos aheadFront = mob.blockPosition().relative(mob.getDirection()).above(i);
                    BlockState blockAheadFront = mob.level().getBlockState(aheadFront);
                    ResourceLocation id = ForgeRegistries.BLOCKS.getKey(blockAheadFront.getBlock());

                    if (id != null) {
                        System.out.println("mob pos: " + aheadFront );
                        System.out.println("Block registry name: " + id.toString());
                    }
                    if (!blockAheadFront.isAir() && blockAheadFront.getDestroySpeed(mob.level(), aheadFront) >= 0) {
                        LOGGER.info("cible un bloc");
                        breakingBlockFront = aheadFront;
                        stateBlockFront = blockAheadFront;
                        breakProgressFront = 0;
                        return;
                    }
                }
            } else {
                breakProgressFront++;
                if (breakProgressFront >= BREAK_TIME) {
                    LOGGER.info("casse un block");
                    mob.level().destroyBlock(breakingBlockFront, true);
                    breakingBlockFront = null;
                    stateBlockFront = null;
                    breakProgressFront = 0;
                }
            }
        }
    }
}