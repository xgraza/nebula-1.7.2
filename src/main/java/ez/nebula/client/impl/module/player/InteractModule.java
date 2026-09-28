package ez.nebula.client.impl.module.player;

import ez.nebula.client.api.listener.EventListener;
import ez.nebula.client.api.listener.Subscribe;
import ez.nebula.client.api.listener.event.game.EventUpdate;
import ez.nebula.client.api.listener.event.world.EventLiquidCollide;
import ez.nebula.client.api.manager.module.Module;
import ez.nebula.client.api.manager.module.trait.ModuleCategory;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.api.manager.module.trait.ModuleManifest;
import ez.nebula.client.api.setting.NumberSetting;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.mixin.duck.IMinecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * @author xgraza
 * @since 06/16/25
 */
@ModuleManifest(name = "Interact",
        description = "Changes how you interact with blocks or entities",
        category = ModuleCategory.PLAYER)
public final class InteractModule extends Module
{
    @ModuleInstance
    public static InteractModule INSTANCE;

    public final NumberSetting<Integer> placeDelaySetting = numberBuilder("Place Delay", 0)
            .setMin(0)
            .setMax(4)
            .setScale(1)
            .setDescription("How many ticks it should take before allowing you to place another block")
            .build();

    public final NumberSetting<Double> attackReachSetting = numberBuilder("Attach Reach", 3.0)
            .setMin(1.0)
            .setMax(6.0)
            .setScale(0.1)
            .setDescription("The distance in blocks you are able to interact with entities")
            .build();
    public final NumberSetting<Double> placeReachSetting = numberBuilder("Place Reach", 3.0)
            .setMin(1.0)
            .setMax(6.0)
            .setScale(0.1)
            .setDescription("The distance in blocks you are able to interact with blocks")
            .build();

    public final Setting<Boolean> waterPlaceSetting = builder("Water Place", false)
            .setDescription("If to allow placing in water")
            .build();

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
            ((IMinecraft)MC).nebula$setRightClickDelayTimer(placeDelaySetting.getValue());

    @Subscribe
    private final EventListener<EventLiquidCollide> liquidCollideEventListener = event ->
    {
        final ItemStack itemStack = MC.thePlayer.getHeldItem();
        if (waterPlaceSetting.getValue() && itemStack != null && itemStack.getItem() instanceof ItemBlock)
        {
            event.setResult(true);
            event.cancel();
        }
    };
}
