package ez.nebula.client.api.player.pathfinding.node;

/**
 * @author xgraza
 * @since 9/18/26
 */
public interface NodeCosts
{
    float MINOR_INCONVENIENCE = 0.5f;
    float NOT_IDEAL_COST = 1.0f;
    float GENERAL_DANGER_COST = 2.0f;
    float DANGER_COST = 5.0f;
    float IMPOSSIBLE = 100.0f;
}
