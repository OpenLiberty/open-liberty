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
package com.ibm.aries.buildtasks.semantic.versioning;

import java.lang.reflect.Modifier;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.ibm.aries.buildtasks.semantic.versioning.model.decls.ClassDeclaration;
import com.ibm.aries.buildtasks.semantic.versioning.model.decls.FieldDeclaration;
import com.ibm.aries.buildtasks.semantic.versioning.model.decls.LiveClassDeclaration;
import com.ibm.aries.buildtasks.semantic.versioning.model.decls.MethodDeclaration;
import com.ibm.aries.buildtasks.utils.SemanticVersioningUtils;

public class SemanticVersioningClassVisitor extends ClassVisitor {

    private LiveClassDeclaration classDeclaration;
    private boolean classNeedsVisit = false;
    private ClassLoader loader = null;

    public SemanticVersioningClassVisitor(ClassLoader newJarLoader,
                                          SerialVersionClassVisitor cv) {
        super(Opcodes.ASM7, cv);
        this.loader = newJarLoader;
        this.cv = cv;
    }

    public SemanticVersioningClassVisitor(ClassLoader newJarLoader) {
        super(Opcodes.ASM7);
        this.loader = newJarLoader;
    }

    public ClassDeclaration getClassDeclaration() {
        return classDeclaration;
    }

    private boolean needsVisit(int access) {
        return (Modifier.isPublic(access) || Modifier.isProtected(access));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.objectweb.asm.ClassAdapter#visit(int, int, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String)
     */
    // visit the header of the class
    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        // only interested in public class
        if (cv != null) {
            cv.visit(version, access, name, signature, superName, interfaces);
        }
        if (needsVisit(access)) {
            classDeclaration = new LiveClassDeclaration(access, name, signature, superName, interfaces, loader, (SerialVersionClassVisitor) cv);
            classNeedsVisit = true;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.objectweb.asm.ClassAdapter#visitField(int, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.Object)
     *
     * Grab all protected or public fields
     */
    @Override
    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value) {
        if (cv != null) {
            cv.visitField(access, name, desc, signature, value);
        }
        if (classNeedsVisit && (needsVisit(access) || "serialVersionUID".equals(name))) {
            FieldDeclaration fd = new FieldDeclaration(access, name, desc, signature, value);
            classDeclaration.addFields(fd);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String[]) Get all
     * non-private methods
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {

        if (cv != null) {
            cv.visitMethod(access, name, desc, signature, exceptions);
        }
        if (classNeedsVisit && needsVisit(access) && (!SemanticVersioningUtils.CLINIT.equals(name))) {
            //Do not process synthetic 0x1000, or bridge methods.
            //
            //asm doesn't offer constants for these because it says
            //they have different meanings for fields & methods.
            //since we're inside visit method, it's safe for use to use
            //these meanings here.
            if ((access & 0x1040) == 0) {
                MethodDeclaration md = new MethodDeclaration(access, name, desc, signature, exceptions);
                classDeclaration.addMethods(md);
            }
        }
        return null;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
        return null;
    }

    @Override
    public void visitAttribute(Attribute arg0) {
        // no-op
    }

    @Override
    public void visitEnd() {
        // no-op

    }

    @Override
    public void visitInnerClass(String name, String outerName,
                                String innerName, int access) {
        // no-op
        // The inner class will be scanned on its own. However, the method level
        // class will be excluded, as they won't be public or protected.

    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        // no op

    }

    @Override
    public void visitSource(String arg0, String arg1) {
        // no-op

    }

}
