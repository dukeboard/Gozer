package org.gozer.webserver.servlet;

import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.ConfigurationProperties;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.async.AsyncRepositoryConnectorFactory;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.impl.internal.EnhancedLocalRepositoryManagerFactory;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 28/03/12
 * Time: 22:01
 */
public class GozerServletHelper extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(GozerServletHelper.class);
    private static final String REPOSITORIES = "Repositories";

    List<RemoteRepository> getRepositoriesFromRequest(HttpServletRequest request) {

        String header = request.getHeader(REPOSITORIES);
        logger.info("Request header : Repositories : {}",header);
        if (header == null) {
            // if no repositories, switch default to central
            return Arrays.asList(new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/"));
        }

        String[] repos = header.split(",");

        List<RemoteRepository> repositories = new ArrayList();
        if (repos.length > 0) {
            for (String repo : repos) {
                String[] repoInfo = repo.split("=");
                String repoId = repoInfo[0];
                String repoURL = repoInfo[1];

                // use a properties file to resolve the gozer server
                if (repoURL.startsWith("gozer:")) {
                    break;
                }
                
                RemoteRepository repository = new RemoteRepository();
                repository.setId(repoId);
                repository.setUrl(repoURL);
                repository.setContentType("default");
                repositories.add(repository);
            }
        }

        return repositories;
    }

    Artifact getArtifactFromRequest(String pathInfo) {
        logger.debug("pathInfo = {}", pathInfo);

        String metadataInfo = pathInfo.substring(0, pathInfo.indexOf("/gozer-metadata.zip"));

        String version = metadataInfo.substring(metadataInfo.lastIndexOf("/")).replaceFirst("/","");
        logger.debug("version = {}", version);

        String metadataInfoWithoutVersion = metadataInfo.substring(0, metadataInfo.indexOf("/"+version));
        logger.debug("metadataInfoWithoutVersion = {}", metadataInfoWithoutVersion);
        String artifactId = metadataInfoWithoutVersion.substring(metadataInfoWithoutVersion.lastIndexOf("/")).replaceFirst("/","");
        logger.debug("artifactId = {}", artifactId);

        String groupId = metadataInfoWithoutVersion.substring(0, metadataInfoWithoutVersion.lastIndexOf("/"));
        groupId = groupId.replaceAll("/", ".");

        logger.debug("groupId = {}", groupId);

        return new DefaultArtifact(groupId, artifactId, "jar", version);
    }

    RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = new DefaultServiceLocator();
//        locator.setService(org.sonatype.aether.spi.log.Logger.class, Slf4jLogger.class);
        locator.setService(LocalRepositoryManagerFactory.class, EnhancedLocalRepositoryManagerFactory.class);
        locator.setService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.setService(RepositoryConnectorFactory.class, AsyncRepositoryConnectorFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    MavenRepositorySystemSession newSession(RepositorySystem system){
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
