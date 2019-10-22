/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.aries.buildtasks.semantic.versioning.model.decls;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.ibm.aries.buildtasks.semantic.versioning.EmptyClassVisitor;
import com.ibm.aries.buildtasks.semantic.versioning.SemanticVersioningClassVisitor;
import com.ibm.aries.buildtasks.semantic.versioning.SerialVersionClassVisitor;
import com.ibm.aries.buildtasks.utils.SemanticVersioningUtils;

public class LiveClassDeclaration extends ClassDeclaration {

    private final SerialVersionClassVisitor serialVisitor;
    private final ClassLoader jarsLoader;

    public LiveClassDeclaration(int access, String name, String signature,
                                String superName, String[] interfaces, ClassLoader loader,
                                SerialVersionClassVisitor cv) {
        super(access, name, signature, superName, interfaces);
        this.serialVisitor = cv;
        this.jarsLoader = loader;
    }

    private SerialVersionClassVisitor getSerialVisitor() {
        return serialVisitor;
    }

    /**
     * NOT FOR USE VIA XML
     *
     * @param superClass
     */
    private void getFieldsRecursively(String superClass) {

        if ((superClass != null)) {
            // load the super class of the cd
            try {
                ClassVisitor cw = new EmptyClassVisitor();
                SerialVersionClassVisitor cv = new SerialVersionClassVisitor(cw);
                SemanticVersioningClassVisitor svc = new SemanticVersioningClassVisitor(jarsLoader, cv);
                ClassReader cr = new ClassReader(jarsLoader.getResourceAsStream(superClass + SemanticVersioningUtils.classExt));
                if (cr != null) {
                    cr.accept(svc, 0);
                    ClassDeclaration cd = svc.getClassDeclaration();
                    if (cd != null) {
                        addFieldInUpperChain(cd.getFields());
                        getFieldsRecursively(cd.getSuperName());
                        for (String iface : cd.getInterfaces()) {
                            getFieldsRecursively(iface);
                        }
                    }
                }
            } catch (IOException ioe) {
                // not a problem
            }
        }
    }

    /**
     * NOT FOR USE VIA XML
     *
     * @param superClass
     */
    private void getMethodsRecursively(String superClass) {
        if ((superClass != null)) {
            // load the super class of the cd
            ClassVisitor cw = new EmptyClassVisitor();
            SerialVersionClassVisitor cv = new SerialVersionClassVisitor(cw);

            SemanticVersioningClassVisitor svc = new SemanticVersioningClassVisitor(jarsLoader, cv);
            // use URLClassLoader to load the class
            try {
                ClassReader cr = new ClassReader(jarsLoader.getResourceAsStream(superClass + SemanticVersioningUtils.classExt));
                if (cr != null) {
                    cr.accept(svc, 0);
                    ClassDeclaration cd = svc.getClassDeclaration();
                    if (cd != null) {
                        addMethodsInUpperChain(cd.getMethods());
                        getMethodsRecursively(cd.getSuperName());
                        for (String iface : cd.getInterfaces()) {
                            getMethodsRecursively(iface);
                        }
                    }
                }
            } catch (IOException ioe) {
                // not a deal
            }
        }
    }

    private Collection<String> getUpperChainRecursively(String className) {
        Collection<String> clazz = new HashSet<String>();

        if (className != null) {
            // load the super class of the cd
            ClassVisitor cw = new EmptyClassVisitor();
            SerialVersionClassVisitor cv = new SerialVersionClassVisitor(cw);

            SemanticVersioningClassVisitor svc = new SemanticVersioningClassVisitor(jarsLoader, cv);
            try {
                ClassReader cr = new ClassReader(jarsLoader.getResourceAsStream(className + SemanticVersioningUtils.classExt));
                cr.accept(svc, 0);
                clazz.add(className);
                if (svc.getClassDeclaration() != null) {
                    String superName = svc.getClassDeclaration().getSuperName();
                    className = superName;
                    clazz.addAll(getUpperChainRecursively(superName));
                    if (svc.getClassDeclaration().getInterfaces() != null) {
                        for (String iface : svc.getClassDeclaration().getInterfaces()) {
                            clazz.addAll(getUpperChainRecursively(iface));
                        }
                    }
                }
            } catch (IOException ioe) {
                // not to worry about this. terminate.
            }
        }
        return clazz;
    }

    @Override
    public Map<String, FieldDeclaration> getFieldsInUpperChain() {
        if (super.getFieldsInUpperChain().isEmpty()) {
            getFieldsRecursively(getSuperName());
            for (String ifs : getInterfaces()) {
                getFieldsRecursively(ifs);
            }
        }
        return super.getFieldsInUpperChain();
    }

    @Override
    public Map<String, Set<MethodDeclaration>> getMethodsInUpperChain() {
        if (super.getMethodsInUpperChain().isEmpty()) {
            getMethodsRecursively(getSuperName());
            for (String ifs : getInterfaces()) {
                getMethodsRecursively(ifs);
            }
        }
        return super.getMethodsInUpperChain();
    }

    @Override
    public Collection<String> getAllSupers() {
        if (super.getAllSupers().isEmpty()) {
            addSuper(getUpperChainRecursively(getSuperName()));
            for (String iface : getInterfaces()) {
                addSuper(getUpperChainRecursively(iface));
            }
        }
        return super.getAllSupers();
    }

    private Long getSerialVersionUIDFromFieldDecl(FieldDeclaration serialID, boolean allowPrivate) {
        Long serialUID = null;
        if (serialID != null) {
            if (serialID.isFinal() && serialID.isStatic() && Type.LONG_TYPE.equals(Type.getType(serialID.getDesc()))) {
                if (allowPrivate || !serialID.isPrivate()) {
                    if (serialID.getValue() != null) {
                        if (serialID.getValue() instanceof Long) {
                            serialUID = ((Long) (serialID.getValue())).longValue();
                        } else {
                            String longString = serialID.getValue().toString();
                            serialUID = Long.valueOf(longString);
                        }
                    } else {
                        serialUID = 0L;
                    }
                }
            }
        }
        return serialUID;
    }

    @Override
    public long getSerialVersionUID() {
        Long serialUID = null;

        // enums always have a svuid of 0L
        if ((getAccess() & Opcodes.ACC_ENUM) != 0) {
            serialUID = 0L;
        } else {
            FieldDeclaration serialID = getAllFields().get(SemanticVersioningUtils.SERIAL_VERSION_UTD);
            FieldDeclaration localSerialID = getFields().get(SemanticVersioningUtils.SERIAL_VERSION_UTD);
            //try locally (allow private)
            serialUID = getSerialVersionUIDFromFieldDecl(localSerialID, true);
            //nope? try super chain (ignore private)
            if (serialUID == null) {
                serialUID = getSerialVersionUIDFromFieldDecl(serialID, false);
            }
            //still nope? use the generated value
            if (serialUID == null) {
                serialUID = getSerialVisitor().getComputeSerialVersionUID();
            }
        }
        // store into super
        super.setSerialVersionUID(serialUID);
        return serialUID;
    }

    //overrides to keep findbugs happy (existing code from apichk)
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    //overrides to keep findbugs happy (existing code from apichk)
    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
