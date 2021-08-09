/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.rest.handler.validator.openapi20;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;
import com.ibm.wsspi.validator.Validator;

import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;

/**
 * Displays validation schema
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = { RESTHandler.class }, property = { RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT
                                                                                                           + "=/openapi/platform",
                                                                                                           RESTHandler.PROPERTY_REST_HANDLER_ROOT
                                                                                                                                   + "=/validation" })
public class ValidatorSchemaRESTHandler implements RESTHandler {
    private static final TraceComponent tc = Tr.register(ValidatorSchemaRESTHandler.class);

    private ComponentContext context;

    /**
     * Restricts use of the validation schema end-point to GET requests only.
     * All other requests will respond with a 405 - method not allowed error.
     *
     * {@inheritDoc}
     */
    @Override
    public final void handleRequest(RESTRequest request, RESTResponse response) throws IOException {
        if (!"GET".equals(request.getMethod())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Request method was " + request.getMethod() + " but the validation schema endpoint is restricted to GET requests only.");
            }
            response.setResponseHeader("Accept", "GET");
            response.sendError(405); // Method Not Allowed
            return;
        }

        OpenAPI openAPI = getOpenAPIDocument(response);

        if (openAPI != null) {
            removeDisabledValidators(openAPI);

            String acceptHeader = request.getHeader("Accept");
            String format = "yaml";
            if ((acceptHeader != null) && acceptHeader.equals("application/json")) {
                format = "json";
            }
            String formatParam = request.getParameter("format");
            if ((formatParam != null) && formatParam.equals("json")) {
                format = "json";
            }

            if (format.equals("json")) {
                response.setContentType("application/json");
                response.getWriter().write(OpenApiSerializer.serialize(openAPI, Format.JSON));
            } else {
                response.setContentType("text/plain");
                response.getWriter().write(OpenApiSerializer.serialize(openAPI, Format.YAML));
            }
        }
    }

    private OpenAPI getOpenAPIDocument(RESTResponse response) {
        OpenAPI openAPI = null;
        InputStream inputStream = ValidatorSchemaRESTHandler.class.getResourceAsStream("/META-INF/openapi.yaml");
        if (inputStream != null) {
            OpenApiStaticFile staticFile = new OpenApiStaticFile(inputStream, Format.YAML);
            openAPI = OpenApiProcessor.modelFromStaticFile(staticFile);
            response.setCharacterEncoding("UTF-8");
            if (openAPI == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Error retrieving openapi.yaml for config validation. Returning error code 500.");
                }
                response.setStatus(500);
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Null inputStream for openapi.yaml. Return 500.");
            }
            response.setStatus(500);
        }
        return openAPI;
    }

    private void removeDisabledValidators(OpenAPI openAPI) {
        boolean cloudantEnabled = false, jcaEnabled = false, jmsEnabled = false, jdbcEnabled = false;
        try {
            cloudantEnabled = getServiceReferences(context.getBundleContext(), Validator.class,
                                                   "(component.name=com.ibm.ws.rest.handler.validator.cloudant.CloudantDatabaseValidator)")
                                                                                                                                           .iterator()
                                                                                                                                           .hasNext();
            jcaEnabled = getServiceReferences(context.getBundleContext(), Validator.class,
                                              "(component.name=com.ibm.ws.rest.handler.validator.jca.ConnectionFactoryValidator)")
                                                                                                                                  .iterator()
                                                                                                                                  .hasNext();
            jdbcEnabled = getServiceReferences(context.getBundleContext(), Validator.class,
                                               "(component.name=com.ibm.ws.rest.handler.validator.jdbc.DataSourceValidator)")
                                                                                                                             .iterator()
                                                                                                                             .hasNext();
        } catch (InvalidSyntaxException e) {
            //Should never happen.
            e.printStackTrace();
        }

        if (jcaEnabled) {
            jmsEnabled = isJMSValidatorEnabled();
        }

        if (!cloudantEnabled) {
            openAPI.getPaths().removePathItem("/validation/cloudantDatabase/");
            openAPI.getPaths().removePathItem("/validation/cloudantDatabase/{uid}");
            openAPI.getComponents().removeSchema("validation.cloudantDatabase.result");
        }
        if (!jcaEnabled) {
            openAPI.getPaths().removePathItem("/validation/connectionFactory/");
            openAPI.getPaths().removePathItem("/validation/connectionFactory/{uid}");
            openAPI.getComponents().removeSchema("validation.connectionFactory.result");
        }
        if (!jmsEnabled) {
            openAPI.getPaths().removePathItem("/validation/jmsConnectionFactory/");
            openAPI.getPaths().removePathItem("/validation/jmsConnectionFactory/{uid}");
            openAPI.getPaths().removePathItem("/validation/jmsQueueConnectionFactory/");
            openAPI.getPaths().removePathItem("/validation/jmsQueueConnectionFactory/{uid}");
            openAPI.getPaths().removePathItem("/validation/jmsTopicConnectionFactory/");
            openAPI.getPaths().removePathItem("/validation/jmsTopicConnectionFactory/{uid}");
            openAPI.getComponents().removeSchema("validation.jms.result");
        }
        if (!jdbcEnabled) {
            openAPI.getPaths().removePathItem("/validation/dataSource/");
            openAPI.getPaths().removePathItem("/validation/dataSource/{uid}");
            openAPI.getComponents().removeSchema("validation.dataSource.result");
        }
    }

    @FFDCIgnore(PrivilegedActionException.class)
    public static <S> Collection<ServiceReference<S>> getServiceReferences(final BundleContext bCtx, final Class<S> clazz, final String filter) throws InvalidSyntaxException {
        if (System.getSecurityManager() == null) {
            return bCtx.getServiceReferences(clazz, filter);
        } else {
            try {
                return AccessController.doPrivileged((PrivilegedExceptionAction<Collection<ServiceReference<S>>>) () -> bCtx.getServiceReferences(clazz, filter));
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof InvalidSyntaxException) {
                    throw (InvalidSyntaxException) e.getCause();
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        this.context = null;
    }

    private boolean isJMSValidatorEnabled() {

        Class<?> jmsClass = AccessController.doPrivileged((PrivilegedAction<Class<?>>) () -> {
            try {
                return getClass().getClassLoader().loadClass("javax.jms.ConnectionFactory");
            } catch (ClassNotFoundException e) {
                return null;
            }
        });
        return jmsClass != null;
    }
}
