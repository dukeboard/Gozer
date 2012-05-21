package org.gozer.webserver.dependency;

import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * Created with IntelliJ IDEA.
 * User: sebastien
 * Date: 10/05/12
 * Time: 13:35.
 */
public class DependenciesResolver {

    void getDependencyTree() throws DependencyCollectionException {
        System.out.println( "------------------------------------------------------------" );
        System.out.println( DependenciesResolver.class.getSimpleName() );

        RepositorySystem system = Booter.newRepositorySystem();

        RepositorySystemSession session = Booter.newRepositorySystemSession(system);

        Artifact artifact = new DefaultArtifact( "org.apache.maven:maven-aether-provider:3.0.2" );

        RemoteRepository repo = Booter.newCentralRepository();

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( new Dependency( artifact, "" ) );
        collectRequest.addRepository( repo );

        CollectResult collectResult = system.collectDependencies( session, collectRequest );

        collectResult.getRoot().accept( new DependencyLogger() );
    }

    public static void main(String... args) {
        DependenciesResolver resolver = new DependenciesResolver();
        try {
            resolver.getDependencyTree();
        } catch (DependencyCollectionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

}
