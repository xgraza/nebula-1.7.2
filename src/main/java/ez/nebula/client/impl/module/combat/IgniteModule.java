package ez.nebula.client.impl.module.combat;

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
import ez.nebula.client.impl.module.player.FreecamModule;
import ez.nebula.client.mixin.duck.IEntity;
import ez.nebula.client.mixin.duck.IWorld;
import ez.nebula.client.util.math.AngleUtil;
import ez.nebula.client.util.minecraft.player.InventoryUtil;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import ez.nebula.client.util.minecraft.world.BlockUtil;
import net.minecraft.block.BlockFire;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.potion.Potion;
import net.minecraft.src.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.Comparator;

/**
 * @author xgraza
 * @since 7/31/26
 */
@ModuleManifest(name = "Ignite",
        description = "Automatically sets another player on fire with a flint and steel",
        category = ModuleCategory.COMBAT)
@RotationPriority(ModuleRotationPriorities.IGNITE)
public final class IgniteModule extends InteractionModule
{
    private final NumberSetting<Double> rangeSetting = numberBuilder("Range", 4.5)
            .setMin(1.0)
            .setMax(6.0)
            .setScale(0.5)
            .setDescription("The place range")
            .build();
    private final Setting<Boolean> rotateSetting = builder("Rotate", false)
            .setDescription("If to rotate to place")
            .build();
    private final Setting<Boolean> hellSetting = builder("Hell", false)
            .setDescription("If to still ignite them if they're on fire or have fire resistance")
            .build();
    private final Setting<Boolean> extinguishSetting = builder("Extinguish", false)
            .setDescription("If to immediately extinguish the fire after placing it")
            .setVisibility((value) -> !hellSetting.getValue())
            .build();

    private EntityPlayer player;
    private float[] angles;

    @Subscribe
    private final EventListener<EventUpdate> updateEventListener = event ->
    {
        player = getTarget();
        if (player == null)
        {
            return;
        }

        final int slot = InventoryUtil.getHotbarItem(Items.flint_and_steel);
        if (slot == InventoryUtil.INVALID_SLOT)
        {
            return;
        }

        final BlockPos origin = PlayerUtil.getOrigin(player);
        if (((IWorld)MC.theWorld).nebula$getBlock(origin) instanceof BlockFire
                || !BlockUtil.isReplaceable(origin)
                || BlockUtil.isReplaceable(origin.down()))
        {
            return;
        }

        if (rotateSetting.getValue() && !rotate(angles))
        {
            return;
        }

        place(origin.down(), EnumFacing.UP, slot);
        if (!hellSetting.getValue() && extinguishSetting.getValue())
        {
            click(origin, EnumFacing.UP);
        }
    };

    @Subscribe
    private final EventListener<EventRender3D> render3DEventListener = event ->
    {
        if (rotateSetting.getValue() && player != null)
        {
            angles = AngleUtil.anglesToBlock(PlayerUtil.getOrigin(player), EnumFacing.DOWN, event.getPartialTicks());
        }
    };

    @SuppressWarnings("unchecked")
    private EntityPlayer getTarget()
    {
        return (EntityPlayer) MC.theWorld.playerEntities.stream()
                .filter((player) -> !player.equals(MC.thePlayer)
                        && !((EntityPlayer)player).isDead
                        && ((EntityPlayer)player).getHealth() > 0.0f
                        && ((EntityPlayer)player).getEntityId() != FreecamModule.CAMERA_ENTITY_ID
                        && ((EntityPlayer)player).getDistanceToEntity(MC.thePlayer) <= rangeSetting.getValue()
                        && (NoFriendsModule.INSTANCE.isToggled() || !Nebula.FRIENDS.has(((EntityPlayer)player)))
                        && !((EntityPlayer)player).isPotionActive(Potion.fireResistance.id)
                        && (hellSetting.getValue() || ((IEntity) player).nebula$getFire() <= 0))
                .min(Comparator.comparingDouble((player) -> MC.thePlayer.getDistanceToEntity(((EntityPlayer)player))))
                .orElse(null);
    }
}
