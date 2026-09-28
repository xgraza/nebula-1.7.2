package ez.nebula.client.impl.module.combat;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventPostUpdate;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.network.EventPacket;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.InteractionModule;
import ez.nebula.client.api.manager.module.type.RotationPriority;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.ModuleRotationPriorities;
import ez.nebula.client.util.math.AngleUtil;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.network.play.server.S0EPacketSpawnObject;

import java.util.Set;
import java.util.TreeMap;

/**
 * @author xgraza
 * @since 6/21/26
 */
@ModuleManifest(name = "AntiFireball",
        description = "Automatically hits away fireballs",
        category = ModuleCategory.COMBAT)
@RotationPriority(ModuleRotationPriorities.ANTI_FIREBALL)
public final class AntiFireballModule extends InteractionModule
{
    private static final int FIREBALL_TYPE = 63;

    private final NumberSetting<Double> rangeSetting = numberBuilder("Range", 4.5)
            .setMin(1.0)
            .setMax(6.0)
            .setScale(0.1)
            .setDescription("How far away to reflect fireballs from")
            .build();
    private final Setting<Boolean> rotateSetting = builder("Rotate", false)
            .setDescription("If to rotate towards the entity")
            .build();
    private final Setting<Boolean> multiSetting = builder("Multi", false)
            .setDescription("If to hit multiple fireballs in one tick")
            .setVisibility((value) -> !rotateSetting.getValue())
            .build();

    private final Set<Integer> trackedFireballList = new ConcurrentSet<>();
    private EntityLargeFireball entity;
    private float[] angles;

    @Override
    public void onDisable()
    {
        super.onDisable();
        trackedFireballList.clear();
        entity = null;
        angles = null;
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        final TreeMap<Double, EntityLargeFireball> entityFireballTreeMap = new TreeMap<>();
        for (final Integer trackedEntityID : trackedFireballList)
        {
            final Entity entity = MC.theWorld.getEntityByID(trackedEntityID);
            if (!(entity instanceof EntityLargeFireball))
            {
                trackedFireballList.remove(trackedEntityID);
                continue;
            }

            final double distance = MC.thePlayer.getDistanceToEntity(entity);
            if (distance > rangeSetting.getValue())
            {
                continue;
            }
            entityFireballTreeMap.put(distance, (EntityLargeFireball) entity);
        }

        if (entityFireballTreeMap.isEmpty())
        {
            return;
        }
        entity = entityFireballTreeMap.firstEntry().getValue();

        if (rotateSetting.getValue() && !rotate(angles))
        {
            if (angles != null)
            {
                entity = null;
            }
            return;
        }

        for (final EntityLargeFireball fireballEntity : entityFireballTreeMap.values())
        {
            attack(fireballEntity, true);
            if (!multiSetting.getValue())
            {
                return;
            }
        }
    };

    @Subscribe
    private final EventListener<EventPostUpdate> postUpdateEventListener = event ->
    {
        if (entity != null && angles != null)
        {
            attack(entity, true);
            entity = null;
            angles = null;
        }
    };

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (entity != null && rotateSetting.getValue())
        {
            MC.mcProfiler.startSection("antiFireball_rotations");
            angles = AngleUtil.entityAngles(entity, entity.height / 2.0, event.getPartialTicks());
            MC.mcProfiler.endSection();
        }
    };

    @Subscribe
    private final EventListener<EventPacket.Inbound> inboundEventListener = event ->
    {
        if (event.getPacket() instanceof S0EPacketSpawnObject)
        {
            final S0EPacketSpawnObject packet = event.getPacket();
            if (packet.func_148993_l() != FIREBALL_TYPE)
            {
                return;
            }
            trackedFireballList.add(packet.func_149001_c());
        }
    };
}
