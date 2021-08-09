/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.info.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.Type;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.annocache.info.MethodInfo;

public class MethodInfoImpl extends InfoImpl implements MethodInfo {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.info");
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
    private List<List<AnnotationInfoImpl>> parameterAnnotations;

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

    @Override
    @Deprecated
    public List<List<? extends com.ibm.wsspi.anno.info.AnnotationInfo>> getParameterAnnotations() {
        if ( parameterAnnotations.isEmpty() ) {
            return Collections.emptyList();
        } else {
            List<List<? extends com.ibm.wsspi.anno.info.AnnotationInfo>> parmAnnotations =
                new ArrayList<List<? extends com.ibm.wsspi.anno.info.AnnotationInfo>>(parameterAnnotations.size());

            for ( List<AnnotationInfoImpl> oneParmAnnos : parameterAnnotations ) {
                parmAnnotations.add(oneParmAnnos);
            }

            return parmAnnotations;
        }
    }

    @Override
    public List<List<AnnotationInfoImpl>> getParmAnnotations() {
        return parameterAnnotations;
    }

    public void setParameterAnnotations(List<AnnotationInfoImpl>[] parmAnnos) {
        if ( (parmAnnos == null) || (parmAnnos.length == 0) ) {
            parameterAnnotations = Collections.emptyList();

        } else {
            parameterAnnotations = new ArrayList<>(parmAnnos.length);

            for ( List<AnnotationInfoImpl> oneParmAnnos : parmAnnos ) {
                if ( (oneParmAnnos == null) || oneParmAnnos.isEmpty() ) {
                    oneParmAnnos = Collections.emptyList();
                } else {
                    oneParmAnnos = new ArrayList<>(oneParmAnnos);
                }
                parameterAnnotations.add(oneParmAnnos);
            }
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

    //

    @Override
    public void log(TraceComponent useLogger) {
        if ( !useLogger.isDebugEnabled() ) {
            return;
        }

        Tr.debug(useLogger, MessageFormat.format("Method [ {0} ]", getHashText()));

        Tr.debug(useLogger, MessageFormat.format("  Name [ {0} ]", getName()));

        for (ClassInfoImpl nextParameterType : getParameterTypes()) {
            Tr.debug(useLogger, MessageFormat.format("  Parameter Type [ {0} ]", nextParameterType.getHashText()));
        }

        Tr.debug(useLogger, MessageFormat.format("  Return Type [ {0} ]", getReturnType().getHashText()));

        for (ClassInfoImpl nextExceptionType : getExceptionTypes()) {
            Tr.debug(useLogger, MessageFormat.format("  Exception Type [ {0} ]", nextExceptionType.getHashText()));
        }

        Tr.debug(useLogger, MessageFormat.format("  Declaring Class [ {0} ]", getDeclaringClass().getHashText()));

        Tr.debug(useLogger, MessageFormat.format("  Default Value [ {0} ]", getAnnotationDefaultValue()));
    }
}
