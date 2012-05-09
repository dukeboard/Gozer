package org.gozer.webserver.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: sebastien
 * Date: 09/05/12
 * Time: 22:20
 */
public class DependencyCache {

    private CacheManager manager;
    private File gozerDir;

    public DependencyCache() {
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


    public static final void main(String... args) {
        System.out.println("BEGIN");
        DependencyCache cache = new DependencyCache();
        cache.put("test", new File("/home/sebastien/PanelBoueeGrid.java"));
        cache.flush();
        cache.shutdown();
        System.out.println("END");
    }

    private void shutdown() {
        manager.shutdown();
    }

    public void put(String key, File file) {
        Element element = new Element(key, file);
        getCache().put(element);
    }

    public File get(String key) {
        return (File) getCache().get(key).getValue();
    }



}
