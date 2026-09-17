package ez.nebula.client.api.listener.event.player;

import ez.nebula.client.api.listener.Event;

public class EventSprint extends Event
{
    private boolean sprinting;

    public void setSprinting(boolean sprinting)
    {
        this.sprinting = sprinting;
    }

    public boolean isSprinting()
    {
        return sprinting;
    }
}
