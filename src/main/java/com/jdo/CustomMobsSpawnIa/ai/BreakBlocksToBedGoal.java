package com.jdo.CustomMobsSpawnIa.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import java.util.EnumSet;

public class BreakBlocksToBedGoal extends Goal {

    private static final Logger LOGGER = LoggerFactory.getLogger("SpawnMobCommand");

    private final Mob mob;
    private final BlockPos targetBed;

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

        LOGGER.info("Recherche de lit autour de {}", basePos);

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
            LOGGER.info("Lit détecté à proximité de la cible {} → {}", id, closestBed);
            return closestBed;
        } else {
            LOGGER.warn("Aucun lit trouvé pour la cible {} — fallback à {}", id, basePos);
            return basePos;
        }
    }
    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void tick() {
        if (mob.distanceToSqr(targetBed.getX(), targetBed.getY(), targetBed.getZ()) > 2) {
            mob.getNavigation().moveTo(targetBed.getX(), targetBed.getY(), targetBed.getZ(), 1.0D);
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

        // Essaye de casser jusqu'à 3 blocs de hauteur devant le mob
        for (int i = 0; i <= 2; i++) {
            BlockPos ahead = mob.blockPosition().relative(mob.getDirection()).above(i);
            BlockState blockAhead = mob.level().getBlockState(ahead);
            if (!blockAhead.isAir() && blockAhead.getDestroySpeed(mob.level(), ahead) >= 0) {
                mob.level().destroyBlock(ahead, true);
            }
        }
    }
}