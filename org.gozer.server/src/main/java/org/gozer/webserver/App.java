package org.gozer.webserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 28/03/12
 * Time: 20:45
 */
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {

        String userDir = System.getProperty("user.home");
        File userConfigPath = new File(userDir + File.separator + "gozer/repository");
        GozerWebServer webServer = new GozerWebServer(userConfigPath);
        Thread serverThread = new Thread(webServer);
        serverThread.start();

        logger.info("Gozer server started / Root dir : "+userConfigPath.getAbsolutePath());

    }
}
