package com.ura.dest;

import com.ura.dest.event.VeinMineHandler;
import com.ura.dest.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Dest.MOD_ID)
public class Dest {
    public static final String MOD_ID = "dest";

    public Dest() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);

        Config.register();

        MinecraftForge.EVENT_BUS.register(new VeinMineHandler());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::registerPackets);
    }
}