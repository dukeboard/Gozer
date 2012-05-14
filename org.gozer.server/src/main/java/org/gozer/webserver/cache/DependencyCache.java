package org.gozer.webserver.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.DefaultDependencyNode;

import java.io.File;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: sebastien
 * Date: 09/05/12
 */
public class DependencyCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyCache.class);
    private static final DependencyCache SINGLETON = new DependencyCache();
    public static final NotInCacheElement notInCacheElement = new NotInCacheElement();
    private CacheManager manager;
    private File gozerDir;

    private DependencyCache() {
        manager = new CacheManager();
        createGozerDir();


        //Create a Cache specifying its configuration.
        Cache gozerCache = new Cache(
                new CacheConfiguration("gozer", 1000)
                        .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)
                        .overflowToDisk(true)
                        .eternal(false)
                        .timeToLiveSeconds(60)
                        .timeToIdleSeconds(30)
                        .diskPersistent(false)
                        .diskStorePath(gozerDir.getAbsolutePath())
                );
        manager.addCache(gozerCache);
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
        LOGGER.debug("cache : {}", manager.getCache("gozer"));
        return manager.getCache("gozer");
    }


    private void shutdown() {
        manager.shutdown();
    }

    public void put(String artifact, Collection<DependencyNode> dependencies) {
        Element element = new Element(artifact, getDependenciesAsString(dependencies));
        getCache().put(element);
    }


    String getDependenciesAsString(Collection<DependencyNode> dependencies) {
         StringBuilder sb = new StringBuilder();
         for (DependencyNode dependencyNode : dependencies) {
            sb.append(dependencyNode.getDependency().getArtifact()).append(";");
         }
        return sb.toString();
    }

    Collection<DependencyNode> getStringAsDependencies(String dependencies) {
       Collection<DependencyNode> nodes = new ArrayList<DependencyNode>();
       for (String dependency : dependencies.split(";")) {
           nodes.add(new DefaultDependencyNode(new Dependency(new DefaultArtifact(dependency), "compile")));
       }
       return nodes;
    }

    public Collection<DependencyNode> get(String artifact) {

        Element element = getCache().get(artifact);
        if (element != null) {
            LOGGER.debug("value in cache : {}", element.getValue());
            return (Collection<DependencyNode>) getStringAsDependencies((String)element.getValue());
        } else {
            return notInCacheElement;
        }
    }


    public static DependencyCache getInstance() {
        return SINGLETON;
    }

    private static class NotInCacheElement extends AbstractCollection<DependencyNode> {
        @Override
        public Iterator<DependencyNode> iterator() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public int size() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
