package io.openliberty.microprofile.openapi20;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.servers.Server;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;

import io.openliberty.microprofile.openapi20.utils.OpenAPIUtils;
import io.smallrye.openapi.runtime.io.Format;

public class WebModuleOpenAPIProvider implements OpenAPIProvider {

    // The WebModuleInfo for the application/module that the OpenAPI model was generated from
    private final WebModuleInfo webModuleInfo;
    
    // The OpenAPI model
    private final OpenAPI openAPIModel;
    
    // Flag that indicates whether the model defined any servers at the point it was generated
    private boolean serversDefined = false;
    
    // Cache of OpenAPI documents to return when server injection is not required
    private Map<Format, String> openAPIDocuments = new HashMap<>();

    /**
     * Constructor
     * 
     * @param webModuleInfo
     *          The WebModuleInfo for the application/module that the OpenAPI model was generated from
     * @param openAPIModel
     *          The OpenAPI model itself
     * @param serversDefined
     *          Flag that indicates whether the model defined any servers at the point it was generated
     */
    public WebModuleOpenAPIProvider(final WebModuleInfo webModuleInfo, final OpenAPI openAPIModel, final boolean serversDefined) {
        this.webModuleInfo = webModuleInfo;
        this.openAPIModel = openAPIModel;
        this.serversDefined = serversDefined;

        /*
         * If the model that has been generated already contains server definitions, we will trust that the user knows
         * what they are doing and will not modify the model in any way when the OpenAPI document is requested. In this
         * scenario, we can generate both the YAML and JSON formats of the document now to improve runtime performance. 
         */
        if (this.serversDefined) {
            openAPIDocuments.put(Format.YAML, OpenAPIUtils.getOpenAPIDocument(openAPIModel, Format.YAML));
            openAPIDocuments.put(Format.JSON, OpenAPIUtils.getOpenAPIDocument(openAPIModel, Format.JSON));
        }
    }

    /**
     * @see io.openliberty.microprofile.openapi20.OpenAPIProvider#getOpenAPIDocument(Format)
     */
    @Override
    public String getOpenAPIDocument(Format format) {
        // Create the variable to return
        String document = null;
        
        if (openAPIModel != null) {
            // Check to see if we can return the cached OpenAPI document
            if (serversDefined) {
                // Return the cached OpenAPI document
                document = openAPIDocuments.get(format);
            } else {
                // We are not caching the OpenAPI document... generate it now
                synchronized (openAPIModel) {
                    document = OpenAPIUtils.getOpenAPIDocument(openAPIModel, format);
                }
            }
        }
        
        return document;
    }

    /**
     * @see io.openliberty.microprofile.openapi20.OpenAPIProvider#getOpenAPIDocument(List<Server>, Format)
     */
    @Override
    public String getOpenAPIDocument(List<Server> servers, Format format) {
        // Create the variable to return
        String document = null;

        // Make sure that the list of servers is valid
        if (servers != null && !servers.isEmpty()) {
            /*
             * We only store a single copy of the OpenAPI model for a given application, but the OpenAPI document could
             * be requested by multiple clients concurrently.  As a result, we need to synchronize operations that
             * update the model to ensure consistent behaviour. 
             */
            synchronized (openAPIModel) {
                // Set the servers in the OpenAPI model and generate the OpenAPI document
                openAPIModel.setServers(servers);
                document = OpenAPIUtils.getOpenAPIDocument(openAPIModel, format);
                openAPIModel.setServers(null);
            }
        }
        
        return document;
    }

    
    /**
     * @see io.openliberty.microprofile.openapi20.OpenAPIProvider#getContextRoot()
     */
    @Override
    public String getContextRoot() {
        return webModuleInfo.getContextRoot();
    }
    
    /**
     * @see io.openliberty.microprofile.openapi20.OpenAPIProvider#getApplicationPath()
     */
    @Override
    public String getApplicationPath() {
        // Create the variable to return
        String applicationPath = null;

        /*
         * Now check if the model contains any paths and, if so, whether the first one starts with the context root.
         * If it does not, we use the context root as the application path and add it to the URL of the server. 
         */
        final String contextRoot = webModuleInfo.getContextRoot();
        Paths paths = openAPIModel.getPaths();
        if (  paths == null
           || paths.getPathItems() == null
           || paths.getPathItems().isEmpty()
           || !paths.getPathItems().keySet().iterator().next().startsWith(contextRoot)
           ) {
            applicationPath = contextRoot;
        }
        
        return applicationPath;
    }

    /**
     * @see io.openliberty.microprofile.openapi20.OpenAPIProvider#getServersDefined()
     */
    @Override
    public boolean getServersDefined() {
        return serversDefined;
    }
}
