package ez.nebula.client.mixin.impl.mp;

import ez.nebula.client.mixin.duck.IPlayerControllerMP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author xgraza
 * @since 9/24/26
 */
@Mixin(value = PlayerControllerMP.class)
public abstract class PlayerControllerMPMixin implements IPlayerControllerMP
{
    @Shadow private int blockHitDelay;
    @Shadow private float curBlockDamageMP;
    @Shadow private WorldSettings.GameType currentGameType;

    @Shadow protected abstract boolean sameToolAndBlock(int par1, int par2, int par3);

    @Shadow private int currentPlayerItem;

    @Override
    public boolean nebula$sameToolAndBlock(int x, int y, int z)
    {
        return sameToolAndBlock(x, y, z);
    }

    @Override
    public WorldSettings.GameType nebula$getCurrentGameType()
    {
        return currentGameType;
    }

    @Override
    public float nebula$getCurBlockDamageMP()
    {
        return curBlockDamageMP;
    }

    @Override
    public void nebula$setBlockHitDelay(int delay)
    {
        blockHitDelay = delay;
    }

    @Override
    public void nebula$setCurrentPlayerItem(int i)
    {
        currentPlayerItem = i;
    }
}
