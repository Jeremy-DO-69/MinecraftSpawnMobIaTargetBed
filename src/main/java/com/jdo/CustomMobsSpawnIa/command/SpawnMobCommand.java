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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.registries.ForgeRegistries;

public class SpawnMobCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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
                                                                                    EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);

                                                                                    if (type == null) {
                                                                                        source.sendFailure(Component.literal("Entity not found: " + id));
                                                                                        return 0;
                                                                                    }

                                                                                    for (int i = 0; i < count; i++) {
                                                                                        Entity entity = type.create(level);
                                                                                        if (entity instanceof Mob mob) {
                                                                                            double offsetX = (Math.random() - 0.5) * 4;
                                                                                            double offsetZ = (Math.random() - 0.5) * 4;
                                                                                            mob.setPos(x + offsetX, y, z + offsetZ);
                                                                                            level.addFreshEntity(mob);
                                                                                            mob.goalSelector.addGoal(1, new BreakBlocksToBedGoal(mob, target));
                                                                                        }
                                                                                    }

                                                                                    source.sendSuccess(() ->
                                                                                                    Component.literal("Spawned " + count + " " + id + " at [" + x + ", " + y + ", " + z + "] targeting bed " + target),
                                                                                            true
                                                                                    );

                                                                                    return 1;
                                                                                }))))))))
                );
    }
}
