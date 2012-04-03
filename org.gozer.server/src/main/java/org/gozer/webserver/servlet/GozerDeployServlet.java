package org.gozer.webserver.servlet;

import org.gozer.webserver.servlet.api.Authentication;
import org.gozer.webserver.util.FileNIOHelper;

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
 * Time: 18:14
 */
public class GozerDeployServlet extends HttpServlet {

	private File rootRepository = null;
	private Authentication authenticationManager;

	public File getRootRepository () {
		return rootRepository;
	}

	public void setRootRepository (File rootRepository) {
		this.rootRepository = rootRepository;
	}

	public Authentication getAuthenticationManager () {
		return authenticationManager;
	}

	public void setAuthenticationManager (Authentication authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	@Override
	protected void service (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (isAuthenticate(req, resp)) {
			File targetFile = new File(rootRepository, req.getRequestURI());
			FileNIOHelper.createParentDirs(targetFile);
			FileNIOHelper.copyFile(req.getInputStream(), targetFile);
		}
	}

	private boolean isAuthenticate (HttpServletRequest req, HttpServletResponse resp) throws IOException {
		System.out.println("try to check authentication parameters");
		String auth = req.getHeader( "Authorization" );

		if (auth != null) {

			final int index = auth.indexOf(' ');
			if (index > 0) {
				final String[] credentials = new String(Base64.decode(auth.substring(index)), "UTF-8").split(":");
				if (credentials.length == 2 && authenticationManager.authenticate(credentials[0], credentials[1])) {
					return true;
				}
			}
		}
		return false;
	}
}
