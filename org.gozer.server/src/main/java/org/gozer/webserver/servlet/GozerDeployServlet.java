package org.gozer.webserver.servlet;

import org.gozer.webserver.util.FileNIOHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/04/12
 * Time: 18:14
 */
public class GozerDeployServlet extends HttpServlet {

    private File rootRepository = null;

    public File getRootRepository() {
        return rootRepository;
    }

    public void setRootRepository(File rootRepository) {
        this.rootRepository = rootRepository;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        File targetFile = new File(rootRepository,req.getRequestURI());
        FileNIOHelper.createParentDirs(targetFile);
        FileNIOHelper.copyFile(req.getInputStream(),targetFile);

    }
}
