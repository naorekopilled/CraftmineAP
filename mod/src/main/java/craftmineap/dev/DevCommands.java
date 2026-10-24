package craftmineap.dev;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.mines.WorldEffect;

/** Test-only commands for tools/smoke.sh (the game's own unlock commands only exist in Mojang's IDE builds). */
public final class DevCommands {
	private DevCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("apdev")
			.then(literal("unlock_effect").then(argument("id", StringArgumentType.word()).executes(ctx -> {
				hub(ctx.getSource()).unlockEffect(effect(ctx.getArgument("id", String.class)));
				return 1;
			})))
			.then(literal("effect_unlocked").then(argument("id", StringArgumentType.word()).executes(ctx -> {
				String id = ctx.getArgument("id", String.class);
				boolean unlocked = hub(ctx.getSource()).isEffectUnlocked(effect(id));
				ctx.getSource().sendSuccess(() -> Component.literal("apdev " + id + " unlocked=" + unlocked), false);
				return 1;
			})))
			.then(literal("crafter_xp").then(argument("amount", IntegerArgumentType.integer(1)).executes(ctx -> {
				ServerLevel hub = hub(ctx.getSource());
				hub.addExperienceToMineCrafter(ctx.getArgument("amount", Integer.class));
				ctx.getSource().sendSuccess(() -> Component.literal("apdev crafter level=" + hub.getMineCrafterLevel()), false);
				return 1;
			}))));
	}

	private static ServerLevel hub(CommandSourceStack source) {
		return source.theGame().overworld();
	}

	private static WorldEffect effect(String id) {
		return BuiltInRegistries.WORLD_EFFECT.getValue(ResourceLocation.withDefaultNamespace(id));
	}
}
