package com.jdo.CustomMobsSpawnIa.ai;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.level.block.state.properties.BedPart;
import java.util.EnumSet;

public class BreakBlocksToBedGoal extends Goal {

    private BlockPos breakingBlockFront = null;
    private BlockPos breakingBlockEast = null;
    private BlockPos breakingBlockWest = null;
    private int breakProgressFront = 0;
    private int breakProgressEast = 0;
    private int breakProgressWest = 0;
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
            basePos = new BlockPos(6, -60, -8);
        } else if (id == 0) {
            basePos = new BlockPos(12, -60, -8);
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

                    if (state.getBlock() instanceof BedBlock) {
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
            return closestBed;
        } else {
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
            if (mob.distanceToSqr(targetBed.getX(), targetBed.getY(), targetBed.getZ()) > 2) {
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

            // Essaye de casser jusqu'à 3 blocs de hauteur devant le mob
            if (breakingBlockFront == null) {
                for (int i = 0; i <= 2; i++) {
                    BlockPos aheadFront = mob.blockPosition().relative(mob.getDirection()).above(i);
                    BlockState blockAheadFront = mob.level().getBlockState(aheadFront);
                    if (!blockAheadFront.isAir() && blockAheadFront.getDestroySpeed(mob.level(), aheadFront) >= 0) {
                        breakingBlockFront = aheadFront;
                        breakProgressFront = 0;
                        return;
                    }
                }
            } else {
                breakProgressFront++;
                if (breakProgressFront >= BREAK_TIME) {
                    mob.level().destroyBlock(breakingBlockFront, true);
                    breakingBlockFront = null;
                    breakProgressFront = 0;
                }
            }
            if (breakingBlockEast == null) {
                for (int i = 0; i <= 2; i++) {
                    BlockPos aheadEast = mob.blockPosition().relative(mob.getDirection()).east(i);
                    BlockState blockAheadEast = mob.level().getBlockState(aheadEast);
                    if (!blockAheadEast.isAir() && blockAheadEast.getDestroySpeed(mob.level(), aheadEast) >= 0) {
                        breakingBlockEast = aheadEast;
                        breakProgressEast = 0;
                        return;
                    }
                }
            } else {
                breakProgressEast++;
                if (breakProgressEast >= BREAK_TIME) {
                    mob.level().destroyBlock(breakingBlockEast, true);
                    breakingBlockEast = null;
                    breakProgressEast = 0;
                }
            }
            if (breakingBlockWest == null) {
                for (int i = 0; i <= 2; i++) {
                    BlockPos aheadWest = mob.blockPosition().relative(mob.getDirection()).west(i);
                    BlockState blockAheadWest = mob.level().getBlockState(aheadWest);

                    if (!blockAheadWest.isAir() && blockAheadWest.getDestroySpeed(mob.level(), aheadWest) >= 0) {
                        breakingBlockWest = aheadWest;
                        breakProgressWest = 0;
                        return;
                    }
                }
            } else {
                breakProgressWest++;
                if (breakProgressWest >= BREAK_TIME) {
                    mob.level().destroyBlock(breakingBlockWest, true);
                    breakingBlockWest = null;
                    breakProgressWest = 0;
                }
            }
        }
    }
}