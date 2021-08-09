package com.ibm.ws.kernel.osgi.web.embed.manager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * Application Lifecycle Listener implementation class OSGiEmbedManager
 */
public class OSGiEmbedManager implements ServletContextListener {

    private Framework framework;

    /**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent sce) {
        // Start a new framework instance
        FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
        File tempStore = new File((File)sce.getServletContext().getAttribute(ServletContext.TEMPDIR), "frameworkStore");
        Map<String, String> osgiConfig = new HashMap<>();
        osgiConfig.put(Constants.FRAMEWORK_STORAGE, tempStore.getAbsolutePath());
        osgiConfig.put(Constants.FRAMEWORK_STORAGE_CLEAN, "true");
        framework = frameworkFactory.newFramework(osgiConfig);
        try {
            framework.start();
            System.out.println("The embedded framework started!");
        } catch (BundleException e) {
            System.out.println("The embedded framework failed to start!");
            throw new RuntimeException(e);
        }
    }

    /**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent sce) {
         if (framework != null) {
             try {
                framework.stop();
            } catch (BundleException e) {
                throw new RuntimeException(e);
            }
         }
    }
}
