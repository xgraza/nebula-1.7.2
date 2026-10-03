package ez.nebula.client.util.fml;

import java.lang.reflect.Field;

public final class FMLHelper
{
    private static Class<?> CORE_MOD_MANAGER_FML_CLASS;

    static
    {
        try
        {
            CORE_MOD_MANAGER_FML_CLASS = Class.forName("cpw.mods.fml.relauncher.CoreModManager");
        } catch (ClassNotFoundException ignored)
        {
        }
    }

    public static boolean isDeobfEnv()
    {
        if (CORE_MOD_MANAGER_FML_CLASS == null)
        {
            return false;
        }
        try
        {
            Field field = CORE_MOD_MANAGER_FML_CLASS.getDeclaredField("deobfuscatedEnvironment");
            return field.getBoolean(null);
        } catch (Exception ignored)
        {
            return false;
        }
    }
}
