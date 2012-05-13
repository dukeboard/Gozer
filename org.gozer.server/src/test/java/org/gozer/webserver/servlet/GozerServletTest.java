package org.gozer.webserver.servlet;

import org.gozer.webserver.dependency.DependencyCacheVisitor;
import org.gozer.webserver.dependency.aether.Aether;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.DefaultDependencyNode;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: sebastien
 * Date: 29/03/12
 */
public class GozerServletTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GozerServletTest.class);
    private static final String PATH_INFO = "/org/springframework/spring-core/3.0.5.RELEASE/gozer-metadata.zip";
    private static final String GROUP_ID = "org.springframework";
    private static final String ARTIFACT_ID = "spring-core";
    private static final String VERSION = "3.0.5.RELEASE";
    private static final String EXTENSION = "jar";


    @Test
    public void should_call_DependencyCacheVisitor() throws IOException, ServletException {

        GozerServlet servlet = new GozerServlet();

        DependencyCacheVisitor visitor = mock(DependencyCacheVisitor.class);
        Aether aether = mock(Aether.class);

        servlet.setVisitor(visitor);
        servlet.setAether(aether);


        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        Artifact artifact = new DefaultArtifact(GROUP_ID, ARTIFACT_ID, EXTENSION, VERSION);

        when(request.getPathInfo()).thenReturn(PATH_INFO);
        when(aether.getArtifactFromRequest(PATH_INFO)).thenReturn(artifact);

        List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();

        DefaultDependencyNode node = new DefaultDependencyNode();
        when(aether.getNodeFromCollectRequest(aether.newSession(), new Dependency(artifact, "compile"), repositories)).thenReturn(node);
        Dependency dependency = new Dependency(artifact, "compile");
        when(visitor.getDependencies(false)).thenReturn(new Dependency[]{dependency});

        when(response.getOutputStream()).thenReturn(new FakeOutputStream());
        servlet.service(request, response);


        verify(visitor).visitEnter(node);



    }


    public class FakeOutputStream extends ServletOutputStream {

        public FakeOutputStream() {

        }

        @Override
        public void write(int b) throws IOException {
        }
    }
}
