package org.gozer.webserver.servlet;

import org.gozer.webserver.util.ZipHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.*;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;
import org.sonatype.aether.util.metadata.DefaultMetadata;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: duke , sebastien
 * Date: 28/03/12
 * Time: 22:01
 */
public class GozerServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(GozerServlet.class);
    private static final String REPOSITORIES = "Repositories";
    public static final String COMPILE = "compile";
    public static final String MAVEN_METADATA_XML = "maven-metadata.xml";
    private final AetherHelper aetherHelper;
    private final ZipHelper zipHelper;

    public GozerServlet() {
        aetherHelper = new AetherHelper();
        zipHelper = new ZipHelper();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGGER.info("uri = {}", req.getRequestURI());

        /**
         * 1 - le client demande au serveur pour demander un artefact sur GozerServlet avec un discrimant (classifier ou packaging)
         * 2 - le serveur calcule l'ensemble des dep transitives et les met en cache
         * 3 - le serveur lui renvoie les poms et les hashs dans un zip
         * 4 - le client calcule le diff par rapport à ce qu'il a et ce qu'il veut
         * 5 - le client renvoie au serveur une request avec les dep qu'il veut
         * 6 - le serveur lui renvoie zippé d'un bloc
         */

        RepositorySystem repSys = aetherHelper.newRepositorySystem();

        RepositorySystemSession session = aetherHelper.newSession(repSys);
        Artifact artifact = aetherHelper.getArtifactFromRequest(req.getPathInfo());
        Dependency dependency = new Dependency(artifact, COMPILE); // TODO handle scope
        LOGGER.debug("dependency : {}", dependency);

        List<RemoteRepository> repositories = aetherHelper.readRepositoriesFromRequest(req);



        CollectRequest collectRequest = buildCollectRequest(dependency, repositories);

        DependencyNode node = getNodeFromCollectRequest(repSys, session, collectRequest);

        DependencyResult dependencyResult = resolveDependencies(repSys, session, node);


        OutputStream os = resp.getOutputStream();

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept(nlg);
        LOGGER.info("classpath : {}", nlg.getClassPath());

        LOGGER.info("dependencies : {}", nlg.getDependencies(false));

        for (Dependency dep : nlg.getDependencies(false)) {
            List<MetadataResult> results = resolveMetadata(repSys, session, repositories, dep);
            sendZipOfMetadataIntoStream(os, results);
        }


//        zipOutputStream.close();



        resp.getOutputStream().close();
    }

    void sendZipOfMetadataIntoStream(OutputStream os, List<MetadataResult> results) {
//        Collection<File> metadataFiles = new ArrayList<File>();

        for (MetadataResult result : results) {
//            metadataFiles.add(result.getMetadata().getFile());
            zipHelper.init(os);
            zipHelper.createZipFromMetadatas(results);

//                FileInputStream fileInputStream = new FileInputStream(result.getMetadata().getFile());
//                FileNIOHelper.copyFileToStream(fileInputStream, os);
        }
    }

    List<MetadataResult> resolveMetadata(RepositorySystem repSys, RepositorySystemSession session, List<RemoteRepository> repositories, Dependency dep) {
        List<MetadataResult> results = null;
        Collection<MetadataRequest> metadataRequests = new ArrayList<MetadataRequest>();
        DefaultMetadata defaultMetadata = new DefaultMetadata(dep.getArtifact().getGroupId(), dep.getArtifact().getArtifactId(), MAVEN_METADATA_XML, Metadata.Nature.RELEASE_OR_SNAPSHOT);
        metadataRequests.add(new MetadataRequest(defaultMetadata, repositories.get(0), null)); //TODO create a request for each repo ?

        results = repSys.resolveMetadata(session, metadataRequests);
        LOGGER.info("metadataResults : {}", results);
        return results;
    }

    DependencyResult resolveDependencies(RepositorySystem repSys, RepositorySystemSession session, DependencyNode node) {
        DependencyRequest dependencyRequest = new DependencyRequest(node, null);
        DependencyResult dependencyResult = null;

        try {
            dependencyResult = repSys.resolveDependencies(session, dependencyRequest);
        } catch (DependencyResolutionException e) {
            LOGGER.error("Error : ", e);
        }
        LOGGER.info("artifact result : {}", dependencyResult.getArtifactResults());
        return dependencyResult;
    }

    DependencyNode getNodeFromCollectRequest(RepositorySystem repSys, RepositorySystemSession session, CollectRequest collectRequest) {
        DependencyNode node = null;
        try {
            CollectResult collectResult = repSys.collectDependencies(session, collectRequest);
            LOGGER.debug("collectResult : {}", collectResult);
            node = collectResult.getRoot();
            LOGGER.debug("node : {}", node);

        } catch (DependencyCollectionException e) {
            LOGGER.error("Error : ", e);
        }
        return node;
    }

    CollectRequest buildCollectRequest(Dependency dependency, List<RemoteRepository> repositories) {
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.setRepositories(repositories);
        LOGGER.debug("collectRequest : {}", collectRequest);
        return collectRequest;
    }
}
