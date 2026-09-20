package ez.nebula.client.impl.module.movement;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.manager.module.type.InteractionModule;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.util.math.Timer;
import net.minecraft.entity.Entity;

/**
 * @author xgraza
 * @since 6/5/26
 */
@ModuleManifest(name = "EntityControl",
        description = "Attempts to force control a riding entity",
        category = ModuleCategory.MOVEMENT)
public final class EntityControlModule extends InteractionModule
{
    @ModuleInstance
    public static EntityControlModule INSTANCE;

    public final Setting<Boolean> horsesSetting = builder("Horses", true)
            .setDescription("If to take control of a horse without a saddle")
            .build();
    public final Setting<Boolean> pigSetting = builder("Pigs", true)
            .setDescription("If to control a pig with a saddle without a carrot on a stick")
            .build();
    private final Setting<Boolean> autoRemountSetting = builder("Auto Remount", true)
            .setDescription("If to automatically remount the last riding entity when kicked off")
            .build();

    private final Timer mountTimer = new Timer();
    private Entity lastRidingEntity;

    @Override
    public void onDisable()
    {
        super.onDisable();
        lastRidingEntity = null;
    }

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        if (autoRemountSetting.getValue())
        {
            if (lastRidingEntity != null && MC.thePlayer.ridingEntity == null && mountTimer.hasElapsed(500L, true))
            {
                interact(lastRidingEntity);
            }
        }
        if (MC.thePlayer.ridingEntity != null)
        {
            lastRidingEntity = MC.thePlayer.ridingEntity;
        }
    };
}
