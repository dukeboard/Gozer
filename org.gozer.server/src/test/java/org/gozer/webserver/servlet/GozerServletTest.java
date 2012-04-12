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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: Sébastien
 * Date: 29/03/12
 * Time: 01:26
 */
public class GozerServletTest {

    private static final Logger logger = LoggerFactory.getLogger(GozerServletTest.class);

    private GozerInternalServer srv;
    private Thread serverThread;

    @Before
    public void startServer() {
        srv = new GozerInternalServer();
        // setting aliases, for an optional file servlet
        Acme.Serve.Serve.PathTreeDictionary aliases = new Acme.Serve.Serve.PathTreeDictionary();
        srv.setMappingTable(aliases);
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
                logger.error("Method failed: " + method.getStatusLine());
            }

            // Read the response body.
            InputStream responseBodyStream = method.getResponseBodyAsStream();

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
//            logger.info("response : {}", convertStreamToString(responseBodyStream));

            File file = new File("gozer-metadata.zip");
            FileWriter writer = new FileWriter(file);
            writer.write(convertStreamToString(responseBodyStream));
            writer.close();

        } catch (HttpException e) {
            logger.error("Fatal protocol violation: ", e);
        } catch (IOException e) {
            logger.error("Fatal transport error: ", e);
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
    }

    private String convertStreamToString(InputStream is) throws IOException {
        /*
         * To convert the InputStream to String we use the
         * Reader.read(char[] buffer) method. We iterate until the
         * Reader return -1 which means there's no more data to
         * read. We use the StringWriter class to produce the string.
         */
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }


}
