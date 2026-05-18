package com.github.wclark.simplespells;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class GraveyardStructures {
    private static final ResourceLocation GRAVEYARD_TEMPLATE = ResourceLocation.fromNamespaceAndPath(SimpleSpells.MODID, "graveyard");
    private static final int RANDOM_PLAINS_CHANCE = 64;
    private static final int EMERGE_TICKS = 42;
    private static final int[][] GRAVES = {
            {4, 5},
            {8, 5},
            {12, 5},
            {4, 11},
            {8, 11},
            {12, 11},
    };
    private final ArrayDeque<PendingChunk> pendingChunks = new ArrayDeque<>();
    private final Set<Long> pendingChunkKeys = new HashSet<>();
    private final List<EmergingUndead> emergingUndead = new ArrayList<>();

    public static void register() {
        NeoForge.EVENT_BUS.register(new GraveyardStructures());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        GraveyardData data = GraveyardData.get(level);
        if (!data.isSpawnGraveyardPlaced()) {
            BlockPos spawn = level.getSharedSpawnPos();
            if (placeGraveyard(level, spawn, true)) {
                data.markSpawnGraveyardPlaced();
                SimpleSpells.LOGGER.info("Placed test graveyard near world spawn at {}", spawn);
            }
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }

        ChunkPos chunkPos = event.getChunk().getPos();
        if (pendingChunkKeys.add(chunkPos.toLong())) {
            pendingChunks.addLast(new PendingChunk(level, chunkPos));
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        processPendingChunks();
        tickEmergingUndead();
    }

    private void processPendingChunks() {
        for (int i = 0; i < 2 && !pendingChunks.isEmpty(); i++) {
            PendingChunk pending = pendingChunks.removeFirst();
            pendingChunkKeys.remove(pending.chunkPos().toLong());
            tryPlaceRandomGraveyard(pending.level(), pending.chunkPos());
        }
    }

    private void tryPlaceRandomGraveyard(ServerLevel level, ChunkPos chunkPos) {
        if (!shouldTryGraveyard(level, chunkPos)) {
            return;
        }

        GraveyardData data = GraveyardData.get(level);
        if (data.hasPlacedChunk(chunkPos)) {
            return;
        }

        BlockPos center = new BlockPos(
                chunkPos.getMiddleBlockX(),
                level.getHeight(Heightmap.Types.WORLD_SURFACE, chunkPos.getMiddleBlockX(), chunkPos.getMiddleBlockZ()),
                chunkPos.getMiddleBlockZ());
        if (placeGraveyard(level, center, false)) {
            data.markPlacedChunk(chunkPos);
            SimpleSpells.LOGGER.info("Generated plains graveyard in chunk {}", chunkPos);
        }
    }

    private static boolean shouldTryGraveyard(ServerLevel level, ChunkPos chunkPos) {
        long mixedSeed = level.getSeed() ^ (chunkPos.toLong() * 341873128712L) ^ 132897987541L;
        return Math.floorMod(mixedSeed, RANDOM_PLAINS_CHANCE) == 0;
    }

    private boolean placeGraveyard(ServerLevel level, BlockPos center, boolean force) {
        Optional<StructureTemplate> template = level.getStructureManager().get(GRAVEYARD_TEMPLATE);
        if (template.isEmpty()) {
            SimpleSpells.LOGGER.warn("Could not load structure template {}", GRAVEYARD_TEMPLATE);
            return false;
        }

        Vec3i size = template.get().getSize();
        BlockPos origin = findOrigin(level, center, size);
        if (!force && !isPlainAndDry(level, origin, size)) {
            return false;
        }

        prepareGround(level, origin, size);
        boolean placed = template.get().placeInWorld(
                level,
                origin,
                origin,
                new StructurePlaceSettings(),
                level.getRandom(),
                2);
        if (placed) {
            triggerEmergingUndead(level, origin);
        }
        return placed;
    }

    private static BlockPos findOrigin(ServerLevel level, BlockPos center, Vec3i size) {
        int originX = center.getX() - size.getX() / 2;
        int originZ = center.getZ() - size.getZ() / 2;
        int originY = level.getMinBuildHeight();

        for (int x = originX; x < originX + size.getX(); x++) {
            for (int z = originZ; z < originZ + size.getZ(); z++) {
                originY = Math.max(originY, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
            }
        }

        return new BlockPos(originX, originY, originZ);
    }

    private static boolean isPlainAndDry(ServerLevel level, BlockPos origin, Vec3i size) {
        int[][] samples = {
                {size.getX() / 2, size.getZ() / 2},
                {1, 1},
                {size.getX() - 2, 1},
                {1, size.getZ() - 2},
                {size.getX() - 2, size.getZ() - 2},
        };

        for (int[] sample : samples) {
            int x = origin.getX() + sample[0];
            int z = origin.getZ() + sample[1];
            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos surface = new BlockPos(x, surfaceY - 1, z);
            if (!level.getBiome(surface).is(Biomes.PLAINS) || !level.getBlockState(surface).getFluidState().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static void prepareGround(ServerLevel level, BlockPos origin, Vec3i size) {
        int clearTop = origin.getY() + size.getY() + 2;
        for (int x = origin.getX(); x < origin.getX() + size.getX(); x++) {
            for (int z = origin.getZ(); z < origin.getZ() + size.getZ(); z++) {
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = surfaceY; y < origin.getY(); y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
                }
                for (int y = origin.getY(); y <= clearTop; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private void triggerEmergingUndead(ServerLevel level, BlockPos origin) {
        for (int[] grave : GRAVES) {
            EntityType<? extends Mob> type = level.getRandom().nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON;
            Mob mob = type.create(level);
            if (mob != null) {
                double x = origin.getX() + grave[0] + 0.5D;
                double endY = origin.getY() + 1.0D;
                double startY = endY - 1.35D;
                double z = origin.getZ() + grave[1] + 0.5D;
                float rotation = level.getRandom().nextFloat() * 360.0F;
                BlockPos spawnPos = BlockPos.containing(x, endY, z);

                mob.moveTo(x, startY, z, rotation, 0.0F);
                mob.setNoAi(true);
                mob.setNoGravity(true);
                mob.setSilent(true);
                mob.setPersistenceRequired();
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null);
                level.addFreshEntity(mob);
                emergingUndead.add(new EmergingUndead(level, mob.getUUID(), startY, endY));
            }
        }
    }

    private void tickEmergingUndead() {
        Iterator<EmergingUndead> iterator = emergingUndead.iterator();
        while (iterator.hasNext()) {
            EmergingUndead emerging = iterator.next();
            if (emerging.tick()) {
                iterator.remove();
            }
        }
    }

    private record PendingChunk(ServerLevel level, ChunkPos chunkPos) {
    }

    private static final class EmergingUndead {
        private final ServerLevel level;
        private final UUID mobId;
        private final double startY;
        private final double endY;
        private int age;

        private EmergingUndead(ServerLevel level, UUID mobId, double startY, double endY) {
            this.level = level;
            this.mobId = mobId;
            this.startY = startY;
            this.endY = endY;
        }

        private boolean tick() {
            Entity entity = level.getEntity(mobId);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                return true;
            }

            age++;
            double progress = Math.min(1.0D, age / (double) EMERGE_TICKS);
            double y = startY + (endY - startY) * progress;
            mob.setDeltaMovement(0.0D, 0.0D, 0.0D);
            mob.setPos(mob.getX(), y, mob.getZ());

            if (age >= EMERGE_TICKS) {
                mob.setNoAi(false);
                mob.setNoGravity(false);
                mob.setSilent(false);
                return true;
            }
            return false;
        }
    }

    private static final class GraveyardData extends SavedData {
        private static final String NAME = SimpleSpells.MODID + "_graveyards";
        private boolean spawnGraveyardPlaced;
        private final Set<Long> placedChunks = new HashSet<>();

        private GraveyardData() {
        }

        private GraveyardData(CompoundTag tag) {
            spawnGraveyardPlaced = tag.getBoolean("spawnGraveyardPlaced");
            for (long chunk : tag.getLongArray("placedChunks")) {
                placedChunks.add(chunk);
            }
        }

        private static SavedData.Factory<GraveyardData> factory() {
            return new SavedData.Factory<>(GraveyardData::new, (tag, registries) -> new GraveyardData(tag));
        }

        private static GraveyardData get(ServerLevel level) {
            DimensionDataStorage storage = level.getDataStorage();
            return storage.computeIfAbsent(factory(), NAME);
        }

        private boolean isSpawnGraveyardPlaced() {
            return spawnGraveyardPlaced;
        }

        private void markSpawnGraveyardPlaced() {
            spawnGraveyardPlaced = true;
            setDirty();
        }

        private boolean hasPlacedChunk(ChunkPos chunkPos) {
            return placedChunks.contains(chunkPos.toLong());
        }

        private void markPlacedChunk(ChunkPos chunkPos) {
            placedChunks.add(chunkPos.toLong());
            setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            tag.putBoolean("spawnGraveyardPlaced", spawnGraveyardPlaced);
            tag.putLongArray("placedChunks", new ArrayList<>(placedChunks));
            return tag;
        }
    }
}
