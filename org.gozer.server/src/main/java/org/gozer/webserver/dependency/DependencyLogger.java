package org.gozer.webserver.dependency;

import org.gozer.webserver.cache.DependencyCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;

/**
 * Created with IntelliJ IDEA.
 * User: sebastien
 * Date: 10/05/12
 * Time: 13:59.
 */
public class DependencyLogger implements DependencyVisitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyLogger.class);


    public DependencyLogger() {
    }

    @Override
    public boolean visitEnter(DependencyNode node) {
        LOGGER.debug("entering");
        LOGGER.debug("aliases {}", node.getAliases());
        LOGGER.debug("dependency {}", node.getDependency());
        LOGGER.debug("children {}", node.getChildren());
        LOGGER.debug("data {}", node.getData());
        LOGGER.debug("premanaged scope {}", node.getPremanagedScope());
        LOGGER.debug("premanaged version {}", node.getPremanagedVersion());
        LOGGER.debug("relocations {}", node.getRelocations());
        LOGGER.debug("repositories {}", node.getRepositories());
        LOGGER.debug("request context {}", node.getRequestContext());
        LOGGER.debug("version {}", node.getVersion());
        LOGGER.debug("version constraint {}", node.getVersionConstraint());


        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean visitLeave(DependencyNode node) {
        LOGGER.debug("----------------------------------------------------------");
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
