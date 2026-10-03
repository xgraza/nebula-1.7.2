package ez.nebula.client.util.optifine;

import java.lang.reflect.Method;

public class OptifineHelper
{
    private static Class<?> OPTIFINE_CLASS;

    public static boolean isFastRender()
    {
        if (OPTIFINE_CLASS == null)
        {
            return false;
        }
        try
        {
            final Method method = OPTIFINE_CLASS.getDeclaredMethod("isFastRender");
            return (boolean) method.invoke(null);
        } catch (Exception ignored)
        {
            return false;
        }
    }

    public static boolean exists()
    {
        return OPTIFINE_CLASS != null;
    }

    static
    {
        try
        {
            OPTIFINE_CLASS = Class.forName("Config");
        } catch (Exception ignored)
        {
        }
    }
}
