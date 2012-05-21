package org.gozer.webserver.dependency.aether;

import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.omg.CORBA.PUBLIC_MEMBER;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.ConfigurationProperties;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.async.AsyncRepositoryConnectorFactory;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.impl.internal.EnhancedLocalRepositoryManagerFactory;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.*;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.metadata.DefaultMetadata;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: duke, sebastien
 * Date: 28/03/12
 */
public class Aether {

    private static final Logger LOGGER = LoggerFactory.getLogger(Aether.class);
    private static final String REPOSITORIES = "Repositories";
    public static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    private RepositorySystem repositorySystem;


    public Aether() {
        init();
    }

    public void init() {
        repositorySystem = newRepositorySystem();
    }


    public List<RemoteRepository> readRepositoriesFromRequest(HttpServletRequest request) {

        String header = request.getHeader(REPOSITORIES);
        LOGGER.info("Request header : Repositories : {}", header);
        if (header == null) {
            // if no repositories, switch default to central
            return Arrays.asList(newCentralRepository());
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
        LOGGER.debug("repositories : {}", repositories);
        return repositories;
    }

    RemoteRepository newCentralRepository() {
        return new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
    }

    public Artifact getArtifactFromRequest(String pathInfo) {
        LOGGER.debug("pathInfo = {}", pathInfo);

        String[] partsOfUrl = pathInfo.split("/");

        String version = buildVersion(partsOfUrl);
        String artifactId = buildArtifactId(partsOfUrl);
        String groupId = buildGroupId(partsOfUrl);

        LOGGER.debug("artifact = {}:{}:{}", new String[]{groupId, artifactId, version});

        return new DefaultArtifact(groupId, artifactId, "jar", version);
    }

    String buildVersion(String[] partsOfUrl) {
        return partsOfUrl[partsOfUrl.length - 2];
    }

    String buildArtifactId(String[] partsOfUrl) {
        return partsOfUrl[partsOfUrl.length - 3];
    }

    String buildGroupId(String[] partsOfUrl) {
        int i = 0;
        StringBuilder groupIdBuilder = new StringBuilder(partsOfUrl[i++]);
        while (i <= partsOfUrl.length - 4) {
            groupIdBuilder.append('.').append(partsOfUrl[i++]);
        }

        return groupIdBuilder.toString();
    }

    RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.setService(LocalRepositoryManagerFactory.class, EnhancedLocalRepositoryManagerFactory.class);
        locator.setService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.setService(RepositoryConnectorFactory.class, AsyncRepositoryConnectorFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    public RepositorySystem getRepositorySystem() {
        return repositorySystem;
    }


    public MavenRepositorySystemSession newSession() {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
        session.setConfigProperty("aether.connector.ahc.provider", "jdk");
        //TODO check this hack
        LocalRepository localRepository = new LocalRepository(defaultLocalRepository());
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(localRepository));
        //TODO try to found maven configuration
        File configFile = new File(defaultUserSettings());    // TODO is never used
        session.getConfigProperties().put(ConfigurationProperties.REQUEST_TIMEOUT, 2000);
        session.getConfigProperties().put(ConfigurationProperties.CONNECT_TIMEOUT, 1000);

        return session;
    }

    String defaultUserSettings() {
        return defaultLocalRepository() + "settings.xml";
    }

    String defaultLocalRepository() {
        return System.getProperty("user.home") + File.separator +".m2"+ File.separator +"repository";
    }

    public DependencyResult resolveDependencies(RepositorySystemSession session, DependencyNode node) {
        DependencyRequest dependencyRequest = new DependencyRequest(node, null);
        DependencyResult dependencyResult = null;

        try {
            dependencyResult = repositorySystem.resolveDependencies(session, dependencyRequest);
        } catch (DependencyResolutionException e) {
            LOGGER.error("Error : ", e);
        }
        LOGGER.info("artifact result : {}", dependencyResult.getArtifactResults());
        return dependencyResult;
    }

    public List<MetadataResult> resolveMetadata(RepositorySystemSession session, List<RemoteRepository> repositories, Dependency dep) {
        List<MetadataResult> results = null;
        Collection<MetadataRequest> metadataRequests = new ArrayList<MetadataRequest>();
        DefaultMetadata defaultMetadata = new DefaultMetadata(dep.getArtifact().getGroupId(), dep.getArtifact().getArtifactId(), MAVEN_METADATA_XML, Metadata.Nature.RELEASE_OR_SNAPSHOT);
        metadataRequests.add(new MetadataRequest(defaultMetadata, repositories.get(0), null)); //TODO create a request for each repo ?

        results = repositorySystem.resolveMetadata(session, metadataRequests);
        LOGGER.info("metadataResults : {}", results);
        return results;
    }

    CollectRequest buildCollectRequest(Dependency dependency, List<RemoteRepository> repositories) {
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.setRepositories(repositories);
        LOGGER.debug("collectRequest : {}", collectRequest);
        return collectRequest;
    }

    public DependencyNode getNodeFromCollectRequest(RepositorySystemSession session, Dependency dependency, List<RemoteRepository> repositories) {
        CollectRequest collectRequest = buildCollectRequest(dependency, repositories);

        DependencyNode node = null;
        try {
            CollectResult collectResult = repositorySystem.collectDependencies(session, collectRequest);
            LOGGER.debug("collectResult : {}", collectResult);
            node = collectResult.getRoot();
            LOGGER.debug("node : {}", node);

        } catch (DependencyCollectionException e) {
            LOGGER.error("Error : ", e);
        }
        return node;
    }

}
