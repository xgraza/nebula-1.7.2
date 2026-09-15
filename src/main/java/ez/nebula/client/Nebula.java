package ez.nebula.client;

import com.github.lunatrius.schematica.Schematica;
import ez.nebula.client.api.config.ConfigManager;
import ez.nebula.client.api.manager.account.AccountManager;
import ez.nebula.client.api.manager.command.CommandManager;
import ez.nebula.client.api.manager.friend.FriendManager;
import ez.nebula.client.api.manager.hud.HUDManager;
import ez.nebula.client.api.manager.hud2.HUDElementManager;
import ez.nebula.client.api.manager.key.KeyManager;
import ez.nebula.client.api.manager.module.ModuleManager;
import ez.nebula.client.api.manager.toast.ToastManager;
import ez.nebula.client.api.manager.waypoint.WaypointManager;
import ez.nebula.client.api.player.InteractionManager;
import ez.nebula.client.api.player.movement.MovementController;
import ez.nebula.client.api.player.server.InventoryManager;
import ez.nebula.client.api.player.server.RotationManager;
import ez.nebula.client.api.player.server.ServerManager;
import ez.nebula.client.api.manager.plugin.PluginManager;
import ez.nebula.client.api.tray.SystemNotifications;
import ez.nebula.client.impl.gui.startup.LoadingScreen;
import ez.nebula.client.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SplashTextProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author xgraza
 * @since 9/6/26
 */
public final class Nebula
{
    private static final Logger LOGGER = LogManager.getLogger(BuildConfig.NAME);
    private static final ResourceLocation NEBULA_SPLASH_TEXT_RESOURCE = new ResourceLocation(
            "nebula", "splashs.txt");

    /**
     * The current Nebula version based on SemVer specifications
     */
    public static final String VERSION = BuildConfig.VERSION
            + "-" + BuildConfig.ENV
            + "." + BuildConfig.BUILD
            + "." + BuildConfig.BRANCH
            + "+" + BuildConfig.HASH;

    /**
     * The origin branch of the code
     */
    public static final String GITHUB_REPO = "https://github.com/xgraza/nebula-1.7.2/tree/"
            + BuildConfig.BRANCH;

    public static boolean FOLK_VALLEY = false;

    /**
     * If features should use heavier debugging
     */
    public static boolean DEBUG;

    /**
     * If to use Nebula splash text on the main menu screen
     */
    public static boolean USE_CUSTOM_SPLASH_TEXT;

    /**
     * If the user has opened the ClickGUI for the first time
     */
    public static boolean OPENED_GUI_BEFORE;

    public static final Executor EXECUTOR = Executors.newFixedThreadPool(1);
    public static File NEBULA_ROOT;

    public static final ConfigManager CONFIGS = new ConfigManager();
    public static final KeyManager KEYS = new KeyManager();
    public static final CommandManager COMMANDS = new CommandManager();
    public static final HUDManager HUD_OLD = new HUDManager();
    public static final HUDElementManager HUD_NEW = new HUDElementManager();
    public static final ModuleManager MODULES = new ModuleManager();
    public static final AccountManager ACCOUNTS = new AccountManager();
    public static final FriendManager FRIENDS = new FriendManager();
    public static final ToastManager TOASTS = new ToastManager();
    public static final InventoryManager INVENTORY = new InventoryManager();
    public static final RotationManager ROTATIONS = new RotationManager();
    public static final ServerManager SERVER = new ServerManager();
    public static final WaypointManager WAYPOINTS = new WaypointManager();
    public static final InteractionManager INTERACTIONS = new InteractionManager();
    public static final MovementController MOVEMENT_CONTROLLER = new MovementController();
    public static final PluginManager PLUGINS = new PluginManager();

    /**
     * Initializes nebula client
     * @param gameDir the {@link File} to the current working directory
     */
    public static void init(final File gameDir)
    {
        System.out.println();
        logBuildInfo();

        LoadingScreen.setTotalLoadingStages(12);
        LoadingScreen.setStage(1, "Pre-initialization");
        createNebulaDirectories(gameDir);
        SplashTextProvider.addSplashTextProvider(NEBULA_SPLASH_TEXT_RESOURCE);

        LoadingScreen.setStage(3, "Initializing Nebula Client...");
        long endTime;
        final long startTime = System.nanoTime();

        KEYS.init();
        HUD_OLD.init();
        HUD_NEW.init();
        COMMANDS.init();
        MODULES.init();
        SERVER.init();
        ROTATIONS.init();
        INVENTORY.init();
        INTERACTIONS.init();
        ACCOUNTS.init();
        TOASTS.init();
        FRIENDS.init();
        WAYPOINTS.init();
        SystemNotifications.init();
        Schematica.load();

        try
        {
            RenderUtil.initShaders();
        } catch (final Exception e)
        {
            LOGGER.error("Failed to initialize shaders!", e);
        }

        endTime = System.nanoTime();
        LOGGER.info("Initialized Nebula Client in {}ms", (endTime - startTime) / 1000000.0);

        LoadingScreen.setStage(4, "Loading configs");
        CONFIGS.init();

        LoadingScreen.setStage(5, "Loading plugins");
        PLUGINS.init();

        LoadingScreen.setStage(6, "Post-initialization");
        setIcon();
        setTitle("Nebula " + VERSION);
    }

    private static void createNebulaDirectories(final File gameDir)
    {
        NEBULA_ROOT = new File(gameDir, "nebula-client");
        if (!NEBULA_ROOT.exists())
        {
            LoadingScreen.setStage(2, "Initializing Nebula directories");
            LOGGER.info("First time launching Nebula, creating {}", NEBULA_ROOT);
            if (NEBULA_ROOT.mkdir())
            {
                LOGGER.info("Created successfully!");
            } else
            {
                throw new RuntimeException("Failed to create " + NEBULA_ROOT + " directory! :(");
            }
        }
    }

    private static void setTitle(final String title)
    {
        final Util.EnumOS os = Util.getOSType();
        if (os == Util.EnumOS.WINDOWS || os == Util.EnumOS.MACOS)
        {
            Display.setTitle(title);
        } else
        {
            Minecraft.setTitle(title);
        }
    }

    private static void setIcon()
    {
        if (Util.getOSType() == Util.EnumOS.MACOS)
        {
            try
            {
                final InputStream is = Nebula.class.getResourceAsStream("/assets/nebula/texture/icon/128x.png");
                final BufferedImage image = ImageIO.read(is);
                final Class<?> clazz = Class.forName("com.apple.eawt.Application");
                final Method getApplicationMethod = clazz.getDeclaredMethod("getApplication");
                final Object application = getApplicationMethod.invoke(null);
                final Method setDockIconMethod = application.getClass().getDeclaredMethod("setDockIconImage", Image.class);
                setDockIconMethod.invoke(application, image);
                return;
            } catch (final Exception e)
            {
                LOGGER.error("Failed to set dock icon with EAWT, fallback to LWJGL2");
            }
        }

        try
        {
            ByteBuffer buffer16x = readImage("/assets/nebula/texture/icon/16x.png");
            ByteBuffer buffer32x = readImage("/assets/nebula/texture/icon/32x.png");
            ByteBuffer buffer128x = readImage("/assets/nebula/texture/icon/128x.png");
            if (buffer16x == null || buffer32x == null || buffer128x == null)
            {
                LOGGER.error("Failed to read Nebula icon buffer(s).");
                return;
            }

            Display.setIcon(new ByteBuffer[]{ buffer16x, buffer32x, buffer128x });
        } catch (final Exception exception)
        {
            LOGGER.error("Couldn't set icon", exception);
        }
    }

    private static ByteBuffer readImage(String location)
    {
        try (final InputStream is = Nebula.class.getResourceAsStream(location))
        {
            if (is == null)
            {
                return null;
            }
            BufferedImage bufferedimage = ImageIO.read(is);
            int[] aint = bufferedimage.getRGB(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), null, 0, bufferedimage.getWidth());
            ByteBuffer bytebuffer = ByteBuffer.allocate(4 * aint.length);

            for (int i : aint)
            {
                bytebuffer.putInt(i << 8 | i >> 24 & 255);
            }

            bytebuffer.flip();
            return bytebuffer;
        } catch (IOException e)
        {
            return null;
        }
    }

    private static void logBuildInfo()
    {
        LOGGER.info("Version: {}", VERSION);
        LOGGER.info("Build Time: " + BuildConfig.BUILD_TIME);
        if (DEBUG)
        {
            LOGGER.warn("\t###");
            LOGGER.warn("\tNebula debug is enabled!");
            LOGGER.warn("\t##");
        }
    }
}
