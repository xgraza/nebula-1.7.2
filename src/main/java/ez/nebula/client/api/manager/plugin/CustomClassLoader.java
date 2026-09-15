package ez.nebula.client.api.manager.plugin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author xgraza
 * @since 9/14/26
 */
final class CustomClassLoader extends URLClassLoader
{
    private final Map<String, byte[]> classBytes = new HashMap<>();
    private final Map<String, byte[]> fileBytes = new HashMap<>();

    private final Map<String, Class<?>> classCacheMap = new HashMap<>();

    public CustomClassLoader(final ClassLoader parent)
    {
        super(new URL[0], parent);
    }

    /**
     * Loads a ZIP-typed file into this class loader
     * @param zis the {@link ZipInputStream} to read from
     * @throws IOException
     * @return a {@link java.util.ArrayList} of all added classes
     */
    public List<Class<?>> from(final ZipInputStream zis) throws IOException
    {
        final List<Class<?>> classes = new ArrayList<>();

        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null)
        {
            final String name = entry.getName();

            if (entry.isDirectory())
            {
                classBytes.put(name, new byte[0]);
                continue;
            }

            final byte[] bytes = from(zis, entry.getSize());

            if (name.endsWith(".class"))
            {
                try
                {
                    final Class<?> clazz = define(name
                            .replaceAll("\\.class", "")
                            .replace("/", "."), bytes);
                    classBytes.put(name, bytes);
                    classes.add(clazz);
                } catch (final Exception e)
                {
                    throw new RuntimeException(e);
                }
            } else
            {
                fileBytes.put(name, bytes);
            }
            zis.closeEntry();
        }
        return classes;
    }

    private byte[] from(final InputStream is, final long size) throws IOException
    {
        byte[] bytes = new byte[Math.toIntExact(size)];
        int i;
        int index = 0;
        while ((i = is.read()) != -1)
        {
            bytes[index] = (byte) i;
            ++index;
        }
        return bytes;
    }

    @Override
    public URL getResource(final String name)
    {
        try
        {
            return new URL(null, "bytes:///" + name, new URLStreamHandler()
            {
                @Override
                protected URLConnection openConnection(final URL u)
                {
                    return new URLConnection(u)
                    {
                        @Override
                        public void connect()
                        {

                        }

                        @Override
                        public InputStream getInputStream()
                        {
                            byte[] bytes = fileBytes.get(getURL().getPath().substring(1));
                            return bytes == null ? null : new ByteArrayInputStream(bytes);
                        }
                    };
                }
            });
        } catch (final MalformedURLException e)
        {
            return super.getResource(name);
        }
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException
    {
        final String formatted = name.replaceAll("\\.", "/") + ".class";
        Class<?> clazz = classCacheMap.getOrDefault(formatted, null);
        if (clazz != null)
        {
            return clazz;
        }

        byte[] bytes = classBytes.get(formatted);
        if (bytes == null || bytes.length == 0)
        {
            return super.findClass(name);
        }

        clazz = define(name, bytes);
        if (clazz != null)
        {
            classCacheMap.put(formatted, clazz);
            return clazz;
        }

        return super.findClass(name);
    }

    public Class<?> define(final String name, byte[] bytes)
    {
        if (bytes == null || bytes.length == 0)
        {
            classBytes.remove(name);
            fileBytes.remove(name);
            classCacheMap.remove(name.replaceAll("\\.", "/") + ".class");
            if (bytes == null)
            {
                bytes = new byte[0];
            }
        }
        return defineClass(name, bytes, 0, bytes.length, (CodeSource) null);
    }
}