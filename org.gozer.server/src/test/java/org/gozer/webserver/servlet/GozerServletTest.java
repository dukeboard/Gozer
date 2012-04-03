package org.gozer.webserver.servlet;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.gozer.webserver.GozerInternalServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: Sébastien
 * Date: 29/03/12
 * Time: 01:26
 */
public class GozerServletTest {

    private GozerInternalServer srv;
    private Thread serverThread;

    @Before
    public void startServer() {
        srv = new GozerInternalServer();
        // setting aliases, for an optional file servlet
        Acme.Serve.Serve.PathTreeDictionary aliases = new Acme.Serve.Serve.PathTreeDictionary();
        srv.setMappingTable(aliases);
//        // setting properties for the server, and exchangeable Acceptors
//        java.util.Properties properties = new java.util.Properties();
//        properties.put("port", 80);
//        properties.setProperty(Acme.Serve.Serve.ARG_NOHUP, "nohup");
//        srv.arguments = properties;
        srv.addServlet("/gozer/*", new GozerServlet()); // optional

        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                srv.serve();
            }
        });
        serverThread.start();
    }

    @After
    public void stopServer() {

        srv.notifyStop();
        srv.destroyAllServlets();

        serverThread.stop();
    }

    @Test
    public void service() {
        String url = "http://localhost:8080/gozer/org/springframework/spring-core/3.0.5.RELEASE/gozer-metadata.zip";
        // Create an instance of HttpClient.
        HttpClient client = new HttpClient();

        // Create a method instance.
        GetMethod method = new GetMethod(url);

        try {
            // Execute the method.
            int statusCode = client.executeMethod(method);





            if (statusCode != HttpStatus.SC_OK) {
                System.err.println("Method failed: " + method.getStatusLine());
            }

            // Read the response body.
            byte[] responseBody = method.getResponseBody();

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            System.out.println(new String(responseBody));

        } catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
    }

}
