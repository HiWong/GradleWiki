package wang.imallen.blog.rshrinker.log;

import org.gradle.api.logging.Logging;

/**
 * author: AllenWang
 * date: 2019/3/31
 */
public class Logger {

    private static final String DEFAULT_TAG = "RShrinker";

    private static org.gradle.api.logging.Logger logger = Logging.getLogger("RShrinker");

    public static void d(String msg) {
        d(DEFAULT_TAG, msg);
    }

    public static void d(String tag, String msg) {
        logger.debug(tag, msg);
    }

    public static void d(String msg, Object... objs) {
        logger.debug(msg, objs);
    }

    public static void i(String tag, String msg) {
        logger.info(tag, msg);
    }

    public static void i(String msg, Object... objs) {
        logger.info(msg, objs);
    }

    public static void i(String msg) {
        i(DEFAULT_TAG, msg);
    }

    public static void e(String tag, String msg) {
        logger.error(tag, msg);
    }

    public static void e(String msg) {
        e(DEFAULT_TAG, msg);
    }

    public static void life(String msg) {
        logger.lifecycle(msg);
    }

    public static void life(String msg, Object... objs) {
        logger.lifecycle(msg, objs);
    }
}
