package craftmineap.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import craftmineap.Text;
import craftmineap.ap.ApSession;
import craftmineap.core.ConnectArgs;
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
			.then(argument("address slot [password]", StringArgumentType.greedyString()).executes(ctx -> {
				ConnectArgs args = ConnectArgs.parse(ctx.getArgument("address slot [password]", String.class));
				if (args == null) {
					ctx.getSource().sendFailure(Text.connectUsage());
					return 0;
				}
				ApSession.current().connect(args.address(), args.slot(), args.password());
				return 1;
			})));
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
}
