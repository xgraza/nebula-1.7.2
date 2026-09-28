package ez.nebula.client.impl.hud;

import ez.nebula.client.api.manager.hud.HUDElement;
import ez.nebula.client.api.manager.hud.trait.HUDManifest;
import ez.nebula.client.api.setting.Setting;
import ez.nebula.client.util.minecraft.player.PlayerUtil;
import ez.nebula.client.util.render.font.Fonts;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import java.util.TreeMap;

/**
 * @author xgraza
 * @since 3/23/26
 */
@HUDManifest(name = "Coordinates",
        description = "Displays overworld and nether coordinates, with your direction",
        x = 2, y = 2)
public final class CoordinatesHUDElement extends HUDElement
{
    private static final TreeMap<Integer, String> DIRECTION_MAP = new TreeMap<>();

    static
    {
        DIRECTION_MAP.put(0, "South");
        DIRECTION_MAP.put(45, "South West");
        DIRECTION_MAP.put(90, "West");
        DIRECTION_MAP.put(135, "North West");
        DIRECTION_MAP.put(180, "North");
        DIRECTION_MAP.put(225, "North East");
        DIRECTION_MAP.put(270, "East");
        DIRECTION_MAP.put(315, "South East");
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
    public void render(final ScaledResolution res)
    {
        final String text = getString();
        setWidth(Fonts.POPPINS.getStringWidth(text) + (getPadding() * 2.0));
        setHeight(Fonts.POPPINS.getFontHeight() + (getPadding() * 2.0));
        Fonts.POPPINS.drawStringShadow(text, x, y, -1);
    }

    private String getString()
    {
        final StringBuilder builder = new StringBuilder();

        if (directionSetting.getValue())
        {
            final Integer key = DIRECTION_MAP.floorKey(Math.abs((int) (MC.thePlayer.rotationYaw % 360.0f)));
            if (key != null)
            {
                final String dir = DIRECTION_MAP.get(key);

                builder.append(EnumChatFormatting.DARK_GRAY);
                if (!axisSetting.getValue())
                {
                    builder.append("(");
                }
                if (shortenedSetting.getValue())
                {
                    final String[] parts = dir.split(" ");
                    builder.append(parts[0].charAt(0));
                    if (parts.length == 2)
                    {
                        builder.append(parts[1].charAt(0));
                    }
                } else
                {
                    builder.append(dir);
                }
                if (!axisSetting.getValue())
                {
                    builder.append(")");
                }
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
            if ((offset = face.getFrontOffsetX()) != 0)
            {
                builder.append(offset == -1 ? "-" : "+");
                builder.append("X");
            }
            if ((offset = face.getFrontOffsetZ()) != 0)
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
                builder.append(String.format("(N: %.1f, %.1f)", pos.xCoord / 8.0, pos.zCoord / 8.0));
                builder.append(EnumChatFormatting.RESET);
            } else
            {
                builder.append(EnumChatFormatting.BLUE);
                builder.append(String.format("(OV: %.1f, %.1f)", pos.xCoord * 8.0, pos.zCoord * 8.0));
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
