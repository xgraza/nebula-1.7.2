package ez.nebula.client.impl.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import ez.nebula.client.Nebula;
import ez.nebula.client.api.manager.command.Command;
import ez.nebula.client.api.manager.command.trait.CommandManifest;
import ez.nebula.client.api.manager.command.trait.CommandSource;
import ez.nebula.client.util.minecraft.player.ChatUtil;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.world.storage.MapData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author xgraza
 * @since 6/24/26
 */
@CommandManifest(aliases = {"downloadmap", "dlmap"},
        description = "Downloads a map texture")
public final class DownloadMapCommand extends Command
{
    private static final int MAP_IMAGE_DIMENSIONS = 128;
    private static final File MAP_DOWNLOAD_FOLDER = new File(Nebula.NEBULA_ROOT, "saved_maps");

    static
    {
        if (!MAP_DOWNLOAD_FOLDER.exists() || !MAP_DOWNLOAD_FOLDER.isDirectory())
        {
            if (!MAP_DOWNLOAD_FOLDER.mkdir())
            {
                throw new RuntimeException("Failed to create map directory");
            }
        }
    }

    @Override
    public void createBuilder(LiteralArgumentBuilder<CommandSource> literal)
    {
//        literal.then(argument("id", IntegerArgumentType.integer())
//                .executes((ctx) ->
//                {
//                    final int mapId = IntegerArgumentType.getInteger(ctx, "id");
//                    MapData var4 = (MapData) MC.theWorld.loadItemData(MapData.class, "map_" + mapId);
//                    if (var4 == null)
//                    {
//                        return ctx.getSource().respond("Could not load map with ID \"%s\"", mapId);
//                    }
//                    final MapItemRenderer.Instance i = MC.entityRenderer.getMapItemRenderer().func_148248_b(var4);
//                    if (i.field_148243_c == null)
//                    {
//                        return ctx.getSource().respond("Texture data for map #%s is missing", mapId);
//                    }
//                    Nebula.EXECUTOR.execute(() ->
//                    {
//                        final File file = new File(MAP_DOWNLOAD_FOLDER,
//                                Nebula.SERVER.ip() + "_map_" + mapId + ".png");
//                        final int[] texData = i.field_148243_c.getTextureData();
//                        final BufferedImage image = new BufferedImage(
//                                MAP_IMAGE_DIMENSIONS, MAP_IMAGE_DIMENSIONS, BufferedImage.TYPE_INT_RGB);
//                        image.setRGB(0, 0,
//                                MAP_IMAGE_DIMENSIONS, MAP_IMAGE_DIMENSIONS,
//                                texData, 0, MAP_IMAGE_DIMENSIONS);
//                        try
//                        {
//                            ImageIO.write(image, "png", file);
//                            ChatUtil.sendNebula("Downloaded map #%s to %s", mapId, file.getName());
//                        } catch (IOException e)
//                        {
//                            ChatUtil.sendNebula("Failed to download image data!");
//                            throw new RuntimeException(e);
//                        }
//                    });
//                    return ctx.getSource().respond("Starting to download map data");
//                }))
//                .executes((ctx) ->
//                {
//                    final Map<String, MapData> mapDataMap = MC.theWorld.mapStorage.loadedDataMap;
//                    if (mapDataMap.isEmpty())
//                    {
//                        return ctx.getSource().respond("No cached maps available for this world");
//                    }
//                    final List<String> mapIDs = new ArrayList<>();
//                    for (final String str : mapDataMap.keySet())
//                    {
//                        if (str.startsWith("map_"))
//                        {
//                            mapIDs.add(str.substring(4));
//                        }
//                    }
//                    if (mapIDs.isEmpty())
//                    {
//                        return ctx.getSource().respond("No cached maps available for this world");
//                    }
//                    return ctx.getSource().respond("Available map IDs (%s): %s",
//                            mapIDs.size(), String.join(", ", mapIDs));
//                });
    }
}
