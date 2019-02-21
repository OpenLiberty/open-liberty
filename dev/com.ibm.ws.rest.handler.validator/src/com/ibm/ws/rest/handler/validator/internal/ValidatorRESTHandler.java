/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.validator.internal;

import java.io.IOException;
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

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
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
           property = { "com.ibm.wsspi.rest.handler.context.root=/ibm/api", "com.ibm.wsspi.rest.handler.root=/validator" }) // TODO switch to /openapi/platform
public class ValidatorRESTHandler extends ConfigBasedRESTHandler {
    private static final TraceComponent tc = Tr.register(ValidatorRESTHandler.class);

    @Reference
    private ConfigurationAdmin configAdmin;

    private ComponentContext context;

    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        this.context = null;
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
        // TODO: app-defined data sources: application/module/component

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
            String filter = FilterUtils.createPropertyFilter("service.pid", (String) config.get("service.pid"));
            targetRefs = getServiceReferences(context.getBundleContext(), (String) null, filter);
        } catch (InvalidSyntaxException x) {
            targetRefs = null; // same error handling as not found
        }

        Object target = validatorRefs.isEmpty() || targetRefs == null ? null : getService(context, targetRefs[0]);
        if (target == null) {
            json.put("successful", false);
            json.put("failure",
                     toJSONObject("message",
                                  "One or more dependencies not satisfied, or feature that enables the resource is not enabled, or it is not possible to validate this type of resource"));
        } else {
            // Build a map of params for the testable service
            Map<String, Object> params = new HashMap<String, Object>();
            String user = request.getHeader("X-Validator-User");
            if (user != null)
                params.put("user", user);
            String pass = request.getHeader("X-Validator-Password");
            if (pass != null)
                params.put("password", pass);
            for (String key : request.getParameterMap().keySet()) {
                params.put(key, request.getParameter(key)); // TODO only add valid parameters (auth, authData)? And if we want any validation of values, this is the central place for it
            }

            Validator validator = getService(context, validatorRefs.iterator().next());
            if (validator == null) {
                json.put("successful", false);
                json.put("failure", toJSONObject("message", "Unable to obtain validator for " + configElementPid));
            } else {
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

    @Override
    public void populateResponse(RESTResponse response, Object responseInfo) throws IOException {
        JSONArtifact json;
        if (responseInfo instanceof JSONArtifact)
            json = (JSONArtifact) responseInfo;
        else if (responseInfo instanceof List) {
            JSONArray ja = new JSONArray();
            for (Object info : (List<?>) responseInfo)
                if (info instanceof JSONArtifact)
                    ja.add(info);
                else
                    throw new IllegalArgumentException(info.toString()); // should be unreachable
            json = ja;
        } else
            throw new IllegalArgumentException(responseInfo.toString()); // should be unreachable

        String jsonString = json.serialize(true);
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
}