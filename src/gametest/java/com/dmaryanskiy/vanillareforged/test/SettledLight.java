package com.dmaryanskiy.vanillareforged.test;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Lets light keep up with the tests. Minecraft works light out on another thread, and the test
 * server runs its ticks as fast as it can: on a slow machine hundreds of ticks can pass before a
 * roof casts its shade, so a test that waits "long enough" in ticks for daylight to come or go is a
 * race. A short pause after every tick turns those ticks back into time the light thread can use.
 * <p>
 * Waiting on the light engine itself would be exact, but its tasks don't finish while the server
 * thread is blocked on them.
 */
public class SettledLight implements ModInitializer {
	private static final long PAUSE_MILLIS = 2;

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			try {
				Thread.sleep(PAUSE_MILLIS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
	}
}
