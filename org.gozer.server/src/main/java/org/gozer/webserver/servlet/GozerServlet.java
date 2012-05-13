package org.gozer.webserver.servlet;

import org.gozer.webserver.cache.DependencyCache;
import org.gozer.webserver.dependency.DependencyCacheVisitor;
import org.gozer.webserver.dependency.aether.Aether;
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
import org.sonatype.aether.util.metadata.DefaultMetadata;

import javax.annotation.PostConstruct;
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
    private Aether aether;
    private ZipHelper zipHelper;

    private DependencyCache cache = null;
    private DependencyCacheVisitor visitor;

    @PostConstruct
    public void init() {
        aether = new Aether();
        zipHelper = new ZipHelper();
        cache = DependencyCache.getInstance();
        visitor = new DependencyCacheVisitor();
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

        RepositorySystemSession session = aether.newSession();
        Artifact artifact = aether.getArtifactFromRequest(req.getPathInfo());
        Dependency dependency = new Dependency(artifact, COMPILE); // TODO handle scope
        LOGGER.debug("dependency : {}", dependency);

        List<RemoteRepository> repositories = aether.readRepositoriesFromRequest(req);

        CollectRequest collectRequest = aether.buildCollectRequest(dependency, repositories);

        DependencyNode node = aether.getNodeFromCollectRequest(session, collectRequest);


        // TODO should we use this or a visitor, check performance...
        DependencyResult dependencyResult = aether.resolveDependencies(session, node); // TODO is never used

//        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
//        node.accept(nlg);
//        LOGGER.debug("classpath : {}", nlg.getClassPath());
//
//        LOGGER.debug("dependencies : {}", nlg.getDependencies(false));

        node.accept(visitor);

        OutputStream os = resp.getOutputStream();
        for (Dependency dep : visitor.getDependencies(false)) {
            List<MetadataResult> results = aether.resolveMetadata(session, repositories, dep);
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

    public void setCache(DependencyCache cache) {
        this.cache = cache;
    }

    public void setVisitor(DependencyCacheVisitor visitor) {
        this.visitor = visitor;
    }


    public void setAether(Aether aether) {
        this.aether = aether;
    }
}
