/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.info.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.Type;

import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.MethodInfo;

public class MethodInfoImpl extends InfoImpl implements MethodInfo {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.anno.info");
    private static final String CLASS_NAME = MethodInfoImpl.class.getSimpleName();

    private static String getHashSuffix(NonDelayedClassInfoImpl declaringClass, String name, String description) {
        return ( declaringClass.getName() + "." + name + ", " + description );
    }

    //

    public MethodInfoImpl(String name, String description,
                          String exceptions[],
                          int modifiers,
                          NonDelayedClassInfoImpl declaringClass) {

        super( name, modifiers, declaringClass.getInfoStore(),
               getHashSuffix(declaringClass, name, description) );

        String methodName = "<init>";

        InfoStoreImpl useInfoStore = getInfoStore();

        this.description = useInfoStore.internDescription(description);

        this.declaringClass = declaringClass;

        if (exceptions != null) {
            for (int exceptionNo = 0; exceptionNo < exceptions.length; exceptionNo++) {
                String nextException = exceptions[exceptionNo];
                nextException = nextException.replace('/', '.');
                exceptions[exceptionNo] = useInfoStore.internClassName(nextException);
            }
        }

        this.exceptionClassNames = (exceptions == null) ? Collections.<String> emptyList() : Arrays.asList(exceptions);
        this.exceptionClassInfos = null;

        this.parameterClassInfos = null;
        this.parameterAnnotations = Collections.emptyList();

        // This bloats the log if allowed on FINER.
        if ( logger.isLoggable(Level.FINEST) ) {
            logger.logp(Level.FINEST, CLASS_NAME, methodName,
                        "{0} ] Created on [ {1} ] [ {2} ]",
                        new Object[] { getHashText(),
                                       getDeclaringClass().getHashText(),
                                       getDescription() });
        }
    }

    //

    protected ClassInfoImpl declaringClass;

    @Override
    public ClassInfoImpl getDeclaringClass() {
        return this.declaringClass;
    }

    //

    @Override
    protected String internName(String methodName) {
        return getInfoStore().internMethodName(methodName);
    }

    @Override
    public String getQualifiedName() {
        return getDeclaringClass().getName() + '.' + getName();
    }

    //

    private final String description;

    @Override
    public String getDescription() {
        return description;
    }

    // Information derived from the description ...

    private final List<String> exceptionClassNames;
    private List<ClassInfoImpl> exceptionClassInfos;

    private List<String> parameterTypeNames;
    private List<ClassInfoImpl> parameterClassInfos;
    private List<List<? extends AnnotationInfo>> parameterAnnotations;

    private ClassInfoImpl returnClassInfo;

    @Override
    public List<String> getExceptionTypeNames() {
        return exceptionClassNames;
    }

    @Override
    public List<ClassInfoImpl> getExceptionTypes() {
        if (this.exceptionClassInfos == null) {
            if (exceptionClassNames.isEmpty()) {
                this.exceptionClassInfos = Collections.emptyList();
            } else {
                ClassInfoImpl exceptionInfos[] = new ClassInfoImpl[exceptionClassNames.size()];

                int i = 0;
                for (String exceptionClassName : this.exceptionClassNames) {
                    exceptionInfos[i++] = getDelayableClassInfo(exceptionClassName);
                }
                exceptionClassInfos = Arrays.asList(exceptionInfos);
            }
        }

        return exceptionClassInfos;
    }

    //

    @Override
    public List<String> getParameterTypeNames() {
        if (parameterTypeNames == null) {
            Type[] argumentTypes = Type.getArgumentTypes(getDescription());

            if (argumentTypes.length == 0) {
                parameterTypeNames = Collections.emptyList();

            } else {
                String[] parameterNames = new String[argumentTypes.length];

                int i = 0;
                for (Type nextType : argumentTypes) {
                    // In all cases, the type name is the type name
                    // returned from 'org.objectweb.asm.Type'.

                    String nextTypeName = nextType.getClassName();

                    // Make sure to intern it!
                    nextTypeName = getInfoStore().internClassName(nextTypeName);

                    parameterNames[i++] = nextTypeName;
                }
                parameterTypeNames = Arrays.asList(parameterNames);
            }
        }

        return parameterTypeNames;
    }

    @Override
    public List<ClassInfoImpl> getParameterTypes() {
        if (this.parameterClassInfos == null) {
            Type[] argumentTypes = Type.getArgumentTypes(getDescription());

            if (argumentTypes.length == 0) {
                this.parameterClassInfos = Collections.emptyList();

            } else {
                ClassInfoImpl[] parameterInfos = new ClassInfoImpl[argumentTypes.length];

                int i = 0;
                for (Type nextType : argumentTypes) {
                    parameterInfos[i++] = getDelayableClassInfo(nextType);
                }
                parameterClassInfos = Arrays.asList(parameterInfos);
            }
        }

        return this.parameterClassInfos;
    }

    /** {@inheritDoc} */
    @Override
    public List<List<? extends AnnotationInfo>> getParameterAnnotations() {
        return parameterAnnotations;
    }

    public void setParameterAnnotations(AnnotationInfoImpl[][] parmAnnos) {
        if (parmAnnos == null) {
            parameterAnnotations = Collections.emptyList();
        } else {
            @SuppressWarnings("unchecked")
            List<? extends AnnotationInfo>[] parmInfos = new List[parmAnnos.length];
            for (int i = 0; i < parmAnnos.length; ++i) {
                parmInfos[i] = Arrays.asList(parmAnnos[i]);
            }
            parameterAnnotations = Arrays.asList(parmInfos);
        }
    }

    protected Type returnTypeAsType;
    protected String returnTypeName;

    // Can't put this in the interface because it is coupled with the implementation.

    public Type getReturnTypeAsType() {
        if (returnTypeAsType == null) {
            returnTypeAsType = Type.getReturnType(getDescription());
        }

        return returnTypeAsType;
    }

    @Override
    public String getReturnTypeName() {
        if (returnTypeName == null) {
            // In all cases, the type name is the type name
            // returned from 'org.objectweb.asm.Type'.
            returnTypeName = getReturnTypeAsType().getClassName();
        }

        return returnTypeName;
    }

    @Override
    public ClassInfoImpl getReturnType() {
        if (this.returnClassInfo == null) {
            this.returnClassInfo = getInfoStore().getDelayableClassInfo(getReturnTypeAsType());
        }

        return this.returnClassInfo;
    }

    //

    protected AnnotationValueImpl annotationDefaultValue;

    public void setAnnotationDefaultValue(AnnotationValueImpl annotationDefaultValue) {
        String methodName = "setAnnotationDefaultValue";

        AnnotationValueImpl priorDefaultValue = this.annotationDefaultValue;
        this.annotationDefaultValue = annotationDefaultValue;

        if ( logger.isLoggable(Level.FINEST) ) {
            logger.logp(Level.FINEST, CLASS_NAME, methodName, 
                        "[ {0} ] of [ {1} ] Updated from [ {2} ] to [ {3} ]",
                        new Object[] { getHashText(),
                                       getDeclaringClass().getHashText(),
                                       priorDefaultValue,
                                       this.annotationDefaultValue });
        }
    }

    @Override
    public AnnotationValueImpl getAnnotationDefaultValue() {
        return annotationDefaultValue;
    }

    //

    @Override
    public String getSignature() {
        return (getQualifiedName() + '(' + getParametersText() + ')');
    }

    protected String getParametersText() {
        Collection<ClassInfoImpl> useParmTypes = getParameterTypes();
        if (useParmTypes.isEmpty()) {
            return "";
        }

        StringBuffer parmsBuf = new StringBuffer();

        boolean isFirst = true;
        for (ClassInfoImpl classInfo : useParmTypes) {
            if (!isFirst) {
                parmsBuf.append(",");
                isFirst = false;
            }

            parmsBuf.append(classInfo.getName());
        }

        return parmsBuf.toString();
    }

    //

    @Override
    public void log(Logger useLogger) {
        String methodName = "log";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Method [ {0} ]", getHashText());

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Name [ {0} ]", getName());

        for (ClassInfoImpl nextParameterType : getParameterTypes()) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Parameter Type [ {0} ]", nextParameterType.getHashText());
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Return Type [ {0} ]", getReturnType().getHashText());

        for (ClassInfoImpl nextExceptionType : getExceptionTypes()) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Exception Type [ {0} ]", nextExceptionType.getHashText());
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Declaring Class [ {0} ]", getDeclaringClass().getHashText());

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Default Value [ {0} ]", getAnnotationDefaultValue());
    }
}
