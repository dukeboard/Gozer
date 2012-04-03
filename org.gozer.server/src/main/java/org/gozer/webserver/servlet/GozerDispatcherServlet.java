package org.gozer.webserver.servlet;

import Acme.Serve.FileServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/04/12
 * Time: 18:04
 */
public class GozerDispatcherServlet extends HttpServlet {

    private HttpServlet fileServlet = null;
    private HttpServlet gozerServlet = null;
    private GozerDeployServlet gozerDeployServlet = null;

    public GozerDispatcherServlet(File rootM2) {
        fileServlet = new FileServlet();
        gozerServlet = new GozerServlet();
        gozerDeployServlet = new GozerDeployServlet();
        gozerDeployServlet.setRootRepository(rootM2);
		gozerDeployServlet.setAuthenticationManager(new UniqueAuthenticationManager());
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        fileServlet.init(config);
        gozerServlet.init(config);
        gozerDeployServlet.init(config);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getMethod().equals("GET") && req.getRequestURI().contains("gozer-metadata.zip")) {
            gozerServlet.service(req, resp);
            return;
        }
        if (req.getMethod().equals("PUT")) {
            gozerDeployServlet.service(req, resp);
            return;
        }
        fileServlet.service(req, resp);
    }
}
