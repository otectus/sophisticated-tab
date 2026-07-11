package dev.otectus.sophisticatedtab.client;

import dev.otectus.sophisticatedtab.client.compat.legendarytabs.LegendaryTabsCompat;
import dev.otectus.sophisticatedtab.client.input.KeyBindings;
import dev.otectus.sophisticatedtab.client.input.TabInteractionHandler;
import dev.otectus.sophisticatedtab.client.prefs.PreferencesStorage;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientBootstrap {
    private static final Logger LOG = LoggerFactory.getLogger("sophisticatedtab");

    private ClientBootstrap() {}

    public static void onClientSetup(FMLClientSetupEvent event) {
        ModList mods = ModList.get();
        boolean hasLegendaryTabs = mods.isLoaded("legendarytabs");
        boolean hasSophisticatedBackpacks = mods.isLoaded("sophisticatedbackpacks");

        if (!hasLegendaryTabs || !hasSophisticatedBackpacks) {
            LOG.warn(
                    "Skipping LegendaryTabs registration (legendarytabs={}, sophisticatedbackpacks={}).",
                    hasLegendaryTabs,
                    hasSophisticatedBackpacks);
            return;
        }

        event.enqueueWork(() -> {
            PreferencesStorage.loadAll();
            LegendaryTabsCompat.register();
        });

        MinecraftForge.EVENT_BUS.register(ClientBootstrap.class);
        MinecraftForge.EVENT_BUS.register(TabInteractionHandler.class);
    }

    // Flush pending preference writes at most once per tick. Mutations mark the
    // model dirty; this drains the flag on the client thread so all I/O stays
    // off the render path.
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PreferencesStorage.flushIfDirty();
        KeyBindings.pollKeys();
    }

    // Guarantees a flush at the moment the player leaves the world / server,
    // before the next world's scope key could overwrite the dirty flag's
    // semantics. Doesn't clear the in-memory PROFILES cache — each
    // BackpackTabPreferences.current() call re-resolves the scope key, so
    // entries from other scopes remain inert but cheaply available on return.
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PreferencesStorage.flushIfDirty();
    }
}
