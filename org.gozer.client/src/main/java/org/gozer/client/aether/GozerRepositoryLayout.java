package org.gozer.client.aether;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.util.layout.MavenDefaultLayout;
import org.sonatype.aether.util.layout.RepositoryLayout;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 29/03/12
 * Time: 03:14
 */
public class GozerRepositoryLayout implements RepositoryLayout {

    private RepositoryLayout layout = new MavenDefaultLayout();

    @Override
    public URI getPath(Artifact artifact) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public URI getPath(Metadata metadata) {
        String uri = layout.getPath(metadata).toString();
        String resource = uri.replace("maven-metadata", "gozer-metadata"); //Hack
        resource = resource.substring(0, resource.length() - 4) + ".zip"; //Hack
        try {
            return new URI(resource);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }

    }
}
