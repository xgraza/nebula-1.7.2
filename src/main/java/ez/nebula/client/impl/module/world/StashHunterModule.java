package ez.nebula.client.impl.module.world;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.network.EventDisconnect;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.listener.event.world.EventRemoveTileEntity;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.ColorSetting;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.mixin.duck.IAxisAlignedBB;
import ez.nebula.client.mixin.duck.IChunkProviderClient;
import ez.nebula.client.util.render.world.QuadMask;
import ez.nebula.client.util.render.world.Render3D;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.src.BlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.chunk.Chunk;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author xgraza
 * @since 05/21/26
 */
@ModuleManifest(name = "StashHunter",
        description = "Attempts to aid in finding large chest/minecart stashes when actively hunting",
        category = ModuleCategory.WORLD)
public final class StashHunterModule extends Module
{
    private final Setting<Boolean> stackedMinecartsSetting = builder("Stacked Minecarts", true)
            .setDescription("If to search for stacked minecarts")
            .build();
    private final ColorSetting minecartColorSetting = colorBuilder("Minecart Color", new Color(195, 122, 50, 120))
            .setDescription("The color to render stacked minecarts with")
            .setVisibility((value) -> stackedMinecartsSetting.getValue())
            .build();

    private final Setting<Boolean> chestsSetting = builder("Chests", true)
            .setDescription("If to search for bulk chests")
            .build();
    private final NumberSetting<Integer> minChestsPerChunkSetting = numberBuilder("Chests per chunk", 3)
            .setMin(1)
            .setMax(10)
            .setScale(1)
            .setDescription("How many chests must be in a chunk before flagging it as a stash")
            .setVisibility((value) -> chestsSetting.getValue())
            .build();
    private final ColorSetting chestsColorSetting = colorBuilder("Chests Color", new Color(3, 195, 244, 120))
            .setDescription("The color to render flagged chests with")
            .setVisibility((value) -> chestsSetting.getValue())
            .build();

    private final Set<Object> observedObjects = new ConcurrentSet<>();
    private final Set<Vec3> stackedMinecartPositionSet = new ConcurrentSet<>();
    private final Set<BlockPos> chestsRenderList = new ConcurrentSet<>();

    @Override
    public void onDisable()
    {
        super.onDisable();
        observedObjects.clear();
        stackedMinecartPositionSet.clear();
        chestsRenderList.clear();
    }

    @Subscribe
    private final EventListener<EventDisconnect> disconnectEventListener = event ->
    {
        observedObjects.clear();
        stackedMinecartPositionSet.clear();
        chestsRenderList.clear();
    };

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        MC.mcProfiler.startSection("stashHunter");
        if (stackedMinecartsSetting.getValue())
        {
            final int color = minecartColorSetting.getValue().getRGB();
            for (final Vec3 pos : stackedMinecartPositionSet)
            {
                final AxisAlignedBB bb = IAxisAlignedBB.create(pos, 1);
                Render3D.filledAABB(bb, QuadMask.ALL_FACES, color);
                Render3D.outlinedAABB(bb, 1.5f, QuadMask.ALL_FACES, color);
            }
        }
        if (chestsSetting.getValue())
        {
            final int color = chestsColorSetting.getValue().getRGB();
            for (final BlockPos pos : chestsRenderList)
            {
                final AxisAlignedBB bb = IAxisAlignedBB.create(pos);
                Render3D.filledAABB(bb, QuadMask.ALL_FACES, color);
                Render3D.outlinedAABB(bb, 1.5f, QuadMask.ALL_FACES, color);
            }
        }
        MC.mcProfiler.endSection();
    };

    @Subscribe
    private final EventListener<EventRemoveTileEntity> removeTileEntityEventListener = event ->
    {
        observedObjects.remove(event.getTileEntity());
    };

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        final List<Entity> entities = MC.theWorld.loadedEntityList;
        final List<TileEntity> tileEntities = MC.theWorld.loadedTileEntityList;
        if (stackedMinecartsSetting.getValue())
        {
            findStackedMinecarts(entities);
        } else
        {
            stackedMinecartPositionSet.clear();
        }
        if (chestsSetting.getValue())
        {
            findALotOfChests();
        } else
        {
            chestsRenderList.clear();
        }
    };

    private void findStackedMinecarts(final List<Entity> entities)
    {
        for (final Entity entity : entities)
        {
            if (!(entity instanceof EntityMinecartChest) || !observedObjects.add(entity))
            {
                continue;
            }
            final AxisAlignedBB bb = entity.boundingBox.copy();
            final List<Entity> collidingEntities = MC.theWorld.getEntitiesWithinAABBExcludingEntity(entity, bb,
                    e -> e instanceof EntityMinecartChest);
            if (collidingEntities.isEmpty())
            {
                continue;
            }
            final Vec3 positon = Vec3.createVectorHelper(entity.posX - 0.5, entity.posY - 0.5, entity.posZ - 0.5);
            stackedMinecartPositionSet.add(positon);
            observedObjects.addAll(collidingEntities);
            notifyInfo(String.format("Found %s stacked minecarts at XYZ: %s, %s, %s",
                    collidingEntities.size() + 1,
                    positon.xCoord, positon.yCoord, positon.zCoord), 7500L);
        }
    }

    private void findALotOfChests()
    {
        for (final Chunk chunk : ((IChunkProviderClient)MC.theWorld.getChunkProvider()).nebula$getChunkListing())
        {
            final List<BlockPos> chestTileEntityList = new ArrayList<>();
            for (final Object o : chunk.chunkTileEntityMap.values())
            {
                if (!(o instanceof TileEntity))
                {
                    continue;
                }
                final TileEntity tileEntity = (TileEntity)o;
                if (!(tileEntity instanceof TileEntityChest) || !observedObjects.add(tileEntity))
                {
                    continue;
                }
                chestTileEntityList.add(new BlockPos(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord));
            }

            if (chestTileEntityList.size() >= minChestsPerChunkSetting.getValue())
            {
                notifyInfo(String.format("Found %s chests inside of a chunk", chestTileEntityList.size()), 7500L);
                chestsRenderList.addAll(chestTileEntityList);
            }
        }
    }
}
