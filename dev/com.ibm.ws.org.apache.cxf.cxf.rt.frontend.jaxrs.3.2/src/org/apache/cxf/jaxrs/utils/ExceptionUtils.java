/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxrs.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public final class ExceptionUtils {
    private static final String PROPAGATE_EXCEPTION = "org.apache.cxf.propagate.exception";
    private static final String  SUPPORT_WAE_SPEC_OPTIMIZATION = "support.wae.spec.optimization";

    private ExceptionUtils() {
    }

    public static String getStackTrace(Throwable ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static Class<?> getWebApplicationExceptionClass(Response exResponse,
                                                           Class<?> defaultExceptionType) {
        return SpecExceptions.getWebApplicationExceptionClass(exResponse, defaultExceptionType);
    }


    public static boolean propogateException(Message m) {

        Object value = m.getContextualProperty(PROPAGATE_EXCEPTION);

        if (value == null) {
            return true;
        }

        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(value.toString());
    }


    @FFDCIgnore(Throwable.class)
    public static <T extends Throwable> Response convertFaultToResponse(T ex, Message currentMessage) {
        if (ex == null || currentMessage == null) {
            return null;
        }
        Message inMessage = currentMessage.getExchange().getInMessage();
        Response response = null;
        if (ex instanceof WebApplicationException) {
            WebApplicationException webEx = (WebApplicationException)ex;
            if (webEx.getResponse().hasEntity()
                && webEx.getCause() == null
                && MessageUtils.getContextualBoolean(inMessage, SUPPORT_WAE_SPEC_OPTIMIZATION, true)) {
                response = webEx.getResponse();
            }
        }

        if (response == null) {
            ExceptionMapper<T>  mapper =
                ServerProviderFactory.getInstance(inMessage).createExceptionMapper(ex.getClass(), inMessage);
            if (mapper != null) {
                try {
                    response = mapper.toResponse(ex);
                } catch (Throwable mapperEx) {
                    inMessage.getExchange().put(JAXRSUtils.EXCEPTION_FROM_MAPPER, "true");
                    mapperEx.printStackTrace();
                    return Response.serverError().build();
                }
            }
        }
        if (response == null) {
            Throwable unwrappedException = ex.getCause();
            if (unwrappedException instanceof WebApplicationException) {
                WebApplicationException webEx = (WebApplicationException)unwrappedException;
                response = webEx.getResponse();
            }
        }
        JAXRSUtils.setMessageContentType(currentMessage, response);
        return response;
    }

    public static WebApplicationException toWebApplicationException(Throwable cause, Response response) {
        return new WebApplicationException(cause, response);
    }

    //TODO: we can simply use the reflection, investigate

    @FFDCIgnore(NoClassDefFoundError.class)
    public static WebApplicationException toInternalServerErrorException(Throwable cause, Response response) {
        try {
            return SpecExceptions.toInternalServerErrorException(cause, response);
        } catch (NoClassDefFoundError ex) {
            return toWebApplicationException(ex, response);
        }
    }

    @FFDCIgnore(NoClassDefFoundError.class)
    public static WebApplicationException toBadRequestException(Throwable cause, Response response) {
        try {
            return SpecExceptions.toBadRequestException(cause, response);
        } catch (NoClassDefFoundError ex) {
            return toWebApplicationException(ex, response);
        }
    }

    @FFDCIgnore(NoClassDefFoundError.class)
    public static WebApplicationException toNotFoundException(Throwable cause, Response response) {
        try {
            return SpecExceptions.toNotFoundException(cause, response);
        } catch (NoClassDefFoundError ex) {
            return toWebApplicationException(ex, response);
        }
    }

    @FFDCIgnore(NoClassDefFoundError.class)
    public static WebApplicationException toNotAuthorizedException(Throwable cause, Response response) {
        try {
            return SpecExceptions.toNotAuthorizedException(cause, response);
        } catch (NoClassDefFoundError ex) {
            return toWebApplicationException(ex, response);
        }
    }

    @FFDCIgnore(NoClassDefFoundError.class)
    public static WebApplicationException toForbiddenException(Throwable cause, Response response) {
        try {
            return SpecExceptions.toForbiddenException(cause, response);
        } catch (NoClassDefFoundError ex) {
            return toWebApplicationException(ex, response);
        }
    }

    @FFDCIgnore(NoClassDefFoundError.class)
    public static WebApplicationException toNotAcceptableException(Throwable cause, Response response) {
        try {
            return SpecExceptions.toNotAcceptableException(cause, response);
        } catch (NoClassDefFoundError ex) {
            return toWebApplicationException(ex, response);
        }
    }

    @FFDCIgnore(NoClassDefFoundError.class)
    public static WebApplicationException toNotSupportedException(Throwable cause, Response response) {
        try {
            return SpecExceptions.toNotSupportedException(cause, response);
        } catch (NoClassDefFoundError ex) {
            return toWebApplicationException(ex, response);
        }
    }

    @FFDCIgnore(NoClassDefFoundError.class)
    public static WebApplicationException toHttpException(Throwable cause, Response response) {
        try {
            return SpecExceptions.toHttpException(cause, response);
        } catch (NoClassDefFoundError ex) {
            return toWebApplicationException(ex, response);
        }
    }
}
