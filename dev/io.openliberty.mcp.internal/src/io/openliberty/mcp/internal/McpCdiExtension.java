/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.internal.ToolMetadata.SpecialArgumentMetadata;
import io.openliberty.mcp.internal.encoders.EncoderRegistry;
import io.openliberty.mcp.internal.exceptions.GenericArgumentException;
import io.openliberty.mcp.internal.exceptions.UnsupportedTypeException;
import io.openliberty.mcp.internal.moduleScope.ModuleContext;
import io.openliberty.mcp.internal.requests.McpRequestIdDeserializer;
import io.openliberty.mcp.internal.requests.McpRequestIdSerializer;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import io.openliberty.mcp.internal.tools.BeanMethodHandler.MethodMetadata;
import io.openliberty.mcp.messaging.Encoder;
import io.openliberty.mcp.tools.ToolManager.ToolArgument;
import io.openliberty.mcp.tools.ToolResponseEncoder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

/**
 * Finds tools
 */

public class McpCdiExtension implements Extension {

    private static final TraceComponent tc = Tr.register(McpCdiExtension.class);

    private final List<Bean<?>> encoderBeans = new ArrayList<>();
    private EncoderRegistry encoderRegistry;
    private ConcurrentHashMap<String, ArrayList<String>> duplicateToolsMap = new ConcurrentHashMap<>();

    private SchemaRegistry schemas = new SchemaRegistry();
    private Jsonb jsonb = createJsonb();
    private ToolRegistry tools = new ToolRegistry(schemas, jsonb);
    private ModuleContext moduleContext;

    private static Jsonb createJsonb() {
        JsonbConfig jsonbConfig = new JsonbConfig().withSerializers(new McpRequestIdSerializer())
                                                   .withDeserializers(new McpRequestIdDeserializer());

        return JsonbBuilder.create(jsonbConfig);
    }

    void addModuleContext(@Observes AfterBeanDiscovery abd) {
        moduleContext = new ModuleContext();
        abd.addContext(moduleContext);
    }

    void endModuleContext(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event) {
        if (moduleContext != null) {
            moduleContext.shutdown();
        }
    }

    void registerTools(@Observes ProcessManagedBean<?> pmb, BeanManager beanManager) {
        AnnotatedType<?> type = pmb.getAnnotatedBeanClass();
        for (AnnotatedMethod<?> m : type.getMethods()) {
            Tool toolAnnotation = m.getAnnotation(Tool.class);
            if (toolAnnotation != null) {
                registerTool(toolAnnotation, pmb.getBean(), m, beanManager);
            }
        }
    }

    void discoverEncoderBeans(@Observes ProcessManagedBean<?> processManagedBean) {
        AnnotatedType<?> type = processManagedBean.getAnnotatedBeanClass();
        Class<?> javaClass = type.getJavaClass();
        if (Encoder.class.isAssignableFrom(javaClass)) {
            encoderBeans.add(processManagedBean.getBean());
        }
    }

    void afterDeploymentValidation(@Observes AfterDeploymentValidation afterDeploymentValidation, BeanManager manager) {
        registerEncoders(manager);

        boolean error = reportOnInvalidToolNames(afterDeploymentValidation) |
                        reportOnDuplicateTools(afterDeploymentValidation) |
                        reportOnToolArgEdgeCases(afterDeploymentValidation) |
                        reportOnDuplicateSpecialArguments(afterDeploymentValidation) |
                        reportOnInvalidSpecialArguments(afterDeploymentValidation);

        if (error) {
            afterDeploymentValidation.addDeploymentProblem(new Exception(Tr.formatMessage(tc, "CWMCM0005E.validation.error")));
        }
    }

    void registerEncoders(BeanManager beanManager) {
        encoderRegistry = beanManager.createInstance().select(EncoderRegistry.class).get();

        CreationalContext<?> context = beanManager.createCreationalContext(null);

        List<ToolResponseEncoder<?>> toolResponseEncoders = new ArrayList<>();
        List<ContentEncoder<?>> contentEncoders = new ArrayList<>();

        for (Bean<?> bean : encoderBeans) {
            if (ToolResponseEncoder.class.isAssignableFrom(bean.getBeanClass())) {
                ToolResponseEncoder<?> encoder = (ToolResponseEncoder<?>) beanManager.getReference(bean, bean.getBeanClass(), context);
                toolResponseEncoders.add(encoder);
                logEncoderRegistration(bean);
            } else if (ContentEncoder.class.isAssignableFrom(bean.getBeanClass())) {
                ContentEncoder<?> encoder = (ContentEncoder<?>) beanManager.getReference(bean, bean.getBeanClass(), context);
                contentEncoders.add(encoder);
                logEncoderRegistration(bean);
            }
        }

        encoderRegistry.registerEncoders(toolResponseEncoders, contentEncoders);

        context.release();
    }

    private static void logEncoderRegistration(Bean<?> encoderBean) {
        if (TraceComponent.isAnyTracingEnabled()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(McpCdiExtension.class, tc, "Registered encoder: " + encoderBean.getName(), encoderBean);
            } else if (tc.isEventEnabled()) {
                Tr.event(McpCdiExtension.class, tc, "Registered encoder: " + encoderBean.getName());
            }
        }
    }

    /**
     * @param afterDeploymentValidation
     */
    private boolean reportOnToolArgEdgeCases(AfterDeploymentValidation afterDeploymentValidation) {
        boolean foundErrors = false;

        for (ToolMetadata tool : tools.getAllTools()) {
            Set<String> names = new HashSet<>();

            for (ToolArgument argMetadata : tool.arguments()) {
                for (var error : ToolValidation.validateToolArgument(argMetadata)) {
                    switch (error.type()) {
                        case NAME_BLANK -> Tr.error(tc, "CWMCM0001E.blank.arguments", tool.getToolQualifiedName());
                        case NAME_MISSING -> Tr.error(tc, "CWMCM0003E.missing.tool.argument.name", tool.getToolQualifiedName());
                        case NO_CONVERTER -> Tr.error(tc, "CWMCM0017E.missing.toolarg.defaultvalue.converter", tool.getToolQualifiedName(), argMetadata.name(), argMetadata.type());
                        case CONVERSION_ERROR -> Tr.error(tc, "CWMCM0020E.defaultvalue.conversion.error", tool.getToolQualifiedName(), argMetadata.name(), argMetadata.type(),
                                                          argMetadata.defaultValue(), error.exception());
                    }
                    foundErrors = true;
                }

                if (!names.add(argMetadata.name())) {
                    Tr.error(tc, "CWMCM0002E.duplicate.arguments", tool.getToolQualifiedName(), argMetadata.name());
                    foundErrors = true;
                }
            }
        }

        return foundErrors;
    }

    private boolean reportOnDuplicateTools(AfterDeploymentValidation afterDeploymentValidation) {
        boolean error = false;
        // prune items that are not duplicates
        duplicateToolsMap.entrySet().removeIf(e -> e.getValue().size() == 1);
        for (String toolName : duplicateToolsMap.keySet()) {
            error = true;
            List<String> qualifiedNames = duplicateToolsMap.get(toolName);
            Tr.error(tc, "CWMCM0004E.duplicate.tools", toolName, String.join(",", qualifiedNames));
        }
        return error;

    }

    private boolean reportOnInvalidToolNames(AfterDeploymentValidation afterDeploymentValidation) {
        boolean hasErrors = false;
        for (ToolMetadata tool : tools.getAllTools()) {
            for (var error : ToolValidation.validateToolName(tool.name())) {
                hasErrors = true;
                switch (error) {
                    case INVALID_CHARACTERS -> Tr.error(tc, "CWMCM0024E.invalid.character.tool.name", tool.name(), tool.getToolQualifiedName());
                    case INVALID_LENGTH -> Tr.error(tc, "CWMCM0023E.invalid.length.tool.name", tool.name(), tool.getToolQualifiedName());
                }
            }
        }
        return hasErrors;
    }

    private boolean reportOnDuplicateSpecialArguments(AfterDeploymentValidation afterDeploymentValidation) {
        AtomicBoolean error = new AtomicBoolean(false);
        for (ToolMetadata tool : tools.getAllTools()) {
            if (tool.methodMetadata().isEmpty()) {
                continue;
            }
            MethodMetadata methodMetadata = tool.methodMetadata().get();
            Map<SpecialArgumentType.Resolution, Integer> resultCountMap = new HashMap<>();
            for (SpecialArgumentMetadata specialArgument : methodMetadata.specialArguments()) {
                SpecialArgumentType.Resolution specialArgumentTypeResolution = specialArgument.typeResolution();
                if (specialArgumentTypeResolution.specialArgsType() == SpecialArgumentType.UNSUPPORTED) {
                    continue;
                }
                resultCountMap.merge(specialArgumentTypeResolution, 1, Integer::sum);

            }
            resultCountMap.forEach((k, v) -> {
                if (v > 1) {
                    error.set(true);
                    Tr.error(tc, "CWMCM0006E.duplicate.special.arguments", tool.getToolQualifiedName(),
                             k.actualClass().getSimpleName());

                }

            });
        }
        return error.get();

    }

    private boolean reportOnInvalidSpecialArguments(AfterDeploymentValidation afterDeploymentValidation) {
        boolean error = false;
        for (ToolMetadata tool : tools.getAllTools()) {
            if (tool.methodMetadata().isEmpty()) {
                continue;
            }
            for (SpecialArgumentMetadata specialArgument : tool.methodMetadata().get().specialArguments()) {
                if (specialArgument.typeResolution().specialArgsType() == SpecialArgumentType.UNSUPPORTED) {
                    error = true;
                    Tr.error(tc, "CWMCM0007E.invalid.arguments", tool.getToolQualifiedName(),
                             specialArgument.typeResolution());
                }
            }
        }
        return error;
    }

    private void registerTool(Tool tool, Bean<?> bean, AnnotatedMethod<?> method, BeanManager beanManager) {
        try {
            ToolMetadata toolmd = ToolMetadata.createFrom(tool, bean, method, beanManager, jsonb);
            List<String> duplicatesList = duplicateToolsMap.computeIfAbsent(toolmd.name(), key -> new ArrayList<>());
            duplicatesList.add(toolmd.getToolQualifiedName());
            if (duplicatesList.size() <= 1) {
                tools.addTool(toolmd);
                if (TraceComponent.isAnyTracingEnabled()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Registered tool: " + toolmd.name(), toolmd);
                    } else if (tc.isEventEnabled()) {
                        Tr.event(this, tc, "Registered tool: " + toolmd.name(), method);
                    }
                }
            }
        } catch (GenericArgumentException e) {
            for (String argument : e.getArguments()) {
                Tr.error(tc, "CWMCM0018E.generic.arguments", ToolMetadata.getToolQualifiedName(bean, method), argument);
            }
        } catch (UnsupportedTypeException e) {
            Tr.error(tc, "CWMCM0025E.unsupported.output", e.getType(), ToolMetadata.getToolQualifiedName(bean, method));
        }
    }

    public ToolRegistry getToolRegistry() {
        return tools;
    }

    public SchemaRegistry getSchemaRegistry() {
        return schemas;
    }

    public Jsonb getJsonb() {
        return jsonb;
    }

    public EncoderRegistry getEncoderRegistry() {
        return encoderRegistry;
    }
}
