package io.smallrye.graphql.schema.creator;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.smallrye.graphql.schema.Annotations;
import io.smallrye.graphql.schema.Classes;
import io.smallrye.graphql.schema.ScanningContext;
import io.smallrye.graphql.schema.SchemaBuilderException;
import io.smallrye.graphql.schema.helper.Direction;
import io.smallrye.graphql.schema.helper.TypeNameHelper;
import io.smallrye.graphql.schema.model.Reference;
import io.smallrye.graphql.schema.model.ReferenceType;
import io.smallrye.graphql.schema.model.Scalars;

/**
 * Here we create references to things that might not yet exist.
 * 
 * We store all references to be created later.
 * 
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class ReferenceCreator {
    private static final Logger LOG = Logger.getLogger(ReferenceCreator.class.getName());

    private final Queue<Reference> inputReferenceQueue = new ArrayDeque<>();
    private final Queue<Reference> typeReferenceQueue = new ArrayDeque<>();
    private final Queue<Reference> enumReferenceQueue = new ArrayDeque<>();
    private final Queue<Reference> interfaceReferenceQueue = new ArrayDeque<>();

    // Some maps we populate during scanning
    private final Map<String, Reference> inputReferenceMap = new HashMap<>();
    private final Map<String, Reference> typeReferenceMap = new HashMap<>();
    private final Map<String, Reference> enumReferenceMap = new HashMap<>();
    private final Map<String, Reference> interfaceReferenceMap = new HashMap<>();

    /**
     * Clear the scanned references. This is done when we created all references and do not need to
     * remember what to scan.
     */
    public void clear() {
        inputReferenceMap.clear();
        typeReferenceMap.clear();
        enumReferenceMap.clear();
        interfaceReferenceMap.clear();

        inputReferenceQueue.clear();
        typeReferenceQueue.clear();
        enumReferenceQueue.clear();
        interfaceReferenceQueue.clear();
    }

    /**
     * Get the values for a certain type
     * 
     * @param referenceType the type
     * @return the references
     */
    public Queue<Reference> values(ReferenceType referenceType) {
        return getReferenceQueue(referenceType);
    }

    /**
     * Get a reference to a field type for an operation
     * Direction is OUT on a field (and IN on an argument)
     * In the case of operations, there is no fields (only methods)
     * 
     * @param fieldType the java type
     * @param annotationsForMethod annotation on this operations method
     * @return a reference to the type
     */
    public Reference createReferenceForOperationField(Type fieldType, Annotations annotationsForMethod) {
        return getReference(Direction.OUT, null, fieldType, annotationsForMethod);
    }

    /**
     * Get a reference to a argument type for an operation
     * Direction is IN on an argument (and OUT on a field)
     * In the case of operation, there is no field (only methods)
     * 
     * @param argumentType the java type
     * @param annotationsForThisArgument annotations on this argument
     * @return a reference to the argument
     */
    public Reference createReferenceForOperationArgument(Type argumentType, Annotations annotationsForThisArgument) {
        return getReference(Direction.IN, null, argumentType, annotationsForThisArgument);
    }

    /**
     * Get a reference to a field (method response) on an interface
     * 
     * Interfaces is only usable on Type, so the direction in OUT.
     * 
     * @param methodType the method response type
     * @param annotationsForThisMethod annotations on this method
     * @return a reference to the type
     */
    public Reference createReferenceForInterfaceField(Type methodType, Annotations annotationsForThisMethod) {
        return getReference(Direction.OUT, null, methodType, annotationsForThisMethod);
    }

    /**
     * Get a reference to a Field Type for a InputType or Type.
     * 
     * We need both the type and the getter/setter method as both is applicable.
     * 
     * @param fieldType the field type
     * @param methodType the method type
     * @param annotations the annotations on the field and method
     * @return a reference to the type
     */
    public Reference createReferenceForPojoField(Direction direction, Type fieldType, Type methodType,
            Annotations annotations) {
        return getReference(direction, fieldType, methodType, annotations);
    }

    /**
     * This method create a reference to type that might not yet exist.
     * It also store to be created later, if we do not already know about it.
     * 
     * @param direction the direction (in or out)
     * @param classInfo the Java class
     * @return a reference
     */
    public Reference createReference(Direction direction, ClassInfo classInfo) {
        // Get the initial reference type. It's either Type or Input depending on the direction. This might change it 
        // we figure out this is actually an enum or interface
        ReferenceType referenceType = getCorrectReferenceType(direction);

        // Now check if this is an interface or enum
        if (Classes.isInterface(classInfo)) {
            // Also check that we create all implementations
            Collection<ClassInfo> knownDirectImplementors = ScanningContext.getIndex()
                    .getAllKnownImplementors(classInfo.name());
            for (ClassInfo impl : knownDirectImplementors) {
                // TODO: First check the class annotations for @Type, if we get one that has that, use it, else any/all ?
                createReference(direction, impl);
            }
            referenceType = ReferenceType.INTERFACE;
        } else if (Classes.isEnum(classInfo)) {
            referenceType = ReferenceType.ENUM;
        }

        // Now we should have the correct reference type.
        String className = classInfo.name().toString();
        Annotations annotationsForClass = Annotations.getAnnotationsForClass(classInfo);
        String name = TypeNameHelper.getAnyTypeName(referenceType, classInfo, annotationsForClass);

        Reference reference = new Reference(className, name, referenceType);

        // Now add it to the correct map
        putIfAbsent(className, reference, referenceType);

        return reference;
    }

    private Reference getReference(Direction direction,
            Type fieldType,
            Type methodType,
            Annotations annotations) {

        // In some case, like operations and interfaces, there is not fieldType
        if (fieldType == null)
            fieldType = methodType;

        String fieldTypeName = fieldType.name().toString();

        if (annotations.containsOneOfTheseAnnotations(Annotations.ID)) {
            // ID
            return Scalars.getIDScalar(fieldTypeName);
        } else if (Scalars.isScalar(fieldTypeName)) {
            // Scalar
            return Scalars.getScalar(fieldTypeName);
        } else if (fieldType.kind().equals(Type.Kind.ARRAY)) {
            // Array 
            Type typeInArray = fieldType.asArrayType().component();
            Type typeInMethodArray = methodType.asArrayType().component();
            return getReference(direction, typeInArray, typeInMethodArray, annotations);
        } else if (fieldType.kind().equals(Type.Kind.PARAMETERIZED_TYPE)) {
            // Collections
            Type typeInCollection = fieldType.asParameterizedType().arguments().get(0);
            Type typeInMethodCollection = methodType.asParameterizedType().arguments().get(0);
            return getReference(direction, typeInCollection, typeInMethodCollection, annotations);
        } else if (fieldType.kind().equals(Type.Kind.CLASS)) {
            ClassInfo classInfo = ScanningContext.getIndex().getClassByName(fieldType.name());
            if (classInfo != null) {
                return createReference(direction, classInfo);
            } else {
                LOG.warn("Class [" + fieldType.name()
                        + "] in not indexed in Jandex. Can not scan Object Type, defaulting to String Scalar");
                return Scalars.getScalar(String.class.getName()); // default
            }
        } else {
            throw new SchemaBuilderException(
                    "Don't know what to do with [" + fieldType + "] of kind [" + fieldType.kind() + "]");
        }
    }

    private void putIfAbsent(String key, Reference reference, ReferenceType referenceType) {
        Map<String, Reference> map = getReferenceMap(referenceType);
        Queue<Reference> queue = getReferenceQueue(referenceType);
        if (!map.containsKey(key)) {
            map.put(key, reference);
            queue.add(reference);
        }
    }

    private Map<String, Reference> getReferenceMap(ReferenceType referenceType) {
        switch (referenceType) {
            case ENUM:
                return enumReferenceMap;
            case INPUT:
                return inputReferenceMap;
            case INTERFACE:
                return interfaceReferenceMap;
            case TYPE:
                return typeReferenceMap;
            default:
                return null;
        }
    }

    private Queue<Reference> getReferenceQueue(ReferenceType referenceType) {
        switch (referenceType) {
            case ENUM:
                return enumReferenceQueue;
            case INPUT:
                return inputReferenceQueue;
            case INTERFACE:
                return interfaceReferenceQueue;
            case TYPE:
                return typeReferenceQueue;
            default:
                return null;
        }
    }

    private static ReferenceType getCorrectReferenceType(Direction direction) {
        if (direction.equals(Direction.IN)) {
            return ReferenceType.INPUT;
        } else {
            return ReferenceType.TYPE;
        }
    }
}
