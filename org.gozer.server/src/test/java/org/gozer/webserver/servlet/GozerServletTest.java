package org.gozer.webserver.servlet;

import org.gozer.webserver.dependency.DependencyCacheVisitor;
import org.gozer.webserver.dependency.aether.Aether;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: Sébastien
 * Date: 29/03/12
 * Time: 01:26
 */
public class GozerServletTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GozerServletTest.class);
    private static final String PATH_INFO = "/org/springframework/spring-core/3.0.5.RELEASE/gozer-metadata.zip";


    @Test
    public void should_call_DependencyCacheVisitor() throws IOException, ServletException {

        GozerServlet servlet = new GozerServlet();

        DependencyCacheVisitor visitor = mock(DependencyCacheVisitor.class);
        Aether aether = mock(Aether.class);

        servlet.setVisitor(visitor);
        servlet.setAether(aether);


        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);


        when(request.getPathInfo()).thenReturn(PATH_INFO);


        servlet.service(request, response);


        verify(visitor).visitEnter(null);



    }
}
