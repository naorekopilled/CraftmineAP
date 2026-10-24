package craftmineap.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import craftmineap.Text;
import craftmineap.ap.ApSession;
import net.minecraft.commands.CommandSourceStack;

/** /connect, /disconnect, /sync and /start, named like the Minecraft AP mod's. */
public final class ApCommands {
	private ApCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("connect")
			.executes(ctx -> {
				ApSession session = ApSession.current();
				if (session.state.address == null) {
					ctx.getSource().sendFailure(Text.noSavedConnection());
					return 0;
				}
				session.connect(session.state.address, session.state.slot, session.state.password);
				return 1;
			})
			.then(argument("address", StringArgumentType.string())
				.then(argument("slot", StringArgumentType.string())
					.executes(ctx -> connect(ctx.getArgument("address", String.class), ctx.getArgument("slot", String.class), null))
					.then(argument("password", StringArgumentType.greedyString())
						.executes(ctx -> connect(ctx.getArgument("address", String.class), ctx.getArgument("slot", String.class),
							ctx.getArgument("password", String.class)))))));
		dispatcher.register(literal("disconnect").executes(ctx -> {
			ApSession.current().disconnect();
			return 1;
		}));
		dispatcher.register(literal("sync").executes(ctx -> {
			ApSession.current().sync();
			ctx.getSource().sendSuccess(Text::synced, false);
			return 1;
		}));
		dispatcher.register(literal("start").executes(ctx -> {
			ApSession session = ApSession.current();
			if (session.slotData() == null) {
				ctx.getSource().sendFailure(Text.notConnectedYet());
				return 0;
			}
			session.start();
			return 1;
		}));
	}

	private static int connect(String address, String slot, String password) {
		ApSession.current().connect(address, slot, password);
		return 1;
	}
}
