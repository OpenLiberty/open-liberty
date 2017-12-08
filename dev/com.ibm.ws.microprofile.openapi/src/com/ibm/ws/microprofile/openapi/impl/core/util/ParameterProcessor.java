package com.ibm.ws.microprofile.openapi.impl.core.util;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.openapi.impl.core.converter.ModelConverters;
import com.ibm.ws.microprofile.openapi.impl.core.converter.ResolvedSchema;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.ParameterImpl;

public class ParameterProcessor {
    //static Logger LOGGER = LoggerFactory.getLogger(ParameterProcessor.class);

    @FFDCIgnore(IOException.class)
    public static Parameter applyAnnotations(Parameter parameter, Type type, List<Annotation> annotations, Components components, String[] classTypes, String[] methodTypes) {

        final AnnotationsHelper helper = new AnnotationsHelper(annotations, type);
        if (helper.isContext()) {
            return null;
        }
        if (parameter == null) {
            // consider it to be body param
            parameter = new ParameterImpl();
        }

        // first handle schema
        List<Annotation> reworkedAnnotations = new ArrayList<>(annotations);
        Annotation paramSchemaOrArrayAnnotation = getParamSchemaAnnotation(annotations);
        if (paramSchemaOrArrayAnnotation != null) {
            reworkedAnnotations.add(paramSchemaOrArrayAnnotation);
        }
        ResolvedSchema resolvedSchema = ModelConverters.getInstance().resolveAnnotatedType(type, reworkedAnnotations, "");

        if (resolvedSchema.schema != null) {
            parameter.setSchema(resolvedSchema.schema);
        }
        resolvedSchema.referencedSchemas.forEach((key, schema) -> components.addSchema(key, schema));

        for (Annotation annotation : annotations) {
            if (annotation instanceof org.eclipse.microprofile.openapi.annotations.parameters.Parameter) {
                org.eclipse.microprofile.openapi.annotations.parameters.Parameter p = (org.eclipse.microprofile.openapi.annotations.parameters.Parameter) annotation;
                if (p.hidden()) {
                    return null;
                }
                if (StringUtils.isNotBlank(p.description())) {
                    parameter.setDescription(p.description());
                }
                if (StringUtils.isNotBlank(p.name())) {
                    parameter.setName(p.name());
                }
                if (StringUtils.isNotBlank(p.in().toString())) {
                    ((ParameterImpl) parameter).setIn(Parameter.In.valueOf(p.in().toString().toUpperCase()));
                }
                if (StringUtils.isNotBlank(p.example())) {
                    try {
                        parameter.setExample(Json.mapper().readTree(p.example()));
                    } catch (IOException e) {
                        parameter.setExample(p.example());
                    }
                }
                if (p.deprecated()) {
                    parameter.setDeprecated(p.deprecated());
                }
                if (p.required()) {
                    parameter.setRequired(p.required());
                }
                if (p.allowEmptyValue()) {
                    parameter.setAllowEmptyValue(p.allowEmptyValue());
                }
                if (p.allowReserved()) {
                    parameter.setAllowReserved(p.allowReserved());
                }

                Map<String, Example> exampleMap = new HashMap<>();
                for (ExampleObject exampleObject : p.examples()) {
                    AnnotationsUtils.getExample(exampleObject).ifPresent(example -> exampleMap.put(exampleObject.name(), example));
                }
                if (exampleMap.size() > 0) {
                    parameter.setExamples(exampleMap);
                }

                Optional<Content> content = AnnotationsUtils.getContent(p.content(), classTypes, methodTypes, parameter.getSchema());
                if (content.isPresent()) {
                    parameter.setContent(content.get());
                    parameter.setSchema(null);
                }
                setParameterStyle(parameter, p);
                setParameterExplode(parameter, p);

            } else if (annotation.annotationType().getName().equals("javax.ws.rs.PathParam")) {
                try {
                    String name = (String) annotation.annotationType().getMethod("value").invoke(annotation);
                    if (StringUtils.isNotBlank(name)) {
                        parameter.setName(name);
                    }
                } catch (Exception e) {
                }
            } else if (annotation.annotationType().getName().equals("javax.validation.constraints.Size")) {
                try {
                    if (parameter.getSchema() == null) {
                        parameter.setSchema(new SchemaImpl().type(SchemaType.ARRAY));
                    }
                    if (parameter.getSchema().getType() == SchemaType.ARRAY) {
                        Integer min = (Integer) annotation.annotationType().getMethod("min").invoke(annotation);
                        if (min != null) {
                            parameter.getSchema().setMinItems(min);
                        }
                        Integer max = (Integer) annotation.annotationType().getMethod("max").invoke(annotation);
                        if (max != null) {
                            parameter.getSchema().setMaxItems(max);
                        }
                    }

                } catch (Exception e) {
                    //LOGGER.error("failed on " + annotation.annotationType().getName(), e);
                }
            }
        }
        final String defaultValue = helper.getDefaultValue();

        Schema paramSchema = parameter.getSchema();
        if (paramSchema == null) {
            if (parameter.getContent() != null && parameter.getContent().values().size() > 0) {
                paramSchema = parameter.getContent().values().iterator().next().getSchema();
            }
        }
        if (paramSchema != null) {
            if (paramSchema.getType() == SchemaType.ARRAY) {
                if (defaultValue != null) {
                    paramSchema.getItems().setDefaultValue(defaultValue);
                }
            } else {
                if (defaultValue != null) {
                    paramSchema.setDefaultValue(defaultValue);
                }
            }
        }
        return parameter;
    }

    public static void setParameterExplode(Parameter parameter, org.eclipse.microprofile.openapi.annotations.parameters.Parameter p) {
        if (isExplodable(p)) {
            if (Explode.TRUE.equals(p.explode())) {
                parameter.setExplode(Boolean.TRUE);
            } else if (Explode.FALSE.equals(p.explode())) {
                parameter.setExplode(Boolean.FALSE);
            }
        }
    }

    private static boolean isExplodable(org.eclipse.microprofile.openapi.annotations.parameters.Parameter p) {
        org.eclipse.microprofile.openapi.annotations.media.Schema schema = p.schema();
        boolean explode = true;
        if (schema != null) {
            Class implementation = schema.implementation();
            if (implementation == Void.class) {
                if (!schema.type().equals("object") && !schema.type().equals("array")) {
                    explode = false;
                }
            }
        }
        return explode;
    }

    public static void setParameterStyle(Parameter parameter, org.eclipse.microprofile.openapi.annotations.parameters.Parameter p) {
        if (StringUtils.isNotBlank(p.style().toString())) {
            parameter.setStyle(Parameter.Style.valueOf(p.style().toString().toUpperCase()));
        }
    }

    public static Annotation getParamSchemaAnnotation(List<Annotation> annotations) {
        if (annotations == null) {
            return null;
        }
        org.eclipse.microprofile.openapi.annotations.media.Schema rootSchema = null;
        org.eclipse.microprofile.openapi.annotations.media.ArraySchema rootArraySchema = null;
        org.eclipse.microprofile.openapi.annotations.media.Schema contentSchema = null;
        org.eclipse.microprofile.openapi.annotations.media.Schema paramSchema = null;
        org.eclipse.microprofile.openapi.annotations.media.ArraySchema paramArraySchema = null;
        for (Annotation annotation : annotations) {
            if (annotation instanceof org.eclipse.microprofile.openapi.annotations.media.Schema) {
                rootSchema = (org.eclipse.microprofile.openapi.annotations.media.Schema) annotation;
            } else if (annotation instanceof org.eclipse.microprofile.openapi.annotations.media.ArraySchema) {
                rootArraySchema = (org.eclipse.microprofile.openapi.annotations.media.ArraySchema) annotation;
            } else if (annotation instanceof org.eclipse.microprofile.openapi.annotations.parameters.Parameter) {
                org.eclipse.microprofile.openapi.annotations.parameters.Parameter paramAnnotation = (org.eclipse.microprofile.openapi.annotations.parameters.Parameter) annotation;
                if (paramAnnotation.content().length > 0) {
                    if (AnnotationsUtils.hasSchemaAnnotation(paramAnnotation.content()[0].schema())) {
                        contentSchema = paramAnnotation.content()[0].schema();
                    }
                }
                if (AnnotationsUtils.hasSchemaAnnotation(paramAnnotation.schema())) {
                    paramSchema = paramAnnotation.schema();
                }
//                if (AnnotationsUtils.hasArrayAnnotation(paramAnnotation.array())) {
//                    paramArraySchema = paramAnnotation.array();
//                }
            }
        }
        if (rootSchema != null || rootArraySchema != null) {
            return null;
        }
        if (contentSchema != null) {
            return contentSchema;
        }
        if (paramSchema != null) {
            return paramSchema;
        }
        if (paramArraySchema != null) {
            return paramArraySchema;
        }
        return null;
    }

    public static Type getParameterType(org.eclipse.microprofile.openapi.annotations.parameters.Parameter paramAnnotation) {
        if (paramAnnotation == null) {
            return null;
        }
        org.eclipse.microprofile.openapi.annotations.media.Schema contentSchema = null;
        org.eclipse.microprofile.openapi.annotations.media.Schema paramSchema = null;
        org.eclipse.microprofile.openapi.annotations.media.ArraySchema paramArraySchema = null;

        if (paramAnnotation.content().length > 0) {
            if (AnnotationsUtils.hasSchemaAnnotation(paramAnnotation.content()[0].schema())) {
                contentSchema = paramAnnotation.content()[0].schema();
            }
        }
        if (AnnotationsUtils.hasSchemaAnnotation(paramAnnotation.schema())) {
            paramSchema = paramAnnotation.schema();
        }
//        if (AnnotationsUtils.hasArrayAnnotation(paramAnnotation.array())) {
//            paramArraySchema = paramAnnotation.array();
//        }
        if (contentSchema != null) {
            return AnnotationsUtils.getSchemaType(contentSchema);
        }
        if (paramSchema != null) {
            return AnnotationsUtils.getSchemaType(paramSchema);
        }
        if (paramArraySchema != null) {
            return AnnotationsUtils.getSchemaType(paramArraySchema.schema());
        }
        return String.class;
    }

    public static final String MEDIA_TYPE = "*/*";

    /**
     * The <code>AnnotationsHelper</code> class defines helper methods for
     * accessing supported parameter annotations.
     */
    private static class AnnotationsHelper {
        private boolean context;
        private final String defaultValue;

        /**
         * Constructs an instance.
         *
         * @param annotations array or parameter annotations
         */
        public AnnotationsHelper(List<Annotation> annotations, Type _type) {
            String rsDefault = null;
            if (annotations != null) {
                for (Annotation item : annotations) {
                    if ("javax.ws.rs.core.Context".equals(item.annotationType().getName())) {
                        context = true;
                    } else if ("javax.ws.rs.DefaultValue".equals(item.annotationType().getName())) {
                        try {
                            rsDefault = (String) item.annotationType().getMethod("value").invoke(item);
                        } catch (Exception ex) {
                            //LOGGER.error("Invocation of value method failed", ex);
                        }
                    }
                }
            }
            defaultValue = rsDefault;

        }

        /**
         */
        public boolean isContext() {
            return context;
        }

        /**
         * Returns default value from annotation.
         *
         * @return default value from annotation
         */
        public String getDefaultValue() {
            return defaultValue;
        }
    }
}
