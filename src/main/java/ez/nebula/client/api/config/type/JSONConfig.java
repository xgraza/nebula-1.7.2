package ez.nebula.client.api.config.type;

import com.google.gson.JsonElement;
import ez.nebula.client.api.config.IConfig;
import ez.nebula.client.util.io.FileUtil;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author xgraza
 * @since 9/7/26
 * @param <T> the type of {@link JsonElement}
 */
public abstract class JSONConfig<T extends JsonElement> implements IConfig
{
    private final Class<T> type;

    @SuppressWarnings("unchecked")
    public JSONConfig()
    {
        final Type type = getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType)
        {
            this.type = (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
        } else
        {
            throw new RuntimeException("Could not gather type");
        }
    }

    @Override
    public String write()
    {
        return FileUtil.GSON.toJson(writeJSON());
    }

    public abstract T writeJSON();

    @Override
    @SuppressWarnings("unchecked")
    public void read(final String data)
    {
        if (data == null || data.isEmpty())
        {
            return;
        }
        final JsonElement element = FileUtil.JSON_PARSER.parse(data);
        if (!type.isAssignableFrom(element.getClass()))
        {
            throw new RuntimeException("Mismatch type, looking for " + type + ", got " + element.getClass());
        }
        readJSON((T) element);
    }

    public abstract void readJSON(final T json);
}
