/*
 * This program may be used, executed, copied, modified and distributed
 * without royalty for the purpose of developing, using, marketing, or distributing.
 */

package com.ibm.ws.logging.hpel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.websphere.logging.hpel.LogRecordContext;

/**
 * This servlet filter adds configured request query parameters to the
 * LogRecordContext
 *
 * Sample content to add to web.xml deployment descriptor to configure this
 * servlet filter to add the userName and productId request query parameters to
 * the LogRecordContext:
 *
 * <filter id="Filter_1">
 * <filter-name>LogDecoratorFilter</filter-name>
 * <filter-class>com.test.LogDecoratorFilter</filter-class>
 * <init-param>
 * <param-name>IncludeParameter</param-name>
 * <param-value>userName productId</param-value>
 * </init-param>
 * </filter>
 * <filter-mapping>
 * <filter-name>LogDecoratorFilter</filter-name>
 * <url-pattern>/*</url-pattern>
 * </filter-mapping>
 *
 */
public class LogDecoratorFilter implements Filter {

    private static Logger logger = Logger.getLogger("com.ibm.ws.test.LogDecoratorFilter");

    private ThreadLocalStringExtension[] extensions;
    private String[] extensionNames;

    /**
     * Default constructor.
     */
    public LogDecoratorFilter() {
    }

    /**
     * @see Filter#destroy()
     */
    @Override
    public void destroy() {
        logger.info("LogDecoratorFilter - destroy");
        for (String extensionName : extensionNames) {
            LogRecordContext.unregisterExtension(extensionName);
        }
    }

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        logger.info("LogDecoratorFilter - doFilter");

        // set values we want to log
        for (int i = 0; i < extensionNames.length; i++) {
            String value = servletRequest.getParameter(extensionNames[i]);
            extensions[i].setValue(value);
            logger.info(extensionNames[i] + " set to: " + value);
        }

        // pass the request along the filter chain
        filterChain.doFilter(servletRequest, servletResponse);

        // unset the values so they don't leak to other threads
        for (int i = 0; i < extensionNames.length; i++) {
            extensions[i].setValue(null);
            logger.info(extensionNames[i] + " set to: " + null);
        }
    }

    /**
     * @see Filter#init(FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("LogDecoratorFilter - init");

        // get names from deployment descriptor
        List<String> names = getIncludeParameters(filterConfig);

        // set up an extension for each name
        extensionNames = new String[names.size()];
        extensions = new ThreadLocalStringExtension[names.size()];
        Map<String, String> extns = new HashMap<String, String>();
        for (int i = 0; i < names.size(); i++) {
            extensionNames[i] = names.get(i);
            extensions[i] = new ThreadLocalStringExtension();
            LogRecordContext.registerExtension(extensionNames[i], extensions[i]);
            LogRecordContext.getExtensions(extns);
            logger.info("LogRecordContextExtensions ::" + extns);
        }
        logger.info("LogDecoratorFilter - init -Ends");
    }

    private List<String> getIncludeParameters(FilterConfig filterConfig) {

        List<String> names = new ArrayList<String>();

        {
            Enumeration<String> initParameterNames = filterConfig.getInitParameterNames();
            while (initParameterNames.hasMoreElements()) {
                String name = initParameterNames.nextElement();
                if (!name.equals("IncludeParameter")) {
                    logger.info("unrecognized <param-name> [" + name + "].  Only IncludeParameter accepted");
                } else {
                    String combinedValue = filterConfig.getInitParameter(name);
                    String[] splitValue = combinedValue.split(" ");
                    for (String value : splitValue) {
                        logger.info("init parm: " + value);
                        names.add(value);
                    }
                }
            }
        }

        return names;
    }
}
