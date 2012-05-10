package org.gozer.webserver.dependency;

import org.gozer.webserver.cache.DependencyCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: sebastien
 * Date: 10/05/12
 */
public class DependencyCacheDumper implements DependencyVisitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyCacheDumper.class);

    private DependencyCache cache;

    public DependencyCacheDumper() {
        cache = new DependencyCache();
    }

    @Override
    public boolean visitEnter(DependencyNode node) {
        LOGGER.debug("entering {}", node.getDependency());
        cache.put(node.getDependency().toString(), null);
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean visitLeave(DependencyNode node) {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
