package org.gozer.client.aether;

import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 28/03/12
 * Time: 22:31
 */
public class AetherStubRequest {

    public static void main(String[] args) throws ArtifactResolutionException {

        PlexusStub stub = new PlexusStub();
        RepositorySystem repSys = stub.getNewRepositorySystem();

        Artifact artifact = new DefaultArtifact("org.kevoree:org.kevoree.model:1.6.0-SNAPSHOT");
        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(artifact);
        java.util.List<RemoteRepository> repositories = new java.util.ArrayList();
        RemoteRepository repo = new RemoteRepository();
        repo.setId("kevGozer");
        repo.setUrl("gozer:http://localhost:8080/");
        repo.setContentType("default");
        repositories.add(repo);
        artifactRequest.setRepositories(repositories);
        ArtifactResult artefactResult = repSys.resolveArtifact(stub.getNewRepositorySystemSession(repSys), artifactRequest);



    }

}
