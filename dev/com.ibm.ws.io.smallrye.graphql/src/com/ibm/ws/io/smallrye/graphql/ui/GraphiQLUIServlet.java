package com.ibm.ws.io.smallrye.graphql.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GraphiQLUIServlet extends HttpServlet {
	private static final long serialVersionUID = 333566789521908L;
	private static final String DEFAULT_HTML_PAGE = "/graphiql.html";
	private static final ClassLoader THIS_CLASSLOADER = AccessController.doPrivileged((PrivilegedAction<ClassLoader>)
			() -> GraphiQLUIServlet.class.getClassLoader());
	private static final String PACKAGE_NAME = "META-INF/ui";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		String path = req.getPathInfo();
		if (path == null || path.equals("")) {
			path = DEFAULT_HTML_PAGE;
		}
		InputStream is = THIS_CLASSLOADER.getResourceAsStream(PACKAGE_NAME + path);
		if (is == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		try {
		    OutputStream os = resp.getOutputStream();
		    byte[] b = new byte[2048];
		    int read = is.read(b);
            while (read > -1) {
                os.write(b, 0, read);
                read = is.read(b);
            }
		} finally {
			try {
			    is.close();
			} catch (IOException ex) {
				// AutoFFDC
			}
		}
	}
}
