package ez.nebula.client.impl.module.world;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.render.EventRender3D;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.InteractionModule;
import ez.nebula.client.api.manager.module.type.RotationPriority;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.impl.module.ModuleRotationPriorities;
import ez.nebula.client.util.math.AngleUtil;
import ez.nebula.client.util.minecraft.player.EntityUtil;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemNameTag;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * @author xgraza
 * @since 6/25/26
 */
@ModuleManifest(name = "AutoTag",
        description = "Automatically nametags entities around you",
        category = ModuleCategory.WORLD)
@RotationPriority(ModuleRotationPriorities.AUTO_TAG)
public final class AutoTagModule extends InteractionModule
{
    private final NumberSetting<Double> rangeSetting = numberBuilder("Range", 4.5)
            .setMin(1.0)
            .setMax(6.0)
            .setScale(0.1)
            .setDescription("The range to tag entities at")
            .build();
    private final Setting<Boolean> rotateSetting = builder("Rotate", false)
            .setDescription("If to rotate towards the entity")
            .build();
    private final Setting<Boolean> overrideSetting = builder("Override", false)
            .setDescription("If to override an already nametagged entity")
            .build();
    private final Setting<Boolean> passiveSetting = builder("Passive", false)
            .setDescription("If to nametag passive entities")
            .build();
    private final Setting<Boolean> hostileSetting = builder("Hostile", false)
            .setDescription("If to nametag hostile entities")
            .build();

    private final Set<Integer> taggedEntityIdSet = new HashSet<>();
    private EntityLiving target;
    private float[] angles;

    @Override
    public void onDisable()
    {
        super.onDisable();
        taggedEntityIdSet.clear();
        angles = null;
        if (MC.thePlayer != null)
        {
            Nebula.INVENTORY.sync();
        }
    }

    @Subscribe
    @SuppressWarnings("unchecked")
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        target = (EntityLiving) MC.theWorld.loadedEntityList
                .stream()
                .filter((e) -> isValidEntity((Entity) e))
                .min(Comparator.comparingDouble((e) -> MC.thePlayer.getDistanceToEntity((Entity)e)))
                .orElse(null);
        if (target == null)
        {
            return;
        }

        final int nametagSlot = InventoryUtil.getHotbarSlot((stack) ->
                stack != null && stack.getItem() instanceof ItemNameTag && stack.hasDisplayName());
        if (nametagSlot == InventoryUtil.INVALID_SLOT)
        {
            return;
        }

        if (rotateSetting.getValue() && !rotate(angles))
        {
            return;
        }
        taggedEntityIdSet.add(target.getEntityId());
        interact(target, nametagSlot);
    };

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (rotateSetting.getValue() && target != null)
        {
            MC.mcProfiler.startSection("autoTag");
            angles = AngleUtil.entityAngles(target, target.height / 2.0, event.getPartialTicks());
            MC.mcProfiler.endSection();
        }
    };

    private boolean isValidEntity(final Entity entity)
    {
        if (!(entity instanceof EntityLiving)
                || entity.isDead
                || taggedEntityIdSet.contains(entity.getEntityId()))
        {
            return false;
        }
        final EntityLiving living = (EntityLiving) entity;
        if (living.getHealth() <= 0.0f)
        {
            return false;
        }
        if (!overrideSetting.getValue() && living.hasCustomNameTag())
        {
            return false;
        }
        if (!passiveSetting.getValue() && EntityUtil.isEntityPassive(entity))
        {
            return false;
        }
        if (!hostileSetting.getValue() && EntityUtil.isEntityHostile(entity))
        {
            return false;
        }
        return MC.thePlayer.getDistanceToEntity(entity) <= rangeSetting.getValue();
    }
}
