package dev.otectus.sophisticatedtab.client;

import dev.otectus.sophisticatedtab.client.compat.legendarytabs.LegendaryTabsCompat;
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

        event.enqueueWork(LegendaryTabsCompat::register);
    }
}
