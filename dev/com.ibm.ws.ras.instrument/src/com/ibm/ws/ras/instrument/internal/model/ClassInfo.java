/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.objectweb.asm.Type;

public class ClassInfo {

    private final String className;
    private final String internalClassName;
    private final String baseName;
    private final String packageName;
    private final String internalPackageName;
    private boolean sensitive;
    private boolean trivial;
    private TraceOptionsData traceOptionsData;
    private Set<FieldInfo> fields = new LinkedHashSet<FieldInfo>();
    private Set<MethodInfo> methods = new LinkedHashSet<MethodInfo>();

    public ClassInfo(String className) {
        this.className = className.replaceAll("/", "\\.");
        this.internalClassName = className.replaceAll("\\.", "/");
        this.baseName = internalClassName.replaceAll("^.*/", "");
        this.packageName = this.className.replaceAll("\\.[^\\.]+$", "");
        this.internalPackageName = internalClassName.replaceAll("/[^/]+$", "");
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public boolean isTrivial() {
        return trivial;
    }

    public void setTrivial(boolean trivial) {
        this.trivial = trivial;
    }

    public TraceOptionsData getTraceOptionsData() {
        return traceOptionsData;
    }

    public void setTraceOptionsData(TraceOptionsData traceOptionsData) {
        this.traceOptionsData = traceOptionsData;
    }

    public String getBaseName() {
        return baseName;
    }

    public String getClassName() {
        return className;
    }

    public String getInternalClassName() {
        return internalClassName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getInternalPackageName() {
        return internalPackageName;
    }

    public Set<FieldInfo> getFieldInfoSet() {
        return fields;
    }

    public void addFieldInfo(FieldInfo fieldInfo) {
        fields.add(fieldInfo);
    }

    public FieldInfo getDeclaredLoggerField() {
        for (FieldInfo fi : fields) {
            if (fi.isLoggerField()) {
                return fi;
            }
        }
        return null;
    }

    public FieldInfo getDeclaredFieldByName(String name) {
        for (FieldInfo fi : fields) {
            if (fi.getFieldName().equals(name)) {
                return fi;
            }
        }
        return null;
    }

    public Set<FieldInfo> getDeclaredFieldsByType(Type fieldType) {
        Set<FieldInfo> fieldSet = new LinkedHashSet<FieldInfo>();
        for (FieldInfo fi : fields) {
            if (fi.getFieldDescriptor().equals(fieldType.getDescriptor())) {
                fieldSet.add(fi);
            }
        }
        return fieldSet;
    }

    public Set<MethodInfo> getMethodInfoSet() {
        return methods;
    }

    public void addMethodInfo(MethodInfo methodInfo) {
        methods.add(methodInfo);
    }

    public Set<MethodInfo> getDeclaredMethodsByName(String name) {
        Set<MethodInfo> methodSet = new LinkedHashSet<MethodInfo>();
        for (MethodInfo mi : methods) {
            if (mi.getMethodName().equals(name)) {
                methodSet.add(mi);
            }
        }
        return methodSet;
    }

    public MethodInfo getDeclaredMethod(String name, String descriptor) {
        for (MethodInfo mi : getDeclaredMethodsByName(name)) {
            if (mi.getMethodDescriptor().equals(descriptor)) {
                return mi;
            }
        }
        return null;
    }

    public void updateDefaultValuesFromPackageInfo(PackageInfo packageInfo) {
        if (!trivial && packageInfo != null) {
            trivial = packageInfo.isTrivial();
        }
        if (traceOptionsData == null) {
            traceOptionsData = packageInfo != null ? packageInfo.getTraceOptionsData() : new TraceOptionsData();
        }
        for (FieldInfo fi : fields) {
            fi.updateDefaultValuesFromClassInfo(this);
        }
        for (MethodInfo mi : methods) {
            mi.updateDefaultValuesFromClassInfo(this);
        }
    }

    public void overrideValuesFromExplicitClassInfo(ClassInfo ci) {
        if (ci.isSensitive()) {
            setSensitive(true);
        }
        if (ci.isTrivial()) {
            setTrivial(true);
        }
        TraceOptionsData optionsData = ci.getTraceOptionsData();
        if (optionsData != null) {
            setTraceOptionsData(optionsData);
        }
        for (FieldInfo fi : ci.fields) {
            FieldInfo field = getDeclaredFieldByName(fi.getFieldName());
            if (field != null && field.getFieldDescriptor().equals(fi.getFieldDescriptor())) {
                fields.remove(field);
                fields.add(fi);
            }
        }
        for (MethodInfo mi : ci.methods) {
            MethodInfo method = getDeclaredMethod(mi.getMethodName(), mi.getMethodDescriptor());
            if (method != null) {
                methods.remove(method);
                methods.add(mi);
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(";className=").append(className);
        sb.append(",internalClassName=").append(internalClassName);
        sb.append(",baseName=").append(baseName);
        sb.append(",sensitive=").append(sensitive);
        sb.append(",trivial=").append(trivial);
        sb.append(",traceOptionsData=").append(traceOptionsData);
        sb.append(",fields=").append(fields);
        sb.append(",methods=").append(methods);
        return sb.toString();
    }
}
