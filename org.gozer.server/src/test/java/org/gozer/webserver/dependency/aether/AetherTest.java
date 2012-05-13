package org.gozer.webserver.dependency.aether;

import org.gozer.webserver.dependency.aether.Aether;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.*;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: sebastien
 * Date: 29/03/12
 */
public class AetherTest {


    private static final String GROUP_ID = "org.springframework";
    private static final String GROUP_ID_PART_1 = "org";
    private static final String GROUP_ID_PART_2 = "springframework";
    private static final String ARTIFACT_ID = "spring-core";
    private static final String VERSION = "3.0.5.RELEASE";
    private static final String GOZER_METADATA_ZIP = "gozer-metadata.zip";
    private static final String URL = GROUP_ID_PART_1+"/"+GROUP_ID_PART_2+"/"+ARTIFACT_ID+"/"+VERSION+"/"+GOZER_METADATA_ZIP;
    private static final String[] PARTS_OF_URL = new String[]{GROUP_ID_PART_1, GROUP_ID_PART_2, ARTIFACT_ID, VERSION, GOZER_METADATA_ZIP};
    private static final String SCOPE = "compile";
    private static final String EXTENSION = "jar";
    private static final String REPOSITORIES = "Repositories";


    private Aether aether;
    private Dependency dependency;
    private RepositorySystemSession session;
    private List<RemoteRepository> repositories;
    private Artifact expectedArtifact;

    @Before
    public void setUp() {
        aether = new Aether();
        session = aether.newSession();
        repositories = new ArrayList<RemoteRepository>();
        repositories.add(aether.newCentralRepository());
        expectedArtifact = new DefaultArtifact(GROUP_ID, ARTIFACT_ID, EXTENSION, VERSION);
        dependency = new Dependency(expectedArtifact, SCOPE);

    }

    @Test
    public void should_read_central_repository_from_request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(REPOSITORIES)).thenReturn("central=http://repo1.maven.org/maven2/");
        List<RemoteRepository> repositories = aether.readRepositoriesFromRequest(request);
        RemoteRepository expectedRepository = aether.newCentralRepository();
        assertNotNull(repositories);
        assertEquals(expectedRepository, repositories.get(0));
    }

    @Test
    public void should_return_central_repository_with_no_header_in_request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(REPOSITORIES)).thenReturn(null);
        List<RemoteRepository> repositories = aether.readRepositoriesFromRequest(request);
        RemoteRepository expectedRepository = aether.newCentralRepository();
        assertNotNull(repositories);
        assertEquals(expectedRepository, repositories.get(0));
    }

    @Test
    public void getArtifactFromRequest_should_retrieve_artifact_information_from_url() {
        String url = URL;
        Artifact artifact = aether.getArtifactFromRequest(url);
        assertEquals(expectedArtifact, artifact);
    }

    @Test
    public void should_return_the_central_repository() {
        RemoteRepository central = aether.newCentralRepository();
        assertEquals(central.getContentType(), "default");
        assertEquals(central.getUrl(), "http://repo1.maven.org/maven2/");
        assertEquals(central.getId(), "central");
    }


    @Test
    public void should_build_a_groupId_from_the_parts_of_url() {
        String groupId = aether.buildGroupId(PARTS_OF_URL);
        assertEquals(GROUP_ID, groupId);
    }

    @Test
    public void should_select_the_artifactId_part_of_the_url() {
        String artifactId = aether.buildArtifactId(PARTS_OF_URL);
        assertEquals(ARTIFACT_ID, artifactId);
    }

    @Test
    public void should_select_the_version_part_of_the_url() {
        String version = aether.buildVersion(PARTS_OF_URL);
        assertEquals(VERSION, version);
    }


    @Test
    public void should_init_the_repository_system() {
        aether.init();
        assertNotNull("the repository system should have been initialized", aether.getRepositorySystem());
    }

    @Test
    public void should_create_a_collectionRequest_from_a_dependency() {
        CollectRequest collectRequest = aether.buildCollectRequest(dependency, repositories);
        assertNotNull("the request should not be null", collectRequest);
    }

    @Test
    public void should_create_a_dependencyRequest_from_a_collectRequest() {
        CollectRequest collectRequest = aether.buildCollectRequest(dependency, repositories);
        DependencyNode node = aether.getNodeFromCollectRequest(session, collectRequest);
        assertNotNull("the node should not be null", node);
    }

    @Test
    public void should_resolve_dependencies() throws DependencyResolutionException {
        CollectRequest collectRequest = aether.buildCollectRequest(dependency, repositories);
        DependencyNode node = aether.getNodeFromCollectRequest(session, collectRequest);
        DependencyResult results = aether.resolveDependencies(session, node);
        assertNotNull("the results should not be null", results);
    }


    @Test
    public void should_resolve_metadata() {
        List<MetadataResult> results = aether.resolveMetadata(session, repositories, dependency);
        assertNotNull("the results should not be null", results);
    }



}
