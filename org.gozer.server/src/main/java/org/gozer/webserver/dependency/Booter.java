package org.gozer.webserver.dependency;

import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;

public class Booter {

    public static RepositorySystem newRepositorySystem() {
        return ManualRepositorySystemFactory.newRepositorySystem();
    }

    public static RepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));

        session.setTransferListener(new ConsoleTransferListener());
        session.setRepositoryListener(new ConsoleRepositoryListener());

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }

    public static RemoteRepository newCentralRepository() {
        return new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
    }

}