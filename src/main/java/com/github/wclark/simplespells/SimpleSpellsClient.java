package com.github.wclark.simplespells;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = SimpleSpells.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = SimpleSpells.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class SimpleSpellsClient {
    public SimpleSpellsClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        SimpleSpells.LOGGER.info("HELLO FROM CLIENT SETUP");
        SimpleSpells.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SimpleSpells.SPELL_PROJECTILE.get(), NoopRenderer::new);
    }

    @EventBusSubscriber(modid = SimpleSpells.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    static class GameEvents {
        @SubscribeEvent
        static void onStaffCastInput(InputEvent.InteractionKeyMappingTriggered event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (!event.isAttack() || event.getHand() != InteractionHand.MAIN_HAND || minecraft.player == null || minecraft.screen != null) {
                return;
            }

            ItemStack stack = minecraft.player.getMainHandItem();
            if (!stack.is(SimpleSpells.MAGE_STAFF.get()) || minecraft.player.getCooldowns().isOnCooldown(SimpleSpells.MAGE_STAFF.get())) {
                return;
            }

            PacketDistributor.sendToServer(new CastStaffPayload());
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }
}
