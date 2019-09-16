package io.leangen.graphql.generator.union;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.annotations.GraphQLUnion;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

/**
 * @author Bojan Tomic (kaqqao)
 */
public abstract class Union {

    private String name;
    private String description;
    private List<AnnotatedType> javaTypes;

    private static final String SINGLE_TYPE_UNION_ERROR = "Unions of a single type are not allowed";

    public Union(String name, String description, List<AnnotatedType> javaTypes) {
        this.name = name;
        this.description = description;
    }

    public static AnnotatedType unionize(AnnotatedType[] types, MessageBundle messageBundle) {
        Objects.requireNonNull(types);
        if (types.length < 2) {
            if (types.length == 1 && GenericTypeReflector.isSuperType(Union.class, types[0].getType())) {
                return types[0];
            }
            throw new IllegalArgumentException(SINGLE_TYPE_UNION_ERROR);
        }
        AnnotatedType t1 = types[0];
        if (stream(types).anyMatch(t -> t.isAnnotationPresent(GraphQLUnion.class))) {
            if (stream(types).allMatch(t -> t.isAnnotationPresent(GraphQLUnion.class) && nameEquals(t, t1, messageBundle))) {
                return of(types);
            } else {
                throw new IllegalArgumentException("All union members must be explicitly annotated: " + Arrays.toString(types));
            }
        }
        if (stream(types).allMatch(t -> t instanceof AnnotatedParameterizedType)) {
            AnnotatedParameterizedType p1 = (AnnotatedParameterizedType) t1;
            AnnotatedParameterizedType[] pTypes = stream(types)
                    .map(t -> (AnnotatedParameterizedType) t)
                    .toArray(AnnotatedParameterizedType[]::new);
            AnnotatedType[] params = new AnnotatedType[p1.getAnnotatedActualTypeArguments().length];
            for (int i = 0; i < p1.getAnnotatedActualTypeArguments().length; i++) {
                final int j = i;
                params[i] = unionize(stream(pTypes)
                        .map(p -> p.getAnnotatedActualTypeArguments()[j])
                        .toArray(AnnotatedType[]::new), messageBundle);
            }
            Class<?> rawType = ((Class<?>) ((ParameterizedType) p1.getType()).getRawType());
            return TypeFactory.parameterizedAnnotatedClass(rawType, ClassUtils.getAllAnnotations(stream(types)), params);
        }
        if (stream(types).allMatch(t -> t instanceof AnnotatedArrayType)) {
            AnnotatedType[] components = stream(types)
                    .map(type -> ((AnnotatedArrayType) type).getAnnotatedGenericComponentType())
                    .toArray(AnnotatedType[]::new);
            return TypeFactory.arrayOf(unionize(components, messageBundle), ClassUtils.getAllAnnotations(stream(types)));
        }
        if (stream(types).allMatch(t -> types[0].getType().equals(t.getType()))) {
            return types[0];
        }
        throw new IllegalArgumentException("Types are incompatible and can not be unionized: ");
    }

    public static AnnotatedType of(AnnotatedType[] types) {
        Objects.requireNonNull(types);
        if (types.length < 2) {
            if (types.length == 1 && GenericTypeReflector.isSuperType(Union.class, types[0].getType())) {
                return types[0];
            }
            throw new IllegalArgumentException(SINGLE_TYPE_UNION_ERROR);
        }

        AnnotatedType[] distinctTypes = dedupe(types);
        Class union;
        try {
            union = ClassUtils.forName(Union.class.getName() + distinctTypes.length);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unions of more than 10 types are not supported");
        }
        Annotation unionAnnotation = stream(ClassUtils.getAllAnnotations(stream(types)))
                .filter(annotation -> annotation.annotationType().equals(GraphQLUnion.class))
                .filter(annotation -> !((GraphQLUnion) annotation).description().isEmpty())
                .findFirst().orElse(types[0].getAnnotation(GraphQLUnion.class));
        return TypeFactory.parameterizedAnnotatedClass(union, new Annotation[] {unionAnnotation}, distinctTypes);
    }

    /**
     * Collapses all {@link AnnotatedType}s of the same {@link java.lang.reflect.Type} into one, merging all the annotations
     * */
    private static AnnotatedType[] dedupe(AnnotatedType... types) {
        return stream(types)
                .collect(Collectors.groupingBy(AnnotatedType::getType)).values().stream()
                .map(typeGroup -> GenericTypeReflector.updateAnnotations(typeGroup.get(0), ClassUtils.getAllAnnotations(typeGroup.stream())))
                .toArray(AnnotatedType[]::new);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<AnnotatedType> getJavaTypes() {
        return javaTypes;
    }

    private static boolean nameEquals(AnnotatedType t1, AnnotatedType t2, MessageBundle messageBundle) {
        return messageBundle.interpolate(t1.getAnnotation(GraphQLUnion.class).name())
                .equals(messageBundle.interpolate(t2.getAnnotation(GraphQLUnion.class).name()));
    }
}
