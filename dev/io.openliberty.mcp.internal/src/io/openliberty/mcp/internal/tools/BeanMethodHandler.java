/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.tools;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.internal.McpServlet.ToolArgumentsImpl;
import io.openliberty.mcp.internal.ToolMetadata.SpecialArgumentMetadata;
import io.openliberty.mcp.internal.encoders.EncoderRegistry;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.tools.ToolManager.ToolArguments;
import io.openliberty.mcp.messaging.Encoder;
import io.openliberty.mcp.tools.ToolCallException;
import io.openliberty.mcp.tools.ToolResponse;
import io.openliberty.mcp.tools.ToolResponseEncoder;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.json.bind.Jsonb;

/**
 * Methods and data common to both {@link SyncBeanMethodHandler} and {@link AsyncBeanMethodHandler}
 *
 * @param <RESPONSE> the handler response type
 */
public abstract class BeanMethodHandler<RESPONSE> implements Function<ToolArguments, RESPONSE> {

    private static final TraceComponent tc = Tr.register(BeanMethodHandler.class);

    private Jsonb jsonb;
    protected final BeanManager bm;
    protected final MethodMetadata method;

    /**
     * The metadata needed to invoke a tool declared as a bean method
     *
     * @param name the tool name
     * @param bean the bean where the tool is defined
     * @param method the method which implements the tool
     * @param isStructuredContent whether the tool returns structured content
     * @param businessExceptions list of business exception types
     * @param specialArguments special arguments required by the methods
     * @param argNames an array corresponding to the method arguments. Each element contains either a name, in which case the argument with that name should be passed to the
     *     parameter with the same index, or {@code null} in which case the parameter at that index expects a special argument.
     * @param genericMap where the tool has being concretized from a generic parent class the concrete types are not reflected on the method so a mapping is provided instead
     *
     */
    public static record MethodMetadata(String name,
                                        Bean<?> bean,
                                        Method method,
                                        boolean isStructuredContent,
                                        List<Class<? extends Throwable>> businessExceptions,
                                        List<SpecialArgumentMetadata> specialArguments,
                                        String[] argNames,
                                        Map<TypeVariable<?>, Type> genericMap) {}

    /**
     * @param jsonb the Jsonb to use to encode a structured response
     * @param bm the bean manager to use to look up the bean
     * @param method metadata about the method to call
     */
    public BeanMethodHandler(Jsonb jsonb, BeanManager bm, MethodMetadata method) {
        super();
        this.jsonb = jsonb;
        this.bm = bm;
        this.method = method;
    }

    /** {@inheritDoc} */
    @Override
    public abstract RESPONSE apply(ToolArguments t);

    protected Object[] constructArgsArray(ToolArguments t) {
        Object[] argsArray = new Object[method.argNames().length];
        int i = 0;
        for (String name : method.argNames()) {
            if (name != null) {
                argsArray[i] = t.args().get(name);
            }
            i++;
        }

        for (SpecialArgumentMetadata specArg : method.specialArguments()) {
            argsArray[specArg.index()] = switch (specArg.typeResolution().specialArgsType()) {
                case CANCELLATION -> t.cancellation();
                case META -> t.meta();
                case REQUEST_ID -> t.requestId();
                default -> throw new RuntimeException("Unknown arg"); //TODO FIX - possibly we can guarantee this is validated earlier
            };
        }
        return argsArray;
    }

    protected ToolResponse createSuccessfulResponse(Object result, ToolArguments toolArgs) {
        // Map method response to a ToolResponse
        if (result instanceof ToolResponse response) {
            return response;
        } else if (result instanceof List<?> list && !list.isEmpty() && list.stream().allMatch(item -> item instanceof Content)) {
            @SuppressWarnings("unchecked")
            List<Content> contents = (List<Content>) list;
            return ToolResponse.success(contents);
        } else if (result instanceof Content content) {
            return ToolResponse.success(content);
        } else if (result instanceof String s) {
            return ToolResponse.success(s);
        } else if (method.isStructuredContent()) {
            return new ToolResponse(false, List.of(new TextContent(jsonb.toJson(result))), result, null);
        } else {
            ToolArgumentsImpl toolArgumentsImpl = (ToolArgumentsImpl) toolArgs;
            return encodeResult(result, toolArgumentsImpl.encoderRegistry());
        }
    }

    private ToolResponse encodeResult(Object result, EncoderRegistry encoderRegistry) {
        if (result == null) {
            return ToolResponse.success(Objects.toString(result));
        }
        Class<?> resultType = result.getClass();
        if (result instanceof List<?> list && !list.isEmpty()) {
            resultType = list.get(0).getClass();
        }
        Optional<Encoder<?, ?>> encoder = encoderRegistry.findEncoder(resultType);

        if (encoder.isPresent()) {
            return encodeResultWithEncoder(result, encoder.get(), jsonb);
        } else {
            return ToolResponse.success(Objects.toString(result));
        }
    }

    @SuppressWarnings("unchecked")
    public ToolResponse encodeResultWithEncoder(Object result, Encoder<?, ?> encoder, Jsonb jsonb) {
        try {
            if (encoder instanceof ToolResponseEncoder) {
                ToolResponse response = ((ToolResponseEncoder<Object>) encoder).encode(result);
                return response;
            } else if (encoder instanceof ContentEncoder) {
                if (result instanceof Iterable) {
                    return encodeIterableWithElementEncoder((Iterable<?>) result, (ContentEncoder<Object>) encoder);
                }
                Content content = ((ContentEncoder<Object>) encoder).encode(result);
                return ToolResponse.success(content);
            } else {
                // Should not occur, we only discover ToolResponseEncoders and ContentEncoders
                throw new IllegalStateException(encoder.getClass().getName() + " is not a ToolResponseEncoder or a ContentEncoder");
            }
        } catch (Exception e) {
            // Report encoding exception
            Tr.error(tc, "CWMCM0019E.error.encoding.element", encoder.getClass().getName(), method.name(), e);
            throw new JSONRPCException(JSONRPCErrorCode.INTERNAL_ERROR, null);
        }
    }

    private ToolResponse encodeIterableWithElementEncoder(Iterable<?> iterable, ContentEncoder<Object> encoder) {
        List<Content> encodedElements = new ArrayList<>();
        for (Object element : iterable) {
            if (element == null) {
                continue;
            }
            Content content = encoder.encode(element);
            encodedElements.add(content);
        }
        return ToolResponse.success(encodedElements);
    }

    protected ToolResponse createBusinessErrorResponse(Throwable t) {
        String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
        return ToolResponse.error(msg);
    }

    protected ToolResponse createNonBusinessErrorResponse(Throwable t) {
        Tr.error(tc,
                 "CWMCM0010E.internal.server.error.detailed",
                 method.name(),
                 t);
        return ToolResponse.error(Tr.formatMessage(tc, "CWMCM0011E.internal.server.error"));
    }

    protected boolean isBusinessException(Throwable t) {
        if (t instanceof ToolCallException) {
            return true;
        }

        for (Class<? extends Throwable> clazz : method.businessExceptions()) {
            if (clazz.isAssignableFrom(t.getClass())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Release a creational context and log any errors
     *
     * @param cc the creational context
     */
    protected void releaseCc(CreationalContext<Object> cc) {
        try {
            cc.release();
        } catch (Exception ex) {
            Tr.warning(tc, "CWMCM0012E.bean.release.fail", ex, method.name());
        }
    }
}