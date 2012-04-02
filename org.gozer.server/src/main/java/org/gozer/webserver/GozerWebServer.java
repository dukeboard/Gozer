package org.gozer.webserver;

import Acme.Serve.FileServlet;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 28/03/12
 * Time: 20:49
 */
public class GozerWebServer implements Runnable {

    File rootFileSystem = null;

    public GozerWebServer(File rootFileSystem) {
        this.rootFileSystem = rootFileSystem;
        srv = new GozerInternalServer();
        java.util.Properties properties = new java.util.Properties();
        properties.put("port", 8080);
        properties.setProperty(Acme.Serve.Serve.ARG_NOHUP, "nohup");
        srv.arguments = properties;

/*
        Acme.Serve.Serve.PathTreeDictionary aliases = new Acme.Serve.Serve.PathTreeDictionary();
        aliases.put("/*", new java.io.File(rootFileSystem.getAbsolutePath()));
        srv.setMappingTable(aliases);
        srv.addDefaultServlets(null);
*/
        srv.addDefaultServlets(null);

        FileServlet fservlet = new FileServlet();
        srv.addServlet("/files/*",fservlet);

        Acme.Serve.Serve.PathTreeDictionary aliases = new Acme.Serve.Serve.PathTreeDictionary();
        aliases.put("/*", new java.io.File(rootFileSystem.getAbsolutePath()));
        srv.setMappingTable(aliases);



        GozerServlet gozerServlet = new GozerServlet();
        srv.addServlet("/gozer/*",gozerServlet);


        SpringServlet jerseyServlet = new SpringServlet();

        Hashtable initParam = new Hashtable();
        initParam.put("com.sun.jersey.api.json.POJOMappingFeature", "true");

        srv.addServlet("/json/*", jerseyServlet, initParam, "true");


        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                srv.notifyStop();
                srv.destroyAllServlets();
            }
        }));
    }

    private GozerInternalServer srv = null;

    class GozerInternalServer extends Acme.Serve.Serve {
        public void setMappingTable(PathTreeDictionary mappingtable) {
            super.setMappingTable(mappingtable);
        }
    }

    public void run() {
        srv.serve();
    }

}
