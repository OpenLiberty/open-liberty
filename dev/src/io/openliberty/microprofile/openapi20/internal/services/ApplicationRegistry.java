package io.openliberty.microprofile.openapi20.internal.services;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;

import io.openliberty.microprofile.openapi20.internal.ApplicationListener;

/**
 * The {@code ApplicationRegistry} maintains a collection of the applications that are deployed to the OpenLiberty
 * instance. It also tracks the application whose OpenAPI document is currently being returned to clients that request
 * it via the /openapi endpoint.
 * <p>
 * OpenAPI documentation is generated for each web module and then merged together if merging is enabled. If merging is not enabled,
 * then documentation is only generated for the first web module found.
 */
public interface ApplicationRegistry {

    /**
     * The addApplication method is invoked by the {@link ApplicationListener} when it is notified that an application
     * is starting. It only needs to process the application if we have not already found an application that implements
     * a JAX-RS based REST API.
     *
     * @param newAppInfo
     *     The ApplicationInfo for the application that is starting.
     */
    void addApplication(ApplicationInfo newAppInfo);

    /**
     * The removeApplication method is invoked by the {@link ApplicationListener} when it is notified that an
     * application is stopping. If the application that is stopping is also the currentApp, we need to iterate over the
     * remaining applications in the collection to find the next one that implements a JAX-RS based REST API, if any.
     *
     * @param removedAppInfo
     *     The ApplicationInfo for the application that is stopping.
     */
    void removeApplication(ApplicationInfo removedAppInfo);

    /**
     * Finds the models from all configured applications, merges them if necessary and returns them.
     *
     * @return an {@code OpenAPIProvider} holding an OpenAPI model or {@code null} if there are no OpenAPI providers
     */
    OpenAPIProvider getOpenAPIProvider();

}