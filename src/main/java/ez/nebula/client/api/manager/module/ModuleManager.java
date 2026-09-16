package ez.nebula.client.api.manager.module;

import ez.nebula.client.Nebula;
import ez.nebula.client.api.DebugFeature;
import ez.nebula.client.api.manager.ITypedManager;
import ez.nebula.client.api.manager.module.trait.ModuleInstance;
import ez.nebula.client.impl.config.ModuleConfig;
import ez.nebula.client.impl.module.combat.*;
import ez.nebula.client.impl.module.exploit.*;
import ez.nebula.client.impl.module.movement.*;
import ez.nebula.client.impl.module.player.*;
import ez.nebula.client.impl.module.render.*;
import ez.nebula.client.impl.module.world.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author xgraza
 * @since 02/14/25
 */
public final class ModuleManager implements ITypedManager<Module>
{
    static final Logger LOGGER = LogManager.getLogger("Modules");

    private final Map<Class<? extends Module>, Module> moduleInstanceMap = new LinkedHashMap<>();
    private final List<Module> moduleInstanceList = new LinkedList<>();

    @Override
    public void init()
    {
        register(new AntiFireballModule());
        register(new AutoArmorModule());
        register(new AutoBedModule());
        register(new AutoGGModule());
        register(new AutoLogModule());
        register(new AutoPotModule());
        register(new AutoTrapModule());
        register(new BurrowModule());
        register(new CriticalsModule());
        register(new IgniteModule());
        register(new KeepSprintModule());
        register(new KillAuraModule());
        register(new NoFriendsModule());
        register(new RegenModule());
        register(new VehicleBreakerModule());
        register(new VelocityModule());
        register(new WTapModule());
        register(new EnderchestBPModule());
        register(new FastLatencyModule());
        register(new FastUseModule());
        register(new FrankyModule());
        register(new GhostHandModule());
        register(new LongChatModule());
        register(new NewChunksModule());
        register(new NoC03Module());
        register(new NoHungerModule());
        register(new NoPacketKickModule());
        register(new NoRotateSetModule());
        register(new PacketCancellerModule());
        register(new PhaseModule());
        register(new PortalsModule());
        register(new PotionSaverModule());
        register(new TimerModule());
        register(new XCarryModule());
        register(new ZootModule());
        register(new AutoWalkModule());
        register(new BlinkModule());
        register(new EntityControlModule());
        register(new EntitySpeedModule());
        register(new FlyModule());
        register(new IceSpeedModule());
        register(new InvWalkModule());
        register(new JesusModule());
        register(new LongJumpModule());
        register(new NoAccelModule());
        register(new NoJumpDelayModule());
        register(new NoMoveDelayModule());
        register(new NoSlowModule());
        register(new PathFinderModule());
        register(new ReverseStepModule());
        register(new SafeWalkModule());
        register(new SpeedModule());
        register(new SprintModule());
        register(new StepModule());
        register(new TargetStrafeModule());
        register(new AntiBlockModule());
        register(new AntiDisconnectModule());
        register(new AntiLagModule());
        register(new AntiPortalModule());
        register(new AntiRevertModule());
        register(new AutoEatModule());
        register(new AutoRapeModule());
        register(new AutoReconnectModule());
        register(new AutoRespawnModule());
        register(new FreecamModule());
        register(new HotbarRefillModule());
        register(new InfiniteMoverModule());
        register(new InteractModule());
        register(new InventorySyncModule());
        register(new KeyPearlModule());
        register(new MCFModule());
        register(new NoFallModule());
        register(new NoSwingModule());
        register(new NotifierModule());
        register(new ParrotModule());
        register(new PearlPhaseModule());
        register(new SpammerModule());
        register(new TestModule());
        register(new TranslateModule());
        register(new YawModule());
        register(new AppleSkinModule());
        register(new BetterF3Module());
        register(new BreadcrumbsModule());
        register(new CameraClipModule());
        register(new ChamsModule());
        register(new ChatModifierModule());
        register(new ChunkBordersModule());
        register(new ClickGUIModule());
        register(new EntityCullingModule());
        register(new ESPModule());
        register(new ExtraTabModule());
        register(new FinderModule());
        register(new FullbrightModule());
        register(new GlintModule());
        register(new HeavenModule());
        register(new HUDModule());
        register(new HUDTestModule());
        register(new ItemPhysicsModule());
        register(new ItemTweaksModule());
        register(new LogoutSpotsModule());
        register(new NameProtectModule());
        register(new NametagsModule());
        register(new NoRenderModule());
        register(new TimeChangerModule());
        register(new TracersModule());
        register(new TrajectoriesModule());
        register(new TunnelESPModule());
        register(new UnfocusedCPUModule());
        register(new ViewModelModule());
        register(new WaypointsModule());
        register(new XRayModule());
        register(new AirPlaceModule());
        register(new AntiGhostBlockModule());
        register(new AutoFarmModule());
        register(new AutoFishModule());
        register(new AutoHighwayModule());
        register(new AutoLavaHoleFillModule());
        register(new AutoTagModule());
        register(new AutoToolModule());
        register(new AutoTorchModule());
        register(new AutoTreeModule());
        register(new AutoTunnelModule());
        register(new AutoWitherModule());
        register(new ChestStealerModule());
        register(new FakePlayerModule());
        register(new FlattenModule());
        register(new LandscaperModule());
        register(new NukerModule());
        register(new PacketMineModule());
        register(new PortalBreakerModule());
        register(new ScaffoldModule());
        register(new StashHunterModule());

        try
        {
            ModuleConfig.loadConfig("default");
        } catch (final IOException e)
        {
            LOGGER.error("Failed to load default module config!", e);
        }
        LOGGER.info("Registered {} modules", moduleInstanceList.size());
    }

    public void register(final Module module)
    {
        if (module.getClass().isAnnotationPresent(DebugFeature.class) && !Nebula.DEBUG)
        {
            return;
        }

        module.discoverSettings();

        moduleInstanceMap.put(module.getClass(), module);
        moduleInstanceList.add(module);

        // Find instance (if present)
        for (final Field field : module.getClass().getDeclaredFields())
        {
            if (field.isAnnotationPresent(ModuleInstance.class)
                    && field.getType().isAssignableFrom(module.getClass()))
            {
                try
                {
                    field.set(null, module);
                } catch (final IllegalAccessException e)
                {
                    LOGGER.error("Failed to set INSTANCE variable", e);
                }
                break;
            }
        }
        Nebula.COMMANDS.register(new ModuleCommand(module));
    }

    @Override
    public Module getReference(final Class<Module> type)
    {
        return moduleInstanceMap.get(type);
    }

    @Override
    public List<Module> getAll()
    {
        return moduleInstanceList;
    }
}
