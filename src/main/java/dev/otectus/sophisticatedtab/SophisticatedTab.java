package dev.otectus.sophisticatedtab;

import dev.otectus.sophisticatedtab.client.ClientBootstrap;
import dev.otectus.sophisticatedtab.client.input.KeyBindings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

// Client-only mod: @Mod(dist = Dist.CLIENT) means this class is only constructed on
// the physical client, so no runtime Dist check is needed. NeoForge injects the mod
// event bus into the constructor.
@Mod(value = SophisticatedTab.MOD_ID, dist = Dist.CLIENT)
public final class SophisticatedTab {
    public static final String MOD_ID = "sophisticatedtab";

    public SophisticatedTab(IEventBus modBus) {
        modBus.addListener(ClientBootstrap::onClientSetup);
        modBus.register(KeyBindings.class);
    }
}
