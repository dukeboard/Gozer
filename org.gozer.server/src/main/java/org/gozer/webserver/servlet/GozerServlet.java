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
public class GozerServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(GozerServlet.class);
    private static final String REPOSITORIES = "Repositories";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.info(req.getRequestURI());

        /**
         * 1 - le client demande au serveur pour demander un artefact sur GozerServlet avec un discrimant (classifier ou packaging)
         * 2 - le serveur calcule l'ensemble des dep transitives et les met en cache
         * 3 - le serveur lui renvoie les poms et les hashs dans un zip
         * 4 - le client calcule le diff par rapport à ce qu'il a et ce qu'il veut
         * 5 - le client renvoie au serveur une request avec les dep qu'il veut
         * 6 - le serveur lui renvoie zippé d'un bloc
         */

        RepositorySystem repSys = newRepositorySystem();

//        Artifact artifact = getArtifactFromRequest(req.getPathInfo());
//
//        ArtifactRequest artifactRequest = new ArtifactRequest();
//        artifactRequest.setArtifact(artifact);
//        artifactRequest.setRepositories(getRepositoriesFromRequest(req));
//
//        logger.info("Resolving artifact");
//        try {
//            ArtifactResult artefactResult = repSys.resolveArtifact(newSession(repSys), artifactRequest);
//        } catch (ArtifactResolutionException e) {
//            logger.error("Error :", e);
//        }

        RepositorySystemSession session = newSession(repSys);
//
        Dependency dependency = new Dependency( new DefaultArtifact( "org.apache.maven:maven-profile:2.2.1" ), "compile" );
        RemoteRepository central = new RemoteRepository( "central", "default", "http://repo1.maven.org/maven2/" );
//
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.addRepository(central);
        collectRequest.setRepositories(getRepositoriesFromRequest(req));
        DependencyNode node = null;
        try {
            node = repSys.collectDependencies(session, collectRequest).getRoot();
        } catch (DependencyCollectionException e) {
            e.printStackTrace();
        }
//
//        DependencyRequest dependencyRequest = new DependencyRequest( node, null );
//
//        try {
//            repoSystem.resolveDependencies(session, dependencyRequest);
//        } catch (DependencyResolutionException e) {
//            e.printStackTrace();
//        }
//
        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept(nlg);
        logger.info(nlg.getClassPath());

        resp.getOutputStream().println("DaFuck");

    }

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
        return new DefaultArtifact("org.springframework", "spring-core", "jar", "3.0.5.RELEASE");
    }

    public RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = new DefaultServiceLocator();
//        locator.setService(org.sonatype.aether.spi.log.Logger.class, Slf4jLogger.class);
        locator.setService(LocalRepositoryManagerFactory.class, EnhancedLocalRepositoryManagerFactory.class);
        locator.setService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.setService(RepositoryConnectorFactory.class, AsyncRepositoryConnectorFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    public MavenRepositorySystemSession newSession(RepositorySystem system){
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
