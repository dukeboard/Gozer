package org.gozer.client.aether;

import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.sonatype.aether.ConfigurationProperties;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.connector.async.AsyncRepositoryConnectorFactory;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.impl.internal.EnhancedLocalRepositoryManagerFactory;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.LocalRepository;

import java.io.File;


/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 28/03/12
 * Time: 22:14
 */
public class PlexusStub {

    public RepositorySystem getNewRepositorySystem() {
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.setServices(Logger.class, new AetherSlf4jLogger());
        locator.setService(LocalRepositoryManagerFactory.class, EnhancedLocalRepositoryManagerFactory.class);
        locator.setService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.setService(RepositoryConnectorFactory.class, AsyncRepositoryConnectorFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    public MavenRepositorySystemSession getNewRepositorySystemSession(RepositorySystem system){
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
        session.setConfigProperty("aether.connector.ahc.provider", "jdk");
        //DEFAULT VALUE
        //TODO CHEKK THIS HACK
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(new LocalRepository(System.getProperty("user.home").toString() + "/.m2/repository")));
        //TRY TO FOUND MAVEN CONFIGURATION
        File configFile = new File(System.getProperty("user.home").toString() + File.separator + ".m2" + File.separator + "settings.xml");
        session.getConfigProperties().put(ConfigurationProperties.REQUEST_TIMEOUT, 2000);
        session.getConfigProperties().put(ConfigurationProperties.CONNECT_TIMEOUT, 1000);
        return session;
      }


}
