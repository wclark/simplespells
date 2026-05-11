package com.github.wclark.simplespells;

import java.util.EnumMap;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(SimpleSpells.MODID)
public class SimpleSpells {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "simplespells";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "simplespells" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "simplespells" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "simplespells" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredItem<Item> RAW_EMITITE = ITEMS.registerSimpleItem("raw_emitite");
    public static final DeferredItem<Item> EMITITE = ITEMS.registerSimpleItem("emitite");
    public static final DeferredItem<Item> MAGIC_DUST = ITEMS.registerSimpleItem("magic_dust");
    public static final DeferredItem<MageStaffItem> MAGE_STAFF = ITEMS.register("mage_staff", () -> new MageStaffItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<EntityType<?>, EntityType<SpellProjectile>> SPELL_PROJECTILE = ENTITY_TYPES.register(
            "spell_projectile",
            () -> EntityType.Builder.<SpellProjectile>of(SpellProjectile::new, MobCategory.MISC)
                    .sized(0.28F, 0.28F)
                    .clientTrackingRange(4)
                    .updateInterval(1)
                    .build("spell_projectile"));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MAGE_ARMOR_MATERIAL = ARMOR_MATERIALS.register("mage", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                defense.put(ArmorItem.Type.BOOTS, 2);
                defense.put(ArmorItem.Type.LEGGINGS, 5);
                defense.put(ArmorItem.Type.CHESTPLATE, 6);
                defense.put(ArmorItem.Type.HELMET, 2);
                defense.put(ArmorItem.Type.BODY, 5);
            }),
            9,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(EMITITE.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MODID, "mage"))),
            0.0F,
            0.0F));

    public static final DeferredItem<ArmorItem> MAGE_HAT = registerMageArmor("mage_hat", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> MAGE_ROBES = registerMageArmor("mage_robes", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> MAGE_PANTS = registerMageArmor("mage_pants", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> MAGE_BOOTS = registerMageArmor("mage_boots", ArmorItem.Type.BOOTS);

    public static final DeferredBlock<Block> EMITITE_ORE = BLOCKS.registerSimpleBlock(
            "emitite_ore",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F));
    public static final DeferredItem<BlockItem> EMITITE_ORE_ITEM = ITEMS.registerSimpleBlockItem("emitite_ore", EMITITE_ORE);

    public static final DeferredBlock<Block> DEEPSLATE_EMITITE_ORE = BLOCKS.registerSimpleBlock(
            "deepslate_emitite_ore",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE));
    public static final DeferredItem<BlockItem> DEEPSLATE_EMITITE_ORE_ITEM = ITEMS.registerSimpleBlockItem("deepslate_emitite_ore", DEEPSLATE_EMITITE_ORE);

    public static final DeferredBlock<FlowerBlock> MANA_BLOOM = BLOCKS.register(
            "mana_bloom",
            () -> new FlowerBlock(
                    MobEffects.NIGHT_VISION,
                    5.0F,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)
                            .noCollission()
                            .noOcclusion()
                            .offsetType(BlockBehaviour.OffsetType.XZ)));
    public static final DeferredItem<BlockItem> MANA_BLOOM_ITEM = ITEMS.registerSimpleBlockItem("mana_bloom", MANA_BLOOM);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SIMPLE_SPELLS_TAB = CREATIVE_MODE_TABS.register("simple_spells_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.simplespells")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> RAW_EMITITE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EMITITE_ORE_ITEM.get());
                output.accept(DEEPSLATE_EMITITE_ORE_ITEM.get());
                output.accept(MANA_BLOOM_ITEM.get());
                output.accept(RAW_EMITITE.get());
                output.accept(EMITITE.get());
                output.accept(MAGIC_DUST.get());
                output.accept(MAGE_STAFF.get());
                output.accept(MAGE_HAT.get());
                output.accept(MAGE_ROBES.get());
                output.accept(MAGE_PANTS.get());
                output.accept(MAGE_BOOTS.get());
            }).build());

    private static DeferredItem<ArmorItem> registerMageArmor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(MAGE_ARMOR_MATERIAL, type, new Item.Properties().durability(type.getDurability(15))));
    }

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public SimpleSpells(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        ARMOR_MATERIALS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want this class to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(SimpleSpellsNetwork::register);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EMITITE_ORE_ITEM);
            event.accept(DEEPSLATE_EMITITE_ORE_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(MANA_BLOOM_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(RAW_EMITITE);
            event.accept(EMITITE);
            event.accept(MAGIC_DUST);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MAGE_STAFF);
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(MAGE_HAT);
            event.accept(MAGE_ROBES);
            event.accept(MAGE_PANTS);
            event.accept(MAGE_BOOTS);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
