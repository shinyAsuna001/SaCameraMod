package com.samod.sacameramod;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CameraCommand {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(createRoot());
    }

    private static LiteralArgumentBuilder<CommandSource> createRoot() {
        return Commands.literal("sacamera")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(createShake())
                        .then(createMove())
                        .then(createRotate())
                        .then(createCinema())
                        .then(createTogglePerspective())
                        .then(createCancel()));
    }

    private static LiteralArgumentBuilder<CommandSource> createShake() {
        return Commands.literal("shake")
                .then(Commands.literal("positioned")
                        .then(Commands.argument("strength", FloatArgumentType.floatArg(0f))
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                        .executes(ctx -> executeShake(ctx, false)))))
                .then(Commands.literal("rotationed")
                        .then(Commands.argument("strength", FloatArgumentType.floatArg(0f))
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                        .executes(ctx -> executeShake(ctx, true)))))
                ;
    }

    private static LiteralArgumentBuilder<CommandSource> createMove() {
        return Commands.literal("move")
                .then(Commands.literal("forward")
                        .then(Commands.argument("dx", FloatArgumentType.floatArg())
                                .then(Commands.argument("dy", FloatArgumentType.floatArg())
                                        .then(Commands.argument("dz", FloatArgumentType.floatArg())
                                                .then(Commands.argument("fadeIn", IntegerArgumentType.integer(0))
                                                        .then(Commands.argument("hold", IntegerArgumentType.integer(0))
                                                                .then(Commands.argument("fadeOut", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> executeMoveForward(ctx)))))))))
                .then(Commands.literal("relative")
                        .then(Commands.argument("dx", FloatArgumentType.floatArg())
                                .then(Commands.argument("dy", FloatArgumentType.floatArg())
                                        .then(Commands.argument("dz", FloatArgumentType.floatArg())
                                                .then(Commands.argument("fadeIn", IntegerArgumentType.integer(0))
                                                        .then(Commands.argument("hold", IntegerArgumentType.integer(0))
                                                                .then(Commands.argument("fadeOut", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> executeMove(ctx, true)))))))))
                .then(Commands.argument("dx", FloatArgumentType.floatArg())
                        .then(Commands.argument("dy", FloatArgumentType.floatArg())
                                .then(Commands.argument("dz", FloatArgumentType.floatArg())
                                        .then(Commands.argument("fadeIn", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("hold", IntegerArgumentType.integer(0))
                                                        .then(Commands.argument("fadeOut", IntegerArgumentType.integer(0))
                                                                .executes(ctx -> executeMove(ctx, false))))))));
    }

        private static LiteralArgumentBuilder<CommandSource> createCinema() {
                return Commands.literal("cinema")
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("path", StringArgumentType.greedyString())
                                                        .executes(ctx -> executeCinema(ctx))));
        }

    private static LiteralArgumentBuilder<CommandSource> createCancel() {
        return Commands.literal("cancel")
                .executes(ctx -> executeCancel(ctx));
    }

    private static LiteralArgumentBuilder<CommandSource> createTogglePerspective() {
        return Commands.literal("toggleperspective")
                .then(Commands.argument("mode", IntegerArgumentType.integer(0, 2))
                        .executes(ctx -> executeTogglePerspective(ctx)));
    }

    private static LiteralArgumentBuilder<CommandSource> createRotate() {
        return Commands.literal("rotate")
                .then(Commands.literal("relative")
                        .then(Commands.argument("axis", StringArgumentType.word())
                                .then(Commands.argument("angle", FloatArgumentType.floatArg())
                                        .then(Commands.argument("fadeIn", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("hold", IntegerArgumentType.integer(0))
                                                        .then(Commands.argument("fadeOut", IntegerArgumentType.integer(0))
                                                                .executes(ctx -> executeRotate(ctx, true))))))))
                .then(Commands.argument("axis", StringArgumentType.word())
                        .then(Commands.argument("angle", FloatArgumentType.floatArg())
                                .then(Commands.argument("fadeIn", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("hold", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("fadeOut", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> executeRotate(ctx, false)))))));
    }

        private static int executeShake(CommandContext<CommandSource> ctx, boolean rotationed) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgument.getPlayers(ctx, "targets");
        float strength = FloatArgumentType.getFloat(ctx, "strength");
        int duration = IntegerArgumentType.getInteger(ctx, "duration");
        PacketCameraEffect packet = PacketCameraEffect.createShake(rotationed, strength, duration);
        for (ServerPlayerEntity player : targets) {
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
        ctx.getSource().sendSuccess(new StringTextComponent("Sent camera shake to " + targets.size() + " player(s)."), true);
        return targets.size();
    }

        private static int executeCinema(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
                Collection<ServerPlayerEntity> targets = EntityArgument.getPlayers(ctx, "targets");
                int duration = IntegerArgumentType.getInteger(ctx, "duration");
                String path = StringArgumentType.getString(ctx, "path");
                PacketCameraCinematic packet = PacketCameraCinematic.create(duration, path);
                for (ServerPlayerEntity player : targets) {
                        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
                }
                ctx.getSource().sendSuccess(new StringTextComponent("Sent camera cinema to " + targets.size() + " player(s)."), true);
                return targets.size();
        }

        private static int executeTogglePerspective(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
                Collection<ServerPlayerEntity> targets = EntityArgument.getPlayers(ctx, "targets");
                int mode = IntegerArgumentType.getInteger(ctx, "mode");
                PacketCameraPerspective packet = PacketCameraPerspective.create(mode);
                for (ServerPlayerEntity player : targets) {
                        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
                }
                ctx.getSource().sendSuccess(new StringTextComponent("Sent perspective toggle to " + targets.size() + " player(s)."), true);
                return targets.size();
        }

        private static int executeMove(CommandContext<CommandSource> ctx, boolean relative) throws CommandSyntaxException {
                Collection<ServerPlayerEntity> targets = EntityArgument.getPlayers(ctx, "targets");
                float dx = FloatArgumentType.getFloat(ctx, "dx");
                float dy = FloatArgumentType.getFloat(ctx, "dy");
                float dz = FloatArgumentType.getFloat(ctx, "dz");
                int fadeIn = IntegerArgumentType.getInteger(ctx, "fadeIn");
                int hold = IntegerArgumentType.getInteger(ctx, "hold");
                int fadeOut = IntegerArgumentType.getInteger(ctx, "fadeOut");
                PacketCameraEffect packet = PacketCameraEffect.createMove(dx, dy, dz, relative, fadeIn, hold, fadeOut);
                for (ServerPlayerEntity player : targets) {
                        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
                }
                ctx.getSource().sendSuccess(new StringTextComponent("Sent camera move to " + targets.size() + " player(s)."), true);
                return targets.size();
        }

        private static int executeMoveForward(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
                Collection<ServerPlayerEntity> targets = EntityArgument.getPlayers(ctx, "targets");
                float dx = FloatArgumentType.getFloat(ctx, "dx");
                float dy = FloatArgumentType.getFloat(ctx, "dy");
                float dz = FloatArgumentType.getFloat(ctx, "dz");
                int fadeIn = IntegerArgumentType.getInteger(ctx, "fadeIn");
                int hold = IntegerArgumentType.getInteger(ctx, "hold");
                int fadeOut = IntegerArgumentType.getInteger(ctx, "fadeOut");
                PacketCameraEffect packet = PacketCameraEffect.createMoveForward(dx, dy, dz, fadeIn, hold, fadeOut);
                for (ServerPlayerEntity player : targets) {
                        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
                }
                ctx.getSource().sendSuccess(new StringTextComponent("Sent camera forward-move to " + targets.size() + " player(s)."), true);
                return targets.size();
        }

        private static int executeCancel(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
                Collection<ServerPlayerEntity> targets = EntityArgument.getPlayers(ctx, "targets");
                PacketCameraEffect packet = new PacketCameraEffect(PacketCameraEffect.TYPE_CANCEL, 0f, 0f, 0f, false, false, 0f, 0, 0, 0);
                for (ServerPlayerEntity player : targets) {
                        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
                }
                ctx.getSource().sendSuccess(new StringTextComponent("Sent camera cancel to " + targets.size() + " player(s)."), true);
                return targets.size();
        }

        private static int executeRotate(CommandContext<CommandSource> ctx, boolean relative) throws CommandSyntaxException {
                Collection<ServerPlayerEntity> targets = EntityArgument.getPlayers(ctx, "targets");
                String axis = StringArgumentType.getString(ctx, "axis").toLowerCase();
                float angle = FloatArgumentType.getFloat(ctx, "angle");
                int fadeIn = IntegerArgumentType.getInteger(ctx, "fadeIn");
                int hold = IntegerArgumentType.getInteger(ctx, "hold");
                int fadeOut = IntegerArgumentType.getInteger(ctx, "fadeOut");
                PacketCameraEffect packet = PacketCameraEffect.createRotate(axis, angle, relative, fadeIn, hold, fadeOut);
                for (ServerPlayerEntity player : targets) {
                        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
                }
                ctx.getSource().sendSuccess(new StringTextComponent("Sent camera rotate to " + targets.size() + " player(s)."), true);
                return targets.size();
        }
}
