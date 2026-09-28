package ez.nebula.client.util.render.world;

import ez.nebula.client.impl.module.render.EntityCullingModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL15;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * @author xgraza
 * @since 03/25/25
 * Credit to <a href="https://github.com/Sk1erLLC/Patcher/blob/4ce6e196e5ad1339f8a0ab96eb5680c2f6464583/src/main/java/club/sk1er/patcher/util/world/render/culling/EntityCulling.java">...</a>
 */
public final class EntityCulling
{
    private static final Map<UUID, Result> QUERY_RESULTS = new ConcurrentHashMap<>();
    private static final Minecraft MC = Minecraft.getMinecraft();

    public static boolean isInactive()
    {
        return !EntityCullingModule.INSTANCE.isToggled();
    }

    public static int getQuery()
    {
        return GL15.glGenQueries();
    }

    public static void reset()
    {
        for (final UUID uuid : QUERY_RESULTS.keySet())
        {
            final Result result = QUERY_RESULTS.get(uuid);
            if (result.id != 0)
            {
                glDeleteQueries(result.id);
                result.id = 0;
            }
            QUERY_RESULTS.remove(uuid);
        }
    }

    public static void checkCulling()
    {
        if (isInactive())
        {
            return;
        }
        for (final Result result : QUERY_RESULTS.values())
        {
            if (result.id == 0 || glGetQueryObjecti(result.id, GL_QUERY_RESULT_AVAILABLE) == 0)
            {
                continue;
            }

            result.value = glGetQueryObjecti(result.id, GL_QUERY_RESULT) > 0;
        }
    }

    public static void queryEntity(final Entity entity)
    {
        if (isInactive())
        {
            return;
        }

        if (entity.isDead || MC.theWorld.getEntityByID(entity.getEntityId()) == null)
        {
            final Result result = QUERY_RESULTS.remove(entity.getUniqueID());
            if (result != null && glIsQuery(result.id))
            {
                glDeleteQueries(result.id);
            }
            return;
        }

        final Result result = QUERY_RESULTS.computeIfAbsent(
                entity.getUniqueID(), (x) -> new Result());

        if (System.currentTimeMillis() - result.reQueryAt < 50L)
        {
            return;
        }

        if (result.id == 0)
        {
            result.id = getQuery();
        }
        glBeginQuery(GL_SAMPLES_PASSED, result.id);
        final AxisAlignedBB renderBox = entity.boundingBox.copy()
                .expand(0.2, 0.2, 0.2)
                .offset(-RenderManager.renderPosX, -RenderManager.renderPosY, -RenderManager.renderPosZ);
        drawOutlinedBoundingBox(renderBox);
        glEndQuery(GL_SAMPLES_PASSED);
        result.reQueryAt = System.currentTimeMillis() + 50L;
    }

    public static boolean shouldRenderEntity(final Entity entity)
    {
        if (isInactive())
        {
            return true;
        }
        if (!QUERY_RESULTS.containsKey(entity.getUniqueID()))
        {
            return true;
        }
        return QUERY_RESULTS.get(entity.getUniqueID()).value;
    }

    public static void drawOutlinedBoundingBox(AxisAlignedBB bb)
    {
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_CULL_FACE);
        glDepthMask(false);
        glColorMask(false, false, false, false);
        Tessellator var2 = Tessellator.instance;
        var2.startDrawing(GL_QUAD_STRIP);
        var2.addVertex(bb.maxX, bb.maxY, bb.maxZ);
        var2.addVertex(bb.maxX, bb.maxY, bb.minZ);
        var2.addVertex(bb.minX, bb.maxY, bb.maxZ);
        var2.addVertex(bb.minX, bb.maxY, bb.minZ);
        var2.addVertex(bb.minX, bb.minY, bb.maxZ);
        var2.addVertex(bb.minX, bb.minY, bb.minZ);
        var2.addVertex(bb.minX, bb.maxY, bb.minZ);
        var2.addVertex(bb.minX, bb.minY, bb.minZ);
        var2.addVertex(bb.maxX, bb.maxY, bb.minZ);
        var2.addVertex(bb.maxX, bb.minY, bb.minZ);
        var2.addVertex(bb.maxX, bb.maxY, bb.maxZ);
        var2.addVertex(bb.maxX, bb.minY, bb.maxZ);
        var2.addVertex(bb.minX, bb.maxY, bb.maxZ);
        var2.addVertex(bb.minX, bb.minY, bb.maxZ);
        var2.addVertex(bb.minX, bb.minY, bb.maxZ);
        var2.addVertex(bb.maxX, bb.minY, bb.maxZ);
        var2.addVertex(bb.minX, bb.minY, bb.minZ);
        var2.addVertex(bb.maxX, bb.minY, bb.minZ);
        var2.draw();
        glDepthMask(true);
        glColorMask(true, true, true, true);
        glEnable(GL_ALPHA_TEST);
    }

    private static final class Result
    {
        private long reQueryAt;
        private int id = 0;
        private boolean value;
    }
}
