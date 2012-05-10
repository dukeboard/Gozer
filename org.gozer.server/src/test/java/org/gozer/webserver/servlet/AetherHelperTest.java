package org.gozer.webserver.servlet;

import org.junit.Test;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: Sébastien
 * Date: 29/03/12
 * Time: 01:26
 */
public class AetherHelperTest {


    @Test
    public void getRepositoriesFromRequest_should_return_central_repo_if_header_is_not_found() {

//        GozerServlet servlet = new GozerServlet();

    }

    @Test
    public void getArtifactFromRequest_should_retrieve_artifact_information_from_url() {

        String url = "org/springframework/spring-core/3.0.5.RELEASE/gozer-metadata.zip";

        AetherHelper servlet = new AetherHelper();
        Artifact artifact = servlet.getArtifactFromRequest(url);

        Artifact expectedArtifact = new DefaultArtifact("org.springframework", "spring-core", "jar", "3.0.5.RELEASE");
        assertEquals(expectedArtifact, artifact);

    }

    @Test
    public void should_return_the_central_repository() {
        AetherHelper helper = new AetherHelper();
        RemoteRepository central = helper.newCentralRepository();
        assertEquals(central.getContentType(), "default");
        assertEquals(central.getUrl(), "http://repo1.maven.org/maven2/");
        assertEquals(central.getId(), "central");

    }




}
