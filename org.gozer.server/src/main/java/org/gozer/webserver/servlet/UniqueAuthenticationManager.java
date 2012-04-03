package org.gozer.webserver.servlet;

import org.gozer.webserver.servlet.api.Authentication;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 03/04/12
 * Time: 23:48
 *
 * @author Erwan Daubert
 * @version 1.0
 */
public class UniqueAuthenticationManager implements Authentication {

	private final String user = "gozer";
	private final String password = "gozer";

	@Override
	public boolean authenticate (String login, String credentials) {
		return user.equals(login) && password.equals(credentials);
	}
}
