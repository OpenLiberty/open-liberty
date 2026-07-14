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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.mcpjava.server.ContentEncoder;
import org.mcpjava.server.content.ContentBlock;
import org.mcpjava.server.content.TextContent;
import org.mcpjava.server.tools.ToolResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.internal.McpServlet.ToolArgumentsImpl;
import io.openliberty.mcp.internal.ToolMetadata.SpecialArgumentMetadata;
import io.openliberty.mcp.internal.encoders.EncoderRegistry;
import io.openliberty.mcp.tools.ToolCallException;
import io.openliberty.mcp.tools.ToolManager.ToolArguments;
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
                case REQUEST -> t.request();
                case PROGRESS -> t.progress();
                default -> throw new RuntimeException("Unknown arg"); //TODO FIX - possibly we can guarantee this is validated earlier
            };
        }
        return argsArray;
    }

    protected ToolResponse createSuccessfulResponse(Object result, ToolArguments toolArgs) {
        // Map method response to a ToolResponse
        if (result instanceof ToolResponse response) {
            return response;
        } else if (result instanceof List<?> list && !list.isEmpty() && list.stream().allMatch(item -> item instanceof ContentBlock)) {
            @SuppressWarnings("unchecked")
            List<ContentBlock> contents = (List<ContentBlock>) list;
            var builder = ToolResponse.builder();
            contents.forEach(builder::addContent);
            return builder.build();
        } else if (result instanceof ContentBlock content) {
            return ToolResponse.builder().addContent(content).build();
        } else if (result instanceof String s) {
            return ToolResponse.ofText(s);
        } else if (method.isStructuredContent()) {
            return ToolResponse.builder()
                               .addContent(TextContent.of(jsonb.toJson(result)))
                               .setStructuredContent(result)
                               .build();
        } else {
            ToolArgumentsImpl toolArgumentsImpl = (ToolArgumentsImpl) toolArgs;
            return encodeResult(result, toolArgumentsImpl.encoderRegistry());
        }
    }

    private <T, E> ToolResponse encodeResult(T result, EncoderRegistry encoderRegistry) {
        if (result == null) {
            return ToolResponse.ofText(Objects.toString(result));
        }

        var response = encoderRegistry.findToolResponseEncoder(result)
                                      .map(e -> e.encode(result))
                                      .orElse(null);

        if (response == null) {
            if (result instanceof List<?> resultList) {
                var responseBuilder = ToolResponse.builder();
                resultList.stream()
                          .map(o -> encodeAsContent(o, encoderRegistry))
                          .forEach(responseBuilder::addContent);
                response = responseBuilder.build();
            } else {
                ContentBlock content = encodeAsContent(result, encoderRegistry);
                response = ToolResponse.builder().addContent(content).build();
            }
        }

        return response;
    }

    /**
     * @param o
     * @param encoderRegistry
     * @return
     */
    private <T> ContentBlock encodeAsContent(T o, EncoderRegistry encoderRegistry) {
        return encoderRegistry.findContentEncoder(o)
                              .map(encoder -> encoder.encode(o))
                              .orElseGet(() -> TextContent.of(Objects.toString(o)));
    }

    public ToolResponse encode(ContentEncoder<?> encoder) {
        return null;

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
