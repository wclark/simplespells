package com.github.wclark.simplespells;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = SimpleSpells.MODID, dist = Dist.CLIENT)
public class SimpleSpellsClient {
    public SimpleSpellsClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(SimpleSpellsClient::onClientSetup);
        modEventBus.addListener(SimpleSpellsClient::registerRenderers);
        NeoForge.EVENT_BUS.addListener(SimpleSpellsClient::onStaffCastInput);

        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        SimpleSpells.LOGGER.info("HELLO FROM CLIENT SETUP");
        SimpleSpells.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        event.enqueueWork(() -> ItemProperties.register(
                SimpleSpells.MAGE_STAFF.get(),
                ResourceLocation.fromNamespaceAndPath(SimpleSpells.MODID, "spell_mode"),
                (stack, level, entity, seed) -> MageStaffItem.getMode(stack).ordinal()));
    }

    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SimpleSpells.SPELL_PROJECTILE.get(), NoopRenderer::new);
        event.registerEntityRenderer(SimpleSpells.ARCHMAGE_SPELL_PROJECTILE.get(), NoopRenderer::new);
    }

    static void onStaffCastInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.isAttack() || event.getHand() != InteractionHand.MAIN_HAND || minecraft.player == null || minecraft.screen != null) {
            return;
        }

        ItemStack stack = minecraft.player.getMainHandItem();
        if (!StaffCasting.canRequestCast(stack, minecraft.player)) {
            return;
        }

        PacketDistributor.sendToServer(new CastStaffPayload());
        event.setSwingHand(false);
        event.setCanceled(true);
    }
}
