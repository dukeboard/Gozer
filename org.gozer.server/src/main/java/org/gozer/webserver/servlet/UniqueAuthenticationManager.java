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

	private final String user = "toto";
	private final String password = "titi";

	@Override
	public boolean authenticate (String login, String credentials) {
		System.out.println("toto is the user and titi is the password");
		return user.equals(login) && password.equals(credentials);
	}
}
