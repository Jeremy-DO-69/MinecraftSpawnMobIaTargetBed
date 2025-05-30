package com.jdo.CustomMobsSpawnIa.command;

import com.jdo.CustomMobsSpawnIa.ai.BreakBlocksToBedGoal;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mojang.text2speech.Narrator.LOGGER;

public class SpawnMobCommand {

    public static void logMobGoals(Mob mob) {
        LOGGER.info("=== [Mob Goals: {}] ===", mob.getType().toString());

        LOGGER.info("➡️ goalSelector:");
        for (WrappedGoal goal : mob.goalSelector.getAvailableGoals()) {
            Goal inner = goal.getGoal();
            LOGGER.info(" - [{}] {}", goal.getPriority(), inner.getClass().getName());
        }

        LOGGER.info("➡️ targetSelector:");
        for (WrappedGoal goal : mob.targetSelector.getAvailableGoals()) {
            Goal inner = goal.getGoal();
            LOGGER.info(" - [{}] {}", goal.getPriority(), inner.getClass().getName());
        }

        LOGGER.info("===========================");
    }

    public static void removeGoalsOfType(Mob mob, Class<? extends Goal> goalClass) {
        Set<WrappedGoal> toRemove = new HashSet<>();

        for (WrappedGoal goal : mob.goalSelector.getAvailableGoals()) {
            if (goalClass.isAssignableFrom(goal.getGoal().getClass())) {
                toRemove.add(goal);
            }
        }

        toRemove.forEach(g -> mob.goalSelector.removeGoal(g.getGoal()));
        LOGGER.info("🧹 Supprimé {} goal(s) de type {} du goalSelector de {}",
                toRemove.size(), goalClass.getSimpleName(), mob.getType());
    }

    public static void removeTargetGoalsOfType(Mob mob, Class<? extends Goal> goalClass) {
        Set<WrappedGoal> toRemove = new HashSet<>();

        for (WrappedGoal goal : mob.targetSelector.getAvailableGoals()) {
            if (goalClass.isAssignableFrom(goal.getGoal().getClass())) {
                toRemove.add(goal);
            }
        }

        toRemove.forEach(g -> mob.targetSelector.removeGoal(g.getGoal()));
        LOGGER.info("🧹 Supprimé {} goal(s) de type {} du targetSelector de {}",
                toRemove.size(), goalClass.getSimpleName(), mob.getType());
    }

    public static int spawnWave(ServerLevel level, ResourceLocation id, int count, int target, double x, double y, double z) {

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) {
            return 0;
        }
        for (int i = 0; i < count; i++) {
            Entity entity = type.create(level);
            if (entity instanceof Mob mob) {
                //logMobGoals(mob);
                removeTargetGoalsOfType(mob, NearestAttackableTargetGoal.class);
                //logMobGoals(mob);
                double offsetX = (Math.random() - 0.5) * 4;
                double offsetZ = (Math.random() - 0.5) * 4;
                mob.setPos(x + offsetX, y, z + offsetZ);
                level.addFreshEntity(mob);
                mob.goalSelector.addGoal(1, new BreakBlocksToBedGoal(mob, target));
            }
        }
        return 1;
    }

    /*public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("spawnwave")
                        .then(Commands.argument("modid", StringArgumentType.word())
                                .then(Commands.argument("mobname", StringArgumentType.word())
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                                .then(Commands.argument("target", IntegerArgumentType.integer(0, 1))
                                                        .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                                                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                                                .executes(ctx -> {
                                                                                    String modid = StringArgumentType.getString(ctx, "modid");
                                                                                    String mobname = StringArgumentType.getString(ctx, "mobname");
                                                                                    int count = IntegerArgumentType.getInteger(ctx, "count");
                                                                                    int target = IntegerArgumentType.getInteger(ctx, "target");
                                                                                    double x = DoubleArgumentType.getDouble(ctx, "x");
                                                                                    double y = DoubleArgumentType.getDouble(ctx, "y");
                                                                                    double z = DoubleArgumentType.getDouble(ctx, "z");
                                                                                    CommandSourceStack source = ctx.getSource();
                                                                                    ServerLevel level = source.getLevel();
                                                                                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(modid, mobname);
                                                                                    int result = spawnWave(level, id, count, target, x, y, z);

                                                                                    if (result == 0) {
                                                                                        source.sendFailure(Component.literal("Entity not found: " + id));
                                                                                        return result;
                                                                                    }
                                                                                    return result;
                                                                                }))))))))
                );
    }*/
}
