package com.ibm.ws.springboot.support.version15.test.webanno.app;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class TestWebListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent e) {
		// nothing
	}

	@Override
	public void contextInitialized(ServletContextEvent e) {
		e.getServletContext().setAttribute(TestApplication.TEST_ATTR, "PASSED");
	}

}
