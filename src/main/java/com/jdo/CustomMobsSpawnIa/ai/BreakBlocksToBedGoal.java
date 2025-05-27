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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.level.block.state.properties.BedPart;
import java.util.EnumSet;


public class BreakBlocksToBedGoal extends Goal {

    private BlockPos breakingBlockFront = null;
    private int breakProgressFront = 0;
    private int BREAK_TIME = 40;
    private final Mob mob;
    private final BlockPos targetBed;
    private final BlockPos targetBedOtherHalf;
    private static final Logger LOGGER = LoggerFactory.getLogger("SpawnMobCommand");
    private Player targetPlayer;

    private void setTargetPlayer(Player player) {
        targetPlayer = player;
    }


    public BreakBlocksToBedGoal(Mob mob, int bedId) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.targetBed = getBedPos(bedId);
        BREAK_TIME = 60 - (int) (((mob.getBbHeight() % 10) + 1) * 10);
        LOGGER.info(" Breaktime of {} = {}", mob.getName() , BREAK_TIME);
        BlockState state = mob.level().getBlockState(targetBed);
        boolean isHead = state.getValue(BedBlock.PART) == BedPart.HEAD;
        this.targetBedOtherHalf = isHead
                ? targetBed.relative(state.getValue(BedBlock.FACING).getOpposite())
                : targetBed.relative(state.getValue(BedBlock.FACING));
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

    public void tickBreakingPath(Mob mob, BlockPos bedPos , BlockPos bedPosOtherHalf) {
        Level level = mob.level();
        if (mob.getLastAttacker() instanceof Player player) {
            if (player.isAlive()) {
                breakingBlockFront = null;
                setTargetPlayer(player);
                mob.setTarget(player);
            } else {
                setTargetPlayer(null);
            }
        }

        if (targetPlayer == null) {
            if (breakingBlockFront == null) {
                mob.getNavigation().moveTo(bedPos.getX(), bedPos.getY(), bedPos.getZ(), 1.0D);
            } else {
                mob.getNavigation().stop();
            }

            if (mob.distanceToSqr(bedPos.getX(), bedPos.getY(), bedPos.getZ()) < 1.5F
                    || mob.distanceToSqr(bedPosOtherHalf.getX(), bedPosOtherHalf.getY(), bedPosOtherHalf.getZ()) < 1.5F) {
                if (mob instanceof Creeper) {
                    mob.level().explode(mob, mob.getX(), mob.getY(), mob.getZ(), 3.0F, Level.ExplosionInteraction.MOB);
                    mob.discard();
                } else {
                    level.destroyBlock(bedPos, true, mob);
                }
            }

            if (breakingBlockFront == null) {
                LOGGER.info("mob height {} {}", mob.getBbHeight(), (int)((mob.getBbHeight() % 10) + 1));

                Vec3 mobCenter = mob.position().add(0, mob.getBbHeight(), 0);
                Vec3 target = new Vec3(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5);

                Vec3 direction = target.subtract(mobCenter).normalize();

                Vec3 frontVec = mob.position().add(direction);
                int front = 0;
                int left = 1;
                int right = 2;
                for (int x = 1; x <= 3; x++) {
                    for (int i = 1; i <= (int)((mob.getBbHeight() % 10) + 1); i++) {
                        int toCheck = 0;
                        switch (x) {
                            case 1:
                                toCheck = front;
                                break;
                            case 2:
                                toCheck = left;
                                break;
                            case 3:
                                toCheck = right;
                                break;

                        }
                        BlockPos frontBlockPos = new BlockPos(
                                (int) Math.floor(frontVec.x),
                                (int) Math.floor(frontVec.y + i),
                                (int) Math.floor(frontVec.z + toCheck)
                        );
                        BlockState state = level.getBlockState(frontBlockPos);

                        if (!state.isAir() && state.canOcclude()) {
                            breakingBlockFront = frontBlockPos;
                            breakProgressFront = 0;
                            return;
                        }
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
        }
    }

    /*public void oldTickBreakingPath() {
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
    }*/

    @Override
    public void tick() {
        //oldTickBreakingPath();
        tickBreakingPath(mob, targetBed, targetBedOtherHalf);
    }
}