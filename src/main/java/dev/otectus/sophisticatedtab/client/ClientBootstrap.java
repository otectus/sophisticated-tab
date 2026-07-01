package dev.otectus.sophisticatedtab.client;

import dev.otectus.sophisticatedtab.client.compat.legendarytabs.LegendaryTabsCompat;
import dev.otectus.sophisticatedtab.client.input.KeyBindings;
import dev.otectus.sophisticatedtab.client.input.TabInteractionHandler;
import dev.otectus.sophisticatedtab.client.prefs.PreferencesStorage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientBootstrap {
    private static final Logger LOG = LoggerFactory.getLogger("sophisticatedtab");

    private ClientBootstrap() {}

    public static void onClientSetup(FMLClientSetupEvent event) {
        ModList mods = ModList.get();
        boolean hasModTabs = mods.isLoaded("modtabs");
        boolean hasSophisticatedBackpacks = mods.isLoaded("sophisticatedbackpacks");

        if (!hasModTabs || !hasSophisticatedBackpacks) {
            LOG.warn(
                    "Skipping Mod Tabs registration (modtabs={}, sophisticatedbackpacks={}).",
                    hasModTabs,
                    hasSophisticatedBackpacks);
            return;
        }

        event.enqueueWork(() -> {
            PreferencesStorage.loadAll();
            LegendaryTabsCompat.register();
        });

        NeoForge.EVENT_BUS.register(ClientBootstrap.class);
        NeoForge.EVENT_BUS.register(TabInteractionHandler.class);
    }

    // Flush pending preference writes at most once per tick. Mutations mark the
    // model dirty; this drains the flag on the client thread so all I/O stays
    // off the render path. NeoForge replaced the phased TickEvent with separate
    // Pre/Post events; Post mirrors the old Phase.END.
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        PreferencesStorage.flushIfDirty();
        KeyBindings.pollSettingsKey();
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
