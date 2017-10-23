package com.ibm.ws.jpa;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jpa22injection.web.JPAInjectionTestServlet;

@RunWith(FATRunner.class)
public class JPA22Injection extends FATServletClient {
	public static final String APP_NAME = "jpa22injection";
    public static final String SERVLET = "TestJPA22Injection";
    
    @Server("JPA22InjectionServer")
    @TestServlet(servlet = JPAInjectionTestServlet.class, path = APP_NAME + "/" + SERVLET)
    public static LibertyServer server1;
    
    @BeforeClass
    public static void setUp() throws Exception {
    		ShrinkHelper.defaultApp(server1, APP_NAME, "jpa22injection.web", "jpa22injection.entity");
    		server1.startServer();
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer();
    }
}
