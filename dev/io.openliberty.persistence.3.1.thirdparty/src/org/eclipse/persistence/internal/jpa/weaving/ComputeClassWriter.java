/*
 * Copyright (c) 2000, 2023 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2000, 2015 -2011 INRIA, France Telecom
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package org.eclipse.persistence.internal.jpa.weaving;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.persistence.asm.ASMFactory;
import org.eclipse.persistence.asm.AnnotationVisitor;
import org.eclipse.persistence.asm.ClassReader;
import org.eclipse.persistence.asm.ClassWriter;
import org.eclipse.persistence.asm.Opcodes;

/**
 * A ClassWriter that computes the common super class of two classes without
 * actually loading them with a ClassLoader.
 *
 * @author Eric Bruneton
 */
class ComputeClassWriter extends ClassWriter {

    private ClassLoader l = null;//getClass().getClassLoader();

    public ComputeClassWriter(ClassLoader loader, final int flags) {
        super(flags);
        setCustomClassWriter(this);
        l = loader;
    }

    @Override
    public String getCommonSuperClass(final String type1, final String type2)
    {
        try {
            ClassReader info1 = typeInfo(type1);
            ClassReader info2 = typeInfo(type2);
            if ((info1.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                if (typeImplements(type2, info2, type1)) {
                    return type1;
                } else {
                    return "java/lang/Object";
                }
            }
            if ((info2.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                if (typeImplements(type1, info1, type2)) {
                    return type2;
                } else {
                    return "java/lang/Object";
                }
            }
            StringBuilder b1 = typeAncestors(type1, info1);
            StringBuilder b2 = typeAncestors(type2, info2);
            String result = "java/lang/Object";
            int end1 = b1.length();
            int end2 = b2.length();
            while (true) {
                int start1 = b1.lastIndexOf(";", end1 - 1);
                int start2 = b2.lastIndexOf(";", end2 - 1);
                if (start1 != -1 && start2 != -1
                        && end1 - start1 == end2 - start2)
                {
                    String p1 = b1.substring(start1 + 1, end1);
                    String p2 = b2.substring(start2 + 1, end2);
                    if (p1.equals(p2)) {
                        result = p1;
                        end1 = start1;
                        end2 = start2;
                    } else {
                        return result;
                    }
                } else {
                    return result;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public void visit(int access, String name, String signature, String superName, String[] interfaces) {
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return super.visitAnnotationSuper(descriptor, visible);
    }

    @Override
    public byte[] toByteArray() {
        return super.toByteArraySuper();
    }

        @Override
    public <T> T unwrap() {
        return super.getInternal().unwrap();
    }

    /**
     * Returns the internal names of the ancestor classes of the given type.
     *
     * @param type the internal name of a class or interface.
     * @param info the ClassReader corresponding to 'type'.
     * @return a StringBuilder containing the ancestor classes of 'type',
     *         separated by ';'. The returned string has the following format:
     *         ";type1;type2 ... ;typeN", where type1 is 'type', and typeN is a
     *         direct subclass of Object. If 'type' is Object, the returned
     *         string is empty.
     * @throws IOException if the bytecode of 'type' or of some of its ancestor
     *         class cannot be loaded.
     */
    private StringBuilder typeAncestors(String type, ClassReader info)
            throws IOException
    {
        StringBuilder b = new StringBuilder();
        while (!"java/lang/Object".equals(type)) {
            b.append(';').append(type);
            type = info.getSuperName();
            info = typeInfo(type);
        }
        return b;
    }

    /**
     * Returns true if the given type implements the given interface.
     *
     * @param type the internal name of a class or interface.
     * @param info the ClassReader corresponding to 'type'.
     * @param itf the internal name of a interface.
     * @return true if 'type' implements directly or indirectly 'itf'
     * @throws IOException if the bytecode of 'type' or of some of its ancestor
     *         class cannot be loaded.
     */
    private boolean typeImplements(String type, ClassReader info, String itf)
            throws IOException
    {
        while (!"java/lang/Object".equals(type)) {
            String[] itfs = info.getInterfaces();
            for (int i = 0; i < itfs.length; ++i) {
                if (itfs[i].equals(itf)) {
                    return true;
                }
            }
            for (int i = 0; i < itfs.length; ++i) {
                if (typeImplements(itfs[i], typeInfo(itfs[i]), itf)) {
                    return true;
                }
            }
            type = info.getSuperName();
            info = typeInfo(type);
        }
        return false;
    }

    /**
     * Returns a ClassReader corresponding to the given class or interface.
     *
     * @param type the internal name of a class or interface.
     * @return the ClassReader corresponding to 'type'.
     * @throws IOException if the bytecode of 'type' cannot be loaded.
     */
    private ClassReader typeInfo(final String type) throws IOException {
        InputStream is = l.getResourceAsStream(type + ".class");
        try {
            return ASMFactory.createClassReader(is);
        } finally {
            is.close();
        }
    }
}
