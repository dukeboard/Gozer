package org.gozer.webserver.servlet;

import org.junit.Before;
import org.junit.Test;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: Sébastien
 * Date: 29/03/12
 */
public class AetherHelperTest {


    private static final String GROUP_ID = "org.springframework";
    private static final String GROUP_ID_PART_1 = "org";
    private static final String GROUP_ID_PART_2 = "springframework";
    private static final String ARTIFACT_ID = "spring-core";
    private static final String VERSION = "3.0.5.RELEASE";
    private static final String GOZER_METADATA_ZIP = "gozer-metadata.zip";
    private static final String URL = GROUP_ID_PART_1+"/"+GROUP_ID_PART_2+"/"+ARTIFACT_ID+"/"+VERSION+"/"+GOZER_METADATA_ZIP;
    private static final String[] PARTS_OF_URL = new String[]{GROUP_ID_PART_1, GROUP_ID_PART_2, ARTIFACT_ID, VERSION, GOZER_METADATA_ZIP};

    private AetherHelper helper;
    @Before
    public void setUp() {
        helper = new AetherHelper();
    }

    @Test
    public void getRepositoriesFromRequest_should_return_central_repo_if_header_is_not_found() {

//        GozerServlet servlet = new GozerServlet();

    }

    @Test
    public void getArtifactFromRequest_should_retrieve_artifact_information_from_url() {

        String url = URL;

        Artifact artifact = helper.getArtifactFromRequest(url);

        Artifact expectedArtifact = new DefaultArtifact(GROUP_ID, ARTIFACT_ID, "jar", VERSION);
        assertEquals(expectedArtifact, artifact);

    }

    @Test
    public void should_return_the_central_repository() {
        RemoteRepository central = helper.newCentralRepository();
        assertEquals(central.getContentType(), "default");
        assertEquals(central.getUrl(), "http://repo1.maven.org/maven2/");
        assertEquals(central.getId(), "central");
    }


    @Test
    public void should_build_a_groupId_from_the_parts_of_url() {
        String groupId = helper.buildGroupId(PARTS_OF_URL);

        assertEquals(GROUP_ID, groupId);

    }

    @Test
    public void should_select_the_artifactId_part_of_the_url() {
        String artifactId = helper.buildArtifactId(PARTS_OF_URL);

        assertEquals(ARTIFACT_ID, artifactId);
    }

    @Test
    public void should_select_the_version_part_of_the_url() {
        String version = helper.buildVersion(PARTS_OF_URL);

        assertEquals(VERSION, version);
    }




}
