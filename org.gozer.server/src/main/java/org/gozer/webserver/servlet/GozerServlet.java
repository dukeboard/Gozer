package org.gozer.webserver.servlet;

import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.gozer.webserver.util.FileNIOHelper;
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
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;
import org.sonatype.aether.util.metadata.DefaultMetadata;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipOutputStream;

import static org.gozer.webserver.servlet.GozerServletHelper.*;
import static org.gozer.webserver.util.FileNIOHelper.createZip;

/**
 * Created by IntelliJ IDEA.
 * User: duke , sebastien
 * Date: 28/03/12
 * Time: 22:01
 */
public class GozerServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(GozerServlet.class);
    private static final String REPOSITORIES = "Repositories";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.info("uri = {}", req.getRequestURI());

//        FileNIOHelper.copyFileToStream(this.getClass().getClassLoader().getResourceAsStream("stub.zip"), resp.getOutputStream());
//        resp.getOutputStream().close();
//        if(true) return ;



        /**
         * 1 - le client demande au serveur pour demander un artefact sur GozerServlet avec un discrimant (classifier ou packaging)
         * 2 - le serveur calcule l'ensemble des dep transitives et les met en cache
         * 3 - le serveur lui renvoie les poms et les hashs dans un zip
         * 4 - le client calcule le diff par rapport à ce qu'il a et ce qu'il veut
         * 5 - le client renvoie au serveur une request avec les dep qu'il veut
         * 6 - le serveur lui renvoie zippé d'un bloc
         */

        GozerServletHelper gozerHelper = new GozerServletHelper();

        RepositorySystem repSys = gozerHelper.newRepositorySystem();

        RepositorySystemSession session = gozerHelper.newSession(repSys);
        Artifact artifact = gozerHelper.getArtifactFromRequest(req.getPathInfo());
        Dependency dependency = new Dependency(artifact, "compile");
        logger.debug("dependency : {}", dependency);
        List<RemoteRepository> repositories = gozerHelper.getRepositoriesFromRequest(req);
        RemoteRepository repo = repositories.get(0);
        logger.debug("repositories : {}", repositories);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.setRepositories(repositories);
        logger.debug("collectRequest : {}", collectRequest);

        DependencyNode node = null;
        try {
            CollectResult collectResult = repSys.collectDependencies(session, collectRequest);
            logger.debug("collectResult : {}",collectResult);
            node = collectResult.getRoot();
            logger.debug("node : {}", node);

        } catch (DependencyCollectionException e) {
            logger.error("Error : ",e);
        }

        DependencyRequest dependencyRequest = new DependencyRequest( node, null );

        try {
            repSys.resolveDependencies( session, dependencyRequest  );
        } catch (DependencyResolutionException e) {
            logger.error("Error : ",e);
        }

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept(nlg);
        logger.info("classpath : {}", nlg.getClassPath());

        OutputStream os = resp.getOutputStream();
        logger.info("dependencies : {}", nlg.getDependencies(false));


        Collection<File> metadataFiles = new ArrayList<File>();
        // create the zip
        ZipOutputStream zipOutputStream = new ZipOutputStream(os);

        for (Dependency dep : nlg.getDependencies(false)) {
            List<MetadataResult> results = null;
            Collection<MetadataRequest> metadataRequests = new ArrayList<MetadataRequest>();
            metadataRequests.add(new MetadataRequest(new DefaultMetadata(dep.getArtifact().getGroupId(), dep.getArtifact().getArtifactId(), "maven-metadata.xml", Metadata.Nature.RELEASE_OR_SNAPSHOT), repo, null));

            results = repSys.resolveMetadata(session, metadataRequests);
            logger.info("metadataResults : {}",results);
            for (MetadataResult result : results) {
                metadataFiles.add(result.getMetadata().getFile());
                createZip("/home/sebastien/.m2/repository/", results, zipOutputStream);

//                FileInputStream fileInputStream = new FileInputStream(result.getMetadata().getFile());
//                FileNIOHelper.copyFileToStream(fileInputStream, os);
            }
        }


//        zipOutputStream.close();



        resp.getOutputStream().println("DaFuck");
        resp.getOutputStream().close();
    }
}
