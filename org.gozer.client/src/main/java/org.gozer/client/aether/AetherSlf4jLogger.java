package org.gozer.client.aether;

import org.slf4j.LoggerFactory;
import org.sonatype.aether.spi.log.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 28/03/12
 * Time: 22:17
 */
public class AetherSlf4jLogger implements Logger {
    
    private org.slf4j.Logger logger = LoggerFactory.getLogger(this.getClass());
    
    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        logger.debug(s);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        logger.debug(s,throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        logger.warn(s);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        logger.warn(s,throwable);
    }
}
