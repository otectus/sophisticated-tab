package dev.otectus.sophisticatedtab;

import dev.otectus.sophisticatedtab.client.ClientBootstrap;
import dev.otectus.sophisticatedtab.client.input.KeyBindings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(SophisticatedTab.MOD_ID)
public final class SophisticatedTab {
    public static final String MOD_ID = "sophisticatedtab";

    public SophisticatedTab() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(ClientBootstrap::onClientSetup);
            modBus.register(KeyBindings.class);
        }
    }
}
