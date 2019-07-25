/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.validator.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONArtifact;
import com.ibm.json.java.JSONObject;
import com.ibm.json.java.OrderedJSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.rest.config.ConfigBasedRESTHandler;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;
import com.ibm.wsspi.validator.Validator;

/**
 * Validates configured resources
 */
@Component(name = "com.ibm.ws.rest.handler.validator",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { RESTHandler.class },
           property = { RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/validation/{element}", RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY + "=true" })
public class ValidatorRESTHandler extends ConfigBasedRESTHandler {
    private static final TraceComponent tc = Tr.register(ValidatorRESTHandler.class);

    private ComponentContext context;

    @Reference
    VariableRegistry variableRegistry;

    private static class HttpErrorInfo {
        private final int code;
        private final String message;

        private HttpErrorInfo(int errorCode, String message) {
            this.code = errorCode;
            this.message = message;
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

    @Override
    public final String getAPIRoot() {
        return "/validation";
    }

    @Override
    public Object handleError(RESTRequest request, String uid, String errorMessage) {
        JSONObject failure = toJSONObject("message", errorMessage);
        if (uid == null)
            return toJSONObject("successful", false, "failure", failure);
        else
            return toJSONObject("uid", uid, "successful", false, "failure", failure);
    }

    @FFDCIgnore(PrivilegedActionException.class)
    public static <S> Collection<ServiceReference<S>> getServiceReferences(final BundleContext bCtx, final Class<S> clazz, final String filter) throws InvalidSyntaxException {
        if (System.getSecurityManager() == null) {
            return bCtx.getServiceReferences(clazz, filter);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Collection<ServiceReference<S>>>() {
                    @Override
                    public Collection<ServiceReference<S>> run() throws InvalidSyntaxException {
                        return bCtx.getServiceReferences(clazz, filter);
                    }
                });
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof InvalidSyntaxException)
                    throw (InvalidSyntaxException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
        }
    }

    @FFDCIgnore(PrivilegedActionException.class)
    public static ServiceReference<?>[] getServiceReferences(final BundleContext bCtx, final String clazz, final String filter) throws InvalidSyntaxException {
        if (System.getSecurityManager() == null) {
            return bCtx.getServiceReferences(clazz, filter);
        } else {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<ServiceReference<?>[]>() {
                    @Override
                    @Trivial
                    public ServiceReference<?>[] run() throws InvalidSyntaxException {
                        return bCtx.getServiceReferences(clazz, filter);
                    }
                });
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof InvalidSyntaxException)
                    throw (InvalidSyntaxException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
        }
    }

    private static <S> S getService(final ComponentContext ctx, final ServiceReference<S> reference) {
        if (System.getSecurityManager() == null) {
            BundleContext bCtx = ctx.getBundleContext();
            return bCtx == null ? null : bCtx.getService(reference);
        } else
            return AccessController.doPrivileged(new PrivilegedAction<S>() {
                @Override
                @Trivial
                public S run() {
                    BundleContext bCtx = ctx.getBundleContext();
                    return bCtx == null ? null : bCtx.getService(reference);
                }
            });
    }

    @Override
    public Object handleSingleInstance(RESTRequest request, String uid, String id, Dictionary<String, Object> config) throws IOException {
        JSONObject json = new OrderedJSONObject();
        json.put("uid", uid);
        if (id != null)
            json.put("id", id);
        String jndiName = (String) config.get("jndiName");
        if (jndiName != null)
            json.put("jndiName", jndiName);
        Object application = config.get("application");
        if (application != null) {
            json.put("application", application);
            Object module = config.get("module");
            if (module != null) {
                json.put("module", module);
                Object component = config.get("component");
                if (component != null)
                    json.put("component", component);
            }
        }

        // Locate the validator
        String configElementPid = (String) config.get("service.factoryPid");
        if (configElementPid == null)
            configElementPid = (String) config.get("service.pid");
        Collection<ServiceReference<Validator>> validatorRefs;
        try {
            String filter = FilterUtils.createPropertyFilter("com.ibm.wsspi.rest.handler.config.pid", configElementPid);
            validatorRefs = getServiceReferences(context.getBundleContext(), Validator.class, filter);
        } catch (InvalidSyntaxException x) {
            validatorRefs = Collections.emptySet(); // same error handling as not found
        }

        // Obtain the instance to validate
        ServiceReference<?>[] targetRefs;
        try {
            String filter = "(|" + FilterUtils.createPropertyFilter("service.pid", (String) config.get("service.pid")) // config without super type
                            + FilterUtils.createPropertyFilter("ibm.extends.subtype.pid", (String) config.get("service.pid")) // config with super type
                            + ")";
            targetRefs = getServiceReferences(context.getBundleContext(), (String) null, filter);
        } catch (InvalidSyntaxException x) {
            targetRefs = null; // same error handling as not found
        }

        // There can be multiple services with the same service.pid!
        // For resource types that are accessible via ResourceFactory, the JNDI/OSGi implementation registers a
        // resource instance reusing the same server.pid that the ResourceFactory has in order to add osgi.jndi.service.name.
        // In this case, we want the resource factory, not the resource instance that is registered with the same service.pid.
        // The resource factory will have a creates.objectClass property to indicate which resource type(s) it creates.
        ServiceReference<?> targetRef = null;
        if (targetRefs != null)
            for (ServiceReference<?> ref : targetRefs) {
                if (targetRef == null || ref.getProperty("creates.objectClass") != null)
                    targetRef = ref;
            }

        Object target = validatorRefs.isEmpty() || targetRef == null ? null : getService(context, targetRef);
        if (target == null) {
            json.put("successful", false);
            json.put("failure", toJSONObject("message", Tr.formatMessage(tc, request.getLocale(), "CWWKO1551_CANNOT_VALIDATE")));
        } else {
            // Build a map of params for the testable service
            Map<String, Object> params = new HashMap<String, Object>();
            boolean headerParamsURLEncoded = false;
            for (String key : request.getParameterMap().keySet())
                if ("headerParamsURLEncoded".equals(key)) {
                    headerParamsURLEncoded = Boolean.parseBoolean(request.getParameter(key));
                } else if (isParameter(key)) {
                    params.put(key, resolvePotentialVariable(request.getParameter(key)));
                } else {
                    return new HttpErrorInfo(400, "unrecognized query parameter: " + key);
                }

            String user = request.getHeader("X-Validation-User");
            if (user != null) {
                if (headerParamsURLEncoded)
                    user = URLDecoder.decode(user, "UTF-8");
                params.put(Validator.USER, resolvePotentialVariable(user));
            }
            String pass = request.getHeader("X-Validation-Password");
            if (pass != null) {
                if (headerParamsURLEncoded)
                    pass = URLDecoder.decode(pass, "UTF-8");
                params.put(Validator.PASSWORD, variableRegistry.resolveRawString(pass));
            }
            String loginConfigProps = request.getHeader("X-Login-Config-Props");
            if (loginConfigProps != null) {
                Map<String, String> lcProps = new TreeMap<String, String>();
                for (String entry : loginConfigProps.split(",")) {
                    int eq = entry.indexOf("=");
                    if (eq > 0) {
                        String name = entry.substring(0, eq);
                        String value = entry.substring(eq + 1);
                        if (headerParamsURLEncoded) {
                            name = URLDecoder.decode(name, "UTF-8");
                            value = URLDecoder.decode(value, "UTF-8");
                        }
                        lcProps.put(resolvePotentialVariable(name), resolvePotentialVariable(value));
                    } else {
                        json.put("successful", false);
                        json.put("failure", toJSONObject("message", Tr.formatMessage(tc, request.getLocale(), "CWWKO1552_MISSING_DELIMITER")));
                    }
                }
                params.put(Validator.LOGIN_CONFIG_PROPS, lcProps);
            }

            Validator validator = getService(context, validatorRefs.iterator().next());
            if (validator == null) {
                json.put("successful", false);
                json.put("failure", toJSONObject("message", Tr.formatMessage(tc, request.getLocale(), "CWWKO1550_VALIDATOR_NOT_FOUND", configElementPid)));
            } else if (!json.containsKey("successful")) {
                Map<String, ?> result;
                try {
                    result = validator.validate(target, params, request.getLocale());
                } catch (Throwable x) {
                    // Shouldn't ever occur, but if it does, report as failure
                    result = Collections.singletonMap("failure", x);
                }

                Object failure = result.get("failure");
                if (failure instanceof Throwable) {
                    json.put("successful", false);
                    json.put("failure", toJSONObjectForThrowable(result, (Throwable) failure));
                } else if (failure instanceof String) {
                    json.put("successful", false);
                    json.put("failure", toJSONObject("message", failure));
                } else {
                    json.put("successful", true);
                    // TODO need to add support for including partial info on failure path
                    JSONObject info = new OrderedJSONObject();
                    for (Map.Entry<String, ?> entry : result.entrySet())
                        info.put(entry.getKey(), entry.getValue());
                    json.put("info", info);
                }
            }
        }
        return json;
    }

    /**
     * Identifies whether the specified query parameter is a valid parameter for the validator.
     * Header parameters such as the user name & password return a false value because they are not query parameters.
     *
     * @param name query parameter name.
     * @return true if a valid parameter for validation. Otherwise false.
     */
    public boolean isParameter(String name) {
        return Validator.AUTH.equals(name)
               || Validator.AUTH_ALIAS.equals(name)
               || Validator.LOGIN_CONFIG.equals(name);
    }

    @Override
    @Trivial
    public void populateResponse(RESTResponse response, Object responseInfo) throws IOException {
        JSONArtifact json;
        if (responseInfo instanceof JSONArtifact)
            json = (JSONArtifact) responseInfo;
        else if (responseInfo instanceof List) {
            JSONArray ja = new JSONArray();
            for (Object info : (List<?>) responseInfo)
                if (info instanceof JSONArtifact) {
                    ja.add(info);
                } else if (info instanceof HttpErrorInfo) {
                    HttpErrorInfo errorInfo = (HttpErrorInfo) info;
                    response.sendError(errorInfo.code, errorInfo.message);
                    return;
                } else {
                    throw new IllegalArgumentException(info.toString()); // should be unreachable
                }
            json = ja;
        } else if (responseInfo instanceof HttpErrorInfo) {
            HttpErrorInfo errorInfo = (HttpErrorInfo) responseInfo;
            response.sendError(errorInfo.code, errorInfo.message);
            return;
        } else {
            throw new IllegalArgumentException(responseInfo.toString()); // should be unreachable
        }

        String jsonString = json.serialize(true);

        /*
         * com.ibm.json.java.JSONArtifact.serialize() escapes / with \\/.
         * The list of special characters in proper JSON data is:
         * \b Backspace (ascii code 08)
         * \f Form feed (ascii code 0C)
         * \n New line
         * \r Carriage return
         * \t Tab
         * \" Double quote
         * \\ Backslash character
         *
         * Therefore, we will remove this extraneous formatting.
         */
        jsonString = jsonString.replaceAll("\\\\/", "/");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "populateResponse", jsonString);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getOutputStream().write(jsonString.getBytes("UTF-8"));
    }

    /**
     * Populates an ordered JSON object with one or more key/value pairs.
     *
     * @param args even number of alternating keys/values.
     * @return ordered JSON object including the specified key/value pairs.
     */
    @Trivial
    private JSONObject toJSONObject(Object... args) {
        OrderedJSONObject json = new OrderedJSONObject();
        for (int i = 0; i < args.length; i += 2)
            json.put(args[i], args[i + 1]);
        return json;
    }

    /**
     * Populate JSON object for a top level exception or error.
     *
     * @param errorInfo additional information to append to exceptions and causes
     * @param error the top level exception or error.
     * @return JSON object representing the Throwable.
     */
    @SuppressWarnings("unchecked")
    @Trivial
    private JSONObject toJSONObjectForThrowable(Map<String, ?> errorInfo, Throwable error) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "toJSONObjectForThrowable", errorInfo, error);

        LinkedHashMap<String, List<?>> addedInfo = new LinkedHashMap<String, List<?>>();
        for (Map.Entry<String, ?> entry : errorInfo.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof List)
                addedInfo.put(entry.getKey(), (List<?>) value);
        }

        JSONObject json = new OrderedJSONObject();

        int index = 0;
        JSONObject current = json;
        Set<Throwable> causes = new HashSet<Throwable>(); // avoid cycles in exception chain
        for (Throwable cause = error; cause != null && causes.add(cause); index++) {
            // custom attributes for position in exception chain (for example, sqlState and errorCode for dataSource)
            for (Map.Entry<String, List<?>> entry : addedInfo.entrySet()) {
                List<?> values = entry.getValue();
                if (values.size() > index) {
                    Object value = values.get(index);
                    if (value != null)
                        current.put(entry.getKey(), value);
                }
            }

            current.put("class", cause.getClass().getName());
            current.put("message", cause.getMessage());

            JSONArray stack = new JSONArray();
            for (StackTraceElement element : cause.getStackTrace())
                stack.add(element.toString());
            current.put("stack", stack);

            if ((cause = cause.getCause()) != null) {
                Map<String, Object> parent = current;
                parent.put("cause", current = new OrderedJSONObject());
            }
        }

        return json;
    }

    @Trivial
    private static String read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }

    @Trivial
    private String resolvePotentialVariable(String value) {
        if (value == null)
            return value;

        String resolvedVariable = variableRegistry.resolveRawString(value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Was a variable value found for " + value + "?  " + !value.equals(resolvedVariable));
        }
        return resolvedVariable;
    }

    /**
     * Restricts use of the validation end-point to GET requests only.
     * All other requests will respond with a 405 - method not allowed error.
     *
     * {@inheritDoc}
     */
    @Override
    public final void handleRequest(RESTRequest request, RESTResponse response) throws IOException {
        if (!"GET".equals(request.getMethod())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Request method was " + request.getMethod() + " but the validation endpoint is restricted to GET requests only.");
            }
            response.setResponseHeader("Accept", "GET");
            response.sendError(405); // Method Not Allowed
            return;
        }

        //Throw 404 for /ibm/api/validation with an empty string element.
        if (request.getPath().startsWith("/validation//")) {
            response.sendError(404, Tr.formatMessage(tc, request.getLocale(), "CWWKO1553_HANDLER_NOT_FOUND", request.getContextPath() + request.getPath()));
            return;
        }

        super.handleRequest(request, response);
    }

    @Override
    public boolean requireAdministratorRole() {
        return true;
    }
}
