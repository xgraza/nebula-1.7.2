package us.nebula.gradle

import java.nio.file.Files
import java.security.MessageDigest

/**
 * @author xgraza
 * @since 1.0.0
 */
class Util {
    static final String SEPARATOR = System.getProperty("file.separator")
    static final String USER_DIR = System.getProperty("user.dir")

    static execute(String command, String defaultValue) {
        try {
            return command.execute().text.trim()
        } catch (exception) {
            exception.printStackTrace()
            return defaultValue
        }
    }

    enum OS {
        WINDOWS,
        OSX,
        UNIX
    }
}
