package craftmineap.dev;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import craftmineap.ap.ApSession;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerUnlock;
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
			.then(literal("buy").then(argument("id", StringArgumentType.word()).executes(ctx -> {
				String id = ctx.getArgument("id", String.class);
				FakePlayer player = FakePlayer.get(hub(ctx.getSource()));
				Holder<PlayerUnlock> unlock = unlock(id);
				player.setExperienceLevels(unlock.value().unlockPrice());
				boolean bought = player.buyUnlock(unlock);
				ctx.getSource().sendSuccess(() -> Component.literal("apdev " + id + " bought=" + bought), false);
				return 1;
			})))
			.then(literal("active").then(argument("id", StringArgumentType.word()).executes(ctx -> {
				String id = ctx.getArgument("id", String.class);
				boolean active = FakePlayer.get(hub(ctx.getSource())).isActive(unlock(id));
				ctx.getSource().sendSuccess(() -> Component.literal("apdev " + id + " active=" + active), false);
				return 1;
			})))
			.then(literal("mine_gear").then(argument("item", StringArgumentType.word()).executes(ctx -> {
				String item = ctx.getArgument("item", String.class);
				FakePlayer player = FakePlayer.get(hub(ctx.getSource()));
				player.getInventory().clearContent();
				player.onMineEntered();
				int count = player.getInventory().countItem(BuiltInRegistries.ITEM.getValue(ResourceLocation.withDefaultNamespace(item)));
				ctx.getSource().sendSuccess(() -> Component.literal("apdev mine_gear " + item + "=" + count), false);
				return 1;
			})))
			.then(literal("drop").executes(ctx -> {
				ApSession.current().dropConnection();
				return 1;
			}))
			.then(literal("win_mine").then(argument("id", StringArgumentType.word()).executes(ctx -> {
				ApSession.current().mineWon(ctx.getArgument("id", String.class));
				return 1;
			})))
			// what entering a mine does to the server: the game restarts inside it, without a server stop
			.then(literal("restart").executes(ctx -> {
				ctx.getSource().theGame().server().sayGoodbye();
				return 1;
			}))
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

	private static Holder<PlayerUnlock> unlock(String id) {
		return BuiltInRegistries.PLAYER_UNLOCK.get(ResourceLocation.withDefaultNamespace(id)).orElseThrow();
	}

	private static WorldEffect effect(String id) {
		return BuiltInRegistries.WORLD_EFFECT.getValue(ResourceLocation.withDefaultNamespace(id));
	}
}
