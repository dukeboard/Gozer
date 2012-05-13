package org.gozer.webserver.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.sonatype.aether.graph.DependencyNode;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: sebastien
 * Date: 09/05/12
 * Time: 22:20
 */
public class DependencyCache {

    private static final DependencyCache SINGLETON = new DependencyCache();
    private CacheManager manager;
    private File gozerDir;

    private DependencyCache() {
        manager = new CacheManager();
        createGozerDir();


        //Create a Cache specifying its configuration.
        Cache testCache = new Cache(
                new CacheConfiguration("testCache", 1000)
                        .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
                        .overflowToDisk(true)
                        .eternal(true)
                        .timeToLiveSeconds(60)
                        .timeToIdleSeconds(30)
                        .diskPersistent(false)
                        .diskStorePath(gozerDir.getAbsolutePath())
                        .diskExpiryThreadIntervalSeconds(0));
        manager.addCache(testCache);
    }

    void createGozerDir() {
        gozerDir = new File(System.getProperty("user.home")+"/.gozer");
        if (!gozerDir.exists()) {
            gozerDir.mkdir();
        }
    }


    public void flush() {
        getCache().flush();
    }

    Cache getCache() {
        return manager.getCache("testCache");
    }


    private void shutdown() {
        manager.shutdown();
    }

    public void put(String artifact, Collection<DependencyNode> dependencies) {
        Element element = new Element(artifact, dependencies);
        getCache().put(element);
    }

    public File get(String artifact) {
        return (File) getCache().get(artifact).getValue();
    }


    public static DependencyCache getInstance() {
        return SINGLETON;
    }
}
