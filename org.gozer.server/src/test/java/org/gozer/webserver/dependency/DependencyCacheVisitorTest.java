package org.gozer.webserver.dependency;

import org.gozer.webserver.cache.DependencyCache;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.DefaultDependencyNode;

import java.io.File;
import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: sebastien
 * Date: 13/05/12
 */
public class DependencyCacheVisitorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyCacheVisitorTest.class);

    public static final String GROUP_ID = "org.springframework";
    public static final String ARTIFACT_ID = "spring-core";
    public static final String EXTENSION = "jar";
    public static final String VERSION = "3.0.5.RELEASE";
    public static final String SCOPE = "compile";
    private DependencyCacheVisitor visitor;
    private DependencyCache cache;
    private DefaultArtifact artifact;
    private DependencyNode node;

    @Before
    public void setUp() {

        visitor = new DependencyCacheVisitor();
        cache = mock(DependencyCache.class);
        visitor.setCache(cache);

        artifact = new DefaultArtifact(GROUP_ID, ARTIFACT_ID, EXTENSION, VERSION);
        node = new DefaultDependencyNode(new Dependency(artifact, SCOPE));
    }

    @Test
    public void should_put_in_cache_the_artifact_as_key_and_its_dependencies_as_value_when_entering_a_new_node() {

        when(cache.get(artifact.toString())).thenReturn(DependencyCache.notInCacheElement);

        visitor.visitEnter(node);

        verify(cache).put(artifact.toString(), node.getChildren());
    }

    @Test
    public void should_return_true_when_entering_a_new_node() {

        when(cache.get(artifact.toString())).thenReturn(DependencyCache.notInCacheElement);

        DependencyNode newNode = node;
        boolean isNewNode = visitor.visitEnter(newNode);

        assertTrue("visitEnter should return true with a new node", isNewNode);
    }

    @Test
    public void should_return_true_when_leaving_a_node() {
        boolean shouldAlwaysBeTrue = visitor.visitLeave(node);

        assertTrue("visitLeave should always return true", shouldAlwaysBeTrue);
    }

    @Test
    public void should_return_false_when_entering_a_known_node() {
        when(cache.get(node.getDependency().getArtifact().toString())).thenReturn(null)
                                                                       .thenReturn(new ArrayList<DependencyNode>());


        DependencyNode newNode = node;
        LOGGER.debug("first visit of the node");
        boolean isNewNode = visitor.visitEnter(newNode);
        DependencyNode knownNode = node;
        LOGGER.debug("Second visit of the node");
        boolean isKnownNode = visitor.visitEnter(knownNode);

        assertFalse("visitEnter should return false when entering an known node", isKnownNode);
    }

    @Test
    public void should_store_dependency_of_node_in_cache() {
        when(cache.get(node.getDependency().getArtifact().toString())).thenReturn(null)
                .thenReturn(new ArrayList<DependencyNode>());


        DependencyNode newNode = node;
        LOGGER.debug("first visit of the node");
        boolean isNewNode = visitor.visitEnter(newNode);
        DependencyNode knownNode = node;
        LOGGER.debug("Second visit of the node");
        boolean isKnownNode = visitor.visitEnter(knownNode);



        assertFalse("visitEnter should return false when entering an known node", isKnownNode);
    }
}
