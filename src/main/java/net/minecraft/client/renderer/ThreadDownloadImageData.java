package net.minecraft.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.src.*;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadDownloadImageData extends SimpleTexture
{
    private static final Logger logger = LogManager.getLogger();
    private static final AtomicInteger threadDownloadCounter = new AtomicInteger(0);
    private final String imageUrl;
    private final IImageBuffer imageBuffer;
    private BufferedImage bufferedImage;
    private Thread imageThread;
    private boolean textureUploaded;
    public boolean enabled = true, pipeline = false;
    private final File cacheFile;

    public ThreadDownloadImageData(File cacheFileIn, String par1Str, ResourceLocation par2ResourceLocation, IImageBuffer par3IImageBuffer)
    {
        super(par2ResourceLocation);
        this.imageUrl = par1Str;
        this.imageBuffer = par3IImageBuffer;
        this.cacheFile = cacheFileIn;
    }

    private void checkTextureUploaded()
    {
        if (!this.textureUploaded && this.bufferedImage != null)
        {
            this.textureUploaded = true;

            if (this.textureLocation != null)
            {
                this.deleteGlTexture();
            }

            TextureUtil.uploadTextureImage(super.getGlTextureId(), this.bufferedImage);
        }
    }

    public int getGlTextureId()
    {
        this.checkTextureUploaded();
        return super.getGlTextureId();
    }

    public void setBufferedImage(BufferedImage p_147641_1_)
    {
        this.bufferedImage = p_147641_1_;
    }

    public void loadTexture(IResourceManager par1ResourceManager) throws IOException
    {
        if (this.bufferedImage == null && this.textureLocation != null)
        {
            super.loadTexture(par1ResourceManager);
        }

        if (this.imageThread == null)
        {
            this.imageThread = new Thread("Texture Downloader #" + threadDownloadCounter.incrementAndGet())
            {
                public void run()
                {
                    HttpURLConnection var1 = null;
                    ThreadDownloadImageData.logger.debug("Downloading http texture from {} to {}", ThreadDownloadImageData.this.imageUrl, ThreadDownloadImageData.this.cacheFile);

                    if (shouldPipeline())
                    {
                        loadPipelined();
                    } else
                    {

                        try
                        {
                            var1 = (HttpURLConnection) (new URL(ThreadDownloadImageData.this.imageUrl)).openConnection(Minecraft.getMinecraft().getProxy());
                            var1.setDoInput(true);
                            var1.setDoOutput(false);
                            var1.connect();

                            if (var1.getResponseCode() / 100 != 2)
                            {
                                if (var1.getErrorStream() != null)
                                {
                                    Config.readAll(var1.getErrorStream());
                                }

                                return;
                            }

                            BufferedImage var6;

                            if (ThreadDownloadImageData.this.cacheFile != null)
                            {
                                FileUtils.copyInputStreamToFile(var1.getInputStream(), ThreadDownloadImageData.this.cacheFile);
                                var6 = ImageIO.read(ThreadDownloadImageData.this.cacheFile);
                            } else
                            {
                                var6 = TextureUtil.readBufferedImage(var1.getInputStream());
                            }

                            if (ThreadDownloadImageData.this.imageBuffer != null)
                            {
                                var6 = ThreadDownloadImageData.this.imageBuffer.parseUserSkin(var6);
                            }

                            ThreadDownloadImageData.this.setBufferedImage(var6);
                        } catch (Exception var61)
                        {
                            ThreadDownloadImageData.logger.error("Couldn't download http texture", var61);
                        } finally
                        {
                            if (var1 != null)
                            {
                                var1.disconnect();
                            }
                        }
                    }
                }
            };
            this.imageThread.setDaemon(true);
            this.imageThread.setName("Skin downloader: " + this.imageUrl);
            this.imageThread.start();

            try
            {
                URL e = new URL(this.imageUrl);
                String path = e.getPath();
                String prefixSkin = "/MinecraftSkins/";
                String prefixCape = "/MinecraftCloaks/";

                if (path.startsWith(prefixCape))
                {
                    String file = path.substring(prefixCape.length());
                    String ofUrl = "http://s.optifine.net/capes/" + file;
                    ThreadDownloadImage t = new ThreadDownloadImage(this, ofUrl, new ImageBufferDownload());
                    t.setDaemon(true);
                    t.setName("Cape downloader: " + this.imageUrl);
                    t.start();
                }
            } catch (Exception var9)
            {
            }
        }
    }

    private boolean shouldPipeline()
    {
        if (!this.pipeline)
        {
            return false;
        } else
        {
            Proxy proxy = Minecraft.getMinecraft().getProxy();
            return (proxy.type() == Proxy.Type.DIRECT || proxy.type() == Proxy.Type.SOCKS) && this.imageUrl.startsWith("http://");
        }
    }

    private void loadPipelined()
    {
        try
        {
            HttpRequest httprequest = HttpPipeline.makeRequest(this.imageUrl, Minecraft.getMinecraft().getProxy());
            HttpResponse httpresponse = HttpPipeline.executeRequest(httprequest);

            if (httpresponse.getStatus() / 100 != 2)
            {
                return;
            }

            byte[] abyte = httpresponse.getBody();
            ByteArrayInputStream bytearrayinputstream = new ByteArrayInputStream(abyte);
            BufferedImage bufferedimage;

            if (this.cacheFile != null)
            {
                FileUtils.copyInputStreamToFile(bytearrayinputstream, this.cacheFile);
                bufferedimage = ImageIO.read(this.cacheFile);
            } else
            {
                bufferedimage = TextureUtil.readBufferedImage(bytearrayinputstream);
            }

            if (this.imageBuffer != null)
            {
                bufferedimage = this.imageBuffer.parseUserSkin(bufferedimage);
            }

            this.setBufferedImage(bufferedimage);
        } catch (Exception exception)
        {
            logger.error("Couldn't download http texture: " + exception.getClass().getName() + ": " + exception.getMessage());
        }
    }

    public boolean isTextureUploaded()
    {
        if (!this.enabled)
        {
            return false;
        } else
        {
            this.checkTextureUploaded();
            return this.textureUploaded;
        }
    }
}
