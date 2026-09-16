package ez.nebula.client.impl.hud2;

import ez.nebula.client.api.manager.hud2.trait.HUDManifest;
import ez.nebula.client.api.manager.hud2.type.TextHUDElement;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import java.util.TreeMap;

/**
 * @author xgraza
 * @since 9/16/26
 */
@HUDManifest(value = "Coordinates", description = "Renders your coordinates")
public final class CoordinatesHUDElement extends TextHUDElement
{
    private static final TreeMap<Integer, String[]> DIRECTION_MAP = new TreeMap<>();

    static
    {
        DIRECTION_MAP.put(0,   new String[] { "South", "S" });
        DIRECTION_MAP.put(45,  new String[] { "South West", "SW" });
        DIRECTION_MAP.put(90,  new String[] { "West", "W" });
        DIRECTION_MAP.put(135, new String[] { "North West", "NW" });
        DIRECTION_MAP.put(180, new String[] { "North", "N" });
        DIRECTION_MAP.put(225, new String[] { "North East", "NE" });
        DIRECTION_MAP.put(270, new String[] { "East", "E" });
        DIRECTION_MAP.put(315, new String[] { "South East", "SE" });
    }

    private final Setting<Boolean> netherCoordinatesSetting = builder("Nether Coordinates", true)
            .setDescription("If to show the nether/overworld equivalent coordinates")
            .build();
    private final Setting<Boolean> directionSetting = builder("Direction", true)
            .setDescription("If to show the direction you're facing")
            .build();
    private final Setting<Boolean> shortenedSetting = builder("Shortened", false)
            .setDescription("If to give a shortened version of that cardinal direction")
            .setVisibility((value) -> directionSetting.getValue())
            .build();
    private final Setting<Boolean> axisSetting = builder("Axis", true)
            .setDescription("If to show which axis you are travelling along")
            .build();
    private final Setting<Boolean> rotationSetting = builder("Rotations", false)
            .setDescription("If to show your yaw and pitch rotations")
            .build();

    @Override
    public String text()
    {
        final StringBuilder builder = new StringBuilder();

        if (directionSetting.getValue())
        {
            final Integer key = DIRECTION_MAP.floorKey(Math.abs((int) (MC.thePlayer.rotationYaw % 360.0f)));
            if (key != null)
            {
                final String[] dir = DIRECTION_MAP.get(key);

                builder.append(EnumChatFormatting.DARK_GRAY);
                builder.append(dir[shortenedSetting.getValue() ? 1 : 0]);
                builder.append(EnumChatFormatting.RESET);
            }
            builder.append(" ");
        }

        if (axisSetting.getValue())
        {
            final EnumFacing face = PlayerUtil.getFacing();
            builder.append(EnumChatFormatting.DARK_GRAY);
            builder.append("(");
            int offset;
            if ((offset = face.getFaceX()) != 0)
            {
                builder.append(offset == -1 ? "-" : "+");
                builder.append("X");
            }
            if ((offset = face.getFaceZ()) != 0)
            {
                builder.append(offset == -1 ? "-" : "+");
                builder.append("Z");
            }
            builder.append(") ");
            builder.append(EnumChatFormatting.RESET);
        }

        Vec3 pos = Vec3.createVectorHelper(MC.thePlayer.posX, MC.thePlayer.boundingBox.minY, MC.thePlayer.posZ);
        builder.append(EnumChatFormatting.GRAY);
        builder.append(String.format("%.1f, %.1f, %.1f", pos.xCoord, pos.yCoord, pos.zCoord));
        builder.append(EnumChatFormatting.RESET);

        if (netherCoordinatesSetting.getValue())
        {
            builder.append(" ");
            if (MC.thePlayer.dimension != -1)
            {
                builder.append(EnumChatFormatting.RED);
                builder.append(String.format("(%.1f, %.1f)", pos.xCoord / 8.0, pos.zCoord / 8.0));
                builder.append(EnumChatFormatting.RESET);
            } else
            {
                builder.append(EnumChatFormatting.BLUE);
                builder.append(String.format("(%.1f, %.1f)", pos.xCoord * 8.0, pos.zCoord * 8.0));
                builder.append(EnumChatFormatting.RESET);
            }
        }

        if (rotationSetting.getValue())
        {
            builder.append(EnumChatFormatting.GRAY);
            builder.append(" [");
            builder.append(String.format("%.1f", MC.thePlayer.rotationYaw));
            builder.append(", ");
            builder.append(String.format("%.1f", MC.thePlayer.rotationPitch));
            builder.append("]");
            builder.append(EnumChatFormatting.RESET);
        }

        return builder.toString();
    }
}
