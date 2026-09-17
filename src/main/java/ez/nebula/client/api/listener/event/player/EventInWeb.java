package ez.nebula.client.api.listener.event.player;

import ez.nebula.client.api.listener.Event;
import net.minecraft.entity.Entity;

public final class EventInWeb extends Event
{
    private final Entity entity;

    public EventInWeb(Entity entity)
    {
        this.entity = entity;
    }

    public Entity getEntity()
    {
        return entity;
    }
}
