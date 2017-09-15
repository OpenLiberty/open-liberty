/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.websocket.DecodeException;
import javax.websocket.DeploymentException;
import javax.websocket.server.PathParam;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.wsoc.impl.MethodDataImpl;
import com.ibm.ws.wsoc.impl.PathParamDataImpl;

/**
 *
 */
public class EndpointMethodHelper implements Cloneable {

    private static final TraceComponent tc = Tr.register(EndpointMethodHelper.class);

    Method method = null;
    Class<?> inputParameterType;
    MethodData methodData = null;
    DecodeException decodeException = null;
    String endpointPath = null;

    public EndpointMethodHelper(Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getInputParameterType() {
        return inputParameterType;
    }

    public HashMap<Integer, PathParamData> getPathParams() {
        return this.methodData.getPathParams();
    }

    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }

    public void setMethodData(MethodData methodData) {
        this.methodData = methodData;
    }

    public MethodData getMethodData() {
        return this.methodData;
    }

    /**
     * introspectPathParams
     * 
     * @param method
     */
    public void introspectPathParams() throws DeploymentException {

        //Section 4.3 JSR 356 - The allowed types for the @PathParam parameters are String, any Java primitive type, or boxed version thereof.
        Class<?>[] pathParamTypesAllowed = { String.class, char.class, Character.class, byte.class, Byte.class, short.class, Short.class, int.class, Integer.class, long.class,
                                            Long.class, float.class, Float.class, double.class, Double.class, boolean.class, Boolean.class };
        List<Class<?>> pathParamTypeList = Arrays.asList(pathParamTypesAllowed);
        Class<?>[] paramTypes = method.getParameterTypes();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        boolean uriSegmentMatched = true;

        for (int i = 0; i < paramAnnotations.length; i++) {
            for (Annotation annotation : paramAnnotations[i]) {
                if (annotation.annotationType() == javax.websocket.server.PathParam.class) {
                    if (pathParamTypeList.contains(paramTypes[i])) {
                        String pathParamValue = ((PathParam) annotation).value();
                        if (pathParamValue != null && !(pathParamValue.isEmpty())) {
                            //check if user has defined @PathParam variable on a method,but there is no corresponding matching path segment in endpoint path
                            //for e.g user might have defined "@PathParam("Boolean-var") Boolean BooleanVar" on onOpen() and the server endpoint path
                            //is /somePath/{Integer-var}/{Long-var}
                            List<String> segments = Arrays.asList(endpointPath.split("/"));
                            if (!(segments.contains("{" + pathParamValue + "}")) && !(segments.contains(pathParamValue))) {
                                //error case: @PathParam does not match any path segment in the uri
                                if (tc.isDebugEnabled()) {
                                    String msg = "@PathParam parameter " + pathParamValue + " defined on the method " + method.getName()
                                                 + " does not have corresponding path segment in @ServerEndpoint URI in Annotated endpoint " + method.getDeclaringClass().getName();
                                    Tr.debug(tc, msg);
                                }
                                //set the parameter to null. This is needed for String type of param.
                                uriSegmentMatched = false;
                            }
                            //create PathParamData with annotation, the parameter index which has @PathParam declared, type of the parameter. 
                            PathParamData paramData = new PathParamDataImpl(pathParamValue, i, paramTypes[i], uriSegmentMatched);
                            //use key for the hashmap as parameter index which has @PathParam declared, so that we can retrieve the correct PathParamData
                            //during parameter value substitution
                            methodData.getPathParams().put(Integer.valueOf(i), paramData);
                        }
                        else {
                            //section 4.3 jsr 356. The value attribute of this annotation must be present otherwise the implementation must throw an  error. [WSC-4.3-2]
                            String msg = Tr.formatMessage(tc,
                                                          "missing.pathparam.value",
                                                          ((PathParam) annotation).value(), method.getName(), method.getDeclaringClass().getName());
                            Tr.error(tc,
                                     "missing.pathparam.value",
                                     ((PathParam) annotation).value(), method.getName(), method.getDeclaringClass().getName());
                            throw new DeploymentException(msg);
                        }
                    } else {
                        //section 4.3 jsr 356. "The allowed types for the @PathParam parameters are String, any Java primitive type, or boxed version thereof. Any other type 
                        //annotated with this annotation is an error that the implementation must report at deployment time. [WSC-4.3-1]"
                        String msg = Tr.formatMessage(tc,
                                                      "invalid.pathparam.type",
                                                      ((PathParam) annotation).value(), method.getName(), method.getDeclaringClass().getName());
                        Tr.error(tc,
                                 "invalid.pathparam.type",
                                 ((PathParam) annotation).value(), method.getName(), method.getDeclaringClass().getName());
                        throw new DeploymentException(msg);
                    }
                }
            }
        }
    }

    /**
     * @param _m
     */
    public void processPathParameters(AnnotatedEndpoint annotatedEP, Object args[]) throws DecodeException {
        if (this.getPathParams().isEmpty()) {
            return;
        }
        //endpoint path. e.g /bookings/{guest-id}
        String[] endpointPathParts = annotatedEP.getEndpointPath().split("/");
        //incoming request uri path. e.g /bookings/JohnDoe
        String[] requestPathParts = annotatedEP.getRequestPath().split("/");

        HashMap<Integer, PathParamData> pathParams = this.getPathParams();
        Iterator<Entry<Integer, PathParamData>> iterator = pathParams.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, PathParamData> mapEntry = iterator.next();
            //index of the method argument which has @Pathparam defined
            Integer pathParamIndex = mapEntry.getKey();
            PathParamData pathParamData = mapEntry.getValue();
            //value of the @PathParam annotation. e.g guest-id
            String pathParamValue = pathParamData.getAnnotationValue();

            if (!pathParamData.isURISegmentMatched()) {
                //Question what about other data types?
                if (pathParamData.getParamType().equals(String.class)) {
                    //@PathParam parameter doesn't have a matching URI path segment and this is a String type parameter
                    //[WSC-4.3-3] Otherwise, the value of the String parameter annotated by this annotation must be set to null by the
                    //implementation.
                    convertStringToParamType(args, pathParamIndex, null, pathParamData.getParamType());
                } else { //for all other types, raise decodeException calling onError() method
                    /*
                     * JSR 356 @PathParam section
                     * "If the container cannot decode the path segment appropriately to the annotated path parameter, then the container must raise an DecodeException to the error
                     * handling method of the websocket containing the path segment. [WSC-4.3-6]"
                     */
                    String msg = Tr.formatMessage(tc,
                                                  "missing.path.segment",
                                                  pathParamValue, method.getName(), method.getDeclaringClass().getName());
                    Tr.error(tc,
                             "missing.path.segment",
                             pathParamValue, method.getName(), method.getDeclaringClass().getName());
                    decodeException = new DecodeException(pathParamValue, msg);
                }
            } else {
                // Performance: improve performance by shifting this loop to introspection (deployment) time and keep track of matched path segment index in PathParamData
                for (int i = 1; i < endpointPathParts.length; i++) { // start with i=1, skipping the first part because first part of split("/") for /../../.. is an empty string
                    if (endpointPathParts[i].equals("{" + pathParamValue + "}") || endpointPathParts[i].equals(pathParamValue)) {
                        //found the match. Now find the value in the incoming requestPathParts, covert it to correct parameter type and set it as a value at the correct parameter 
                        //position for the method argument which is the args[]
                        convertStringToParamType(args, pathParamIndex, requestPathParts[i], pathParamData.getParamType());
                    }
                }
            }
        }
        //error processing @PathParam parameter
        if (decodeException != null) {
            throw decodeException;
        }
    }

    /*
     * JSR 356, Section 4.3 @PathParam
     * "if the parameter is a Java primitive type or boxed version thereof, the container must use the path segment
     * string to construct the type with the same result as if it had used the public one argument String constructor
     * to obtain the boxed type, and reduced to its primitive type if necessary. [WSC-4.3-5]"
     */
    /**
     * @param object
     * @param pathParamIndex
     * @param string
     * @param pathParamData
     */
    private void convertStringToParamType(Object args[], Integer pathParamIndex, String paramValue, Class<?> pathParamData) {
        try {
            if (pathParamData.equals(String.class)) {
                args[pathParamIndex] = paramValue;
            } else if (pathParamData.equals(Character.class) || pathParamData.equals(char.class)) {
                //it has to be just one character
                args[pathParamIndex] = Character.valueOf(paramValue.toCharArray()[0]);
            } else if (pathParamData.equals(Integer.class) || pathParamData.equals(int.class)) {
                args[pathParamIndex] = Integer.valueOf(paramValue);
            } else if (pathParamData.equals(Byte.class) || pathParamData.equals(byte.class)) {
                args[pathParamIndex] = Byte.valueOf(paramValue);
            } else if (pathParamData.equals(Short.class) || pathParamData.equals(short.class)) {
                args[pathParamIndex] = Short.valueOf(paramValue);
            } else if (pathParamData.equals(Long.class) || pathParamData.equals(long.class)) {
                args[pathParamIndex] = Long.valueOf(paramValue);
            } else if (pathParamData.equals(Double.class) || pathParamData.equals(double.class)) {
                args[pathParamIndex] = Double.valueOf(paramValue);
            } else if (pathParamData.equals(Boolean.class) || pathParamData.equals(boolean.class)) {
                args[pathParamIndex] = Boolean.valueOf(paramValue);
            } else if (pathParamData.equals(Float.class) || pathParamData.equals(float.class)) {
                args[pathParamIndex] = Float.valueOf(paramValue);
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "convertStringToParamType", this);

            //if the @PathParam declared on method onError() is a primitive type, then set default value as -1 and false for boolean
            //because in-order to call onError method, we need to set all argument values and null default will not work if parameter 
            //type is primitive. See AnnotatedEndpoint.onError() method to find out how this usecase can happen.
            if (method.getName().equals("onError")) {
                if (pathParamData.equals(int.class) || pathParamData.equals(short.class) || pathParamData.equals(double.class)
                    || pathParamData.equals(short.class) || pathParamData.equals(long.class)) {
                    args[pathParamIndex] = -1;
                } else if (pathParamData.equals(boolean.class)) {
                    args[pathParamIndex] = false;
                }
            }
            /*
             * JSR 356 @PathParam section
             * "If the container cannot decode the path segment appropriately to the annotated path parameter, then the container must raise an DecodeException to the error
             * handling method of the websocket containing the path segment. [WSC-4.3-6]"
             */
            String msg = Tr.formatMessage(tc,
                                          "mismatch.pathparam.type",
                                          method.getName(), method.getDeclaringClass().getName(), pathParamIndex, pathParamData.getName());
            Tr.error(tc,
                     "mismatch.pathparam.type",
                     method.getName(), method.getDeclaringClass().getName(), pathParamIndex, pathParamData.getName());
            decodeException = new DecodeException(paramValue, msg, e);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "@PathParam index " + pathParamIndex + " @Pathparam type " + pathParamData.getName());
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        EndpointMethodHelper clone = (EndpointMethodHelper) super.clone();
        clone.methodData = (MethodData) ((MethodDataImpl) this.methodData).clone();
        return clone;
    }
}
