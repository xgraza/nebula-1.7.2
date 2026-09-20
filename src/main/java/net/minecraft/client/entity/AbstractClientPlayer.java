package net.minecraft.client.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.world.World;

public abstract class AbstractClientPlayer extends EntityPlayer
{
    public static final ResourceLocation locationStevePng = new ResourceLocation("textures/entity/steve.png");
    private ThreadDownloadImageData downloadImageSkin;
    private ThreadDownloadImageData downloadImageCape;
    private ResourceLocation locationSkin;
    private ResourceLocation locationCape;
    private static final String __OBFID = "CL_00000935";
    private String nameClear;

    public AbstractClientPlayer(World p_i45074_1_, GameProfile p_i45074_2_)
    {
        super(p_i45074_1_, p_i45074_2_);
        this.setupCustomSkin(p_i45074_2_);

        this.nameClear = p_i45074_2_.getName();

        if (this.nameClear != null && !this.nameClear.isEmpty())
        {
            this.nameClear = StringUtils.stripControlCodes(this.nameClear);
        }
    }

    protected void setupCustomSkin(GameProfile profile)
    {
        String var1 = this.getCommandSenderName();

        if (!var1.isEmpty())
        {
            this.locationSkin = getLocationSkin(var1);
            this.locationCape = getLocationCape(var1);
            this.downloadImageSkin = getDownloadImageSkin(this.locationSkin, var1);
            this.downloadImageCape = getDownloadImageCape(this.locationCape, var1);
        }
    }

    public ThreadDownloadImageData getTextureSkin()
    {
        return this.downloadImageSkin;
    }

    public ThreadDownloadImageData getTextureCape()
    {
        return this.downloadImageCape;
    }

    public ResourceLocation getLocationSkin()
    {
        return this.locationSkin;
    }

    public ResourceLocation getLocationCape()
    {
        return this.locationCape;
    }

    public static ThreadDownloadImageData getDownloadImageSkin(ResourceLocation par0ResourceLocation, String par1Str)
    {
        return getDownloadImage(par0ResourceLocation, getSkinUrl(par1Str), locationStevePng, new ImageBufferDownload());
    }

    public static ThreadDownloadImageData getDownloadImageCape(ResourceLocation par0ResourceLocation, String par1Str)
    {
        return getDownloadImage(par0ResourceLocation, getCapeUrl(par1Str), null, null);
    }

    private static ThreadDownloadImageData getDownloadImage(ResourceLocation par0ResourceLocation, String par1Str, ResourceLocation par2ResourceLocation, IImageBuffer par3IImageBuffer)
    {
        TextureManager var4 = Minecraft.getMinecraft().getTextureManager();
        Object var5 = var4.getTexture(par0ResourceLocation);

        if (var5 == null)
        {
            var5 = new ThreadDownloadImageData(null, par1Str, locationStevePng, par3IImageBuffer);
            var4.loadTexture(par0ResourceLocation, (ITextureObject) var5);
        }

        return (ThreadDownloadImageData) var5;
    }

    public static String getSkinUrl(String par0Str)
    {
        return String.format("https://minotar.net/skin/%s.png", StringUtils.stripControlCodes(par0Str));
    }

    public static String getCapeUrl(String par0Str)
    {
        return String.format("http://minecraft.net/MinecraftCloaks/%s.png", StringUtils.stripControlCodes(par0Str));
    }

    public static ResourceLocation getLocationSkin(String par0Str)
    {
        return new ResourceLocation("skins/" + StringUtils.stripControlCodes(par0Str));
    }

    public static ResourceLocation getLocationCape(String par0Str)
    {
        return new ResourceLocation("cloaks/" + StringUtils.stripControlCodes(par0Str));
    }

    public static ResourceLocation getLocationSkull(String par0Str)
    {
        return new ResourceLocation("skull/" + StringUtils.stripControlCodes(par0Str));
    }

    public String getNameClear()
    {
        return nameClear;
    }
}
