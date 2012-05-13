package org.gozer.webserver.cache;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;


/**
 * Created with IntelliJ IDEA.
 * User: sebastien
 * Date: 09/05/12
 * Time: 23:42.
 */
public class DependencyCacheTest {

    @Test
    public void should_create_a_gozer_directory() {
        DependencyCache cache = DependencyCache.getInstance();
        cache.createGozerDir();
        assertTrue(new File(System.getProperty("user.home") + "/.gozer").exists());
    }
}
