package ez.nebula.client.mixin.duck;

import net.minecraft.world.WorldSettings;

/**
 * @author xgraza
 * @since 9/24/26
 */
public interface IPlayerControllerMP
{
    boolean nebula$sameToolAndBlock(int x, int y, int z);

    WorldSettings.GameType nebula$getCurrentGameType();

    float nebula$getCurBlockDamageMP();

    void nebula$setBlockHitDelay(int delay);

    void nebula$setCurrentPlayerItem(int i);
}
