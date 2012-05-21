package org.gozer.webserver.cache;

import javolution.util.FastMap;
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
    private FastMap<String, Collection<DependencyNode>> manager;
    private File gozerDir;

    private DependencyCache() {
        manager = new FastMap<String, Collection<DependencyNode>>();
        createGozerDir();
    }

    void createGozerDir() {
        gozerDir = new File(System.getProperty("user.home")+"/.gozer");
        if (!gozerDir.exists()) {
            gozerDir.mkdir();
        }
    }


    public void flush() {
        getCache().clear();
    }

    Map<String, Collection<DependencyNode>> getCache() {
        LOGGER.debug("cache : {}", manager);
        return manager;
    }


//    private void shutdown() {
//        manager.shutdown();
//    }

    public void put(String artifact, Collection<DependencyNode> dependencies) {
        getCache().put(artifact, dependencies);
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

        Collection<DependencyNode> element = getCache().get(artifact);
        if (element != null) {
            LOGGER.debug("value in cache : {}", element);
            return element;
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
