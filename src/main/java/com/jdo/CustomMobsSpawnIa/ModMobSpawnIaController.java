package com.jdo.CustomMobsSpawnIa;

import com.jdo.CustomMobsSpawnIa.command.SpawnMobCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("modmobspawniacontroller")
public class ModMobSpawnIaController {

    // Déclaration correcte du logger
    private static final Logger LOGGER = LoggerFactory.getLogger("ModMobSpawnIa");

    public ModMobSpawnIaController() {
        // Enregistrement des événements Forge
        MinecraftForge.EVENT_BUS.register(this);

        // Log au moment du chargement du mod
        LOGGER.info("ModMobSpawnIaController chargé avec succès !");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        SpawnMobCommand.register(event.getDispatcher());
        LOGGER.info("Commande /spawnmob enregistrée !");
    }
}