package com.dmaryanskiy.vanillareforged.test;

import io.netty.channel.embedded.EmbeddedChannel;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;

/** A clientless connection for tests that need login hooks, advancement packets or respawning. */
final class TestPlayers {
	private TestPlayers() {
	}

	static ServerPlayer connect(GameTestHelper helper, GameType mode) {
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(mode);
		connect(helper, player);
		return player;
	}

	static void connect(GameTestHelper helper, ServerPlayer player) {
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(player.getGameProfile(), false);
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
	}
}
