/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.common.spi;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.OpcodesProxy;

public class NamespaceClassGenerator extends ClassGeneratorClassLoader implements NamespaceClassCreator {

    private static final Logger LOG = LogUtils.getL7dLogger(ClassGeneratorClassLoader.class);
    private final ASMHelper helper;

    public NamespaceClassGenerator(Bus bus) {
        super(bus);
        helper = bus.getExtension(ASMHelper.class);
    }

    @Override
    public synchronized Class<?> createNamespaceWrapperClass(Class<?> mcls, Map<String, String> map) {
        String postFix = "";

        if (mcls.getName().contains("eclipse")) {
            return createEclipseNamespaceMapper(mcls, map);
        } else if (mcls.getName().contains(".internal")) {
            postFix = "Internal";
        } else if (mcls.getName().contains("com.sun")) {
            postFix = "RI";
        //Liberty change begin
        } else if (mcls.getName().startsWith("org.glassfish")) {
            postFix = "Glassfish";
        }
        //Liberty change end

        String className = "org.apache.cxf.jaxb.NamespaceMapper";
        className += postFix;
        Class<?> cls = findClass(className, NamespaceClassCreator.class);
        Throwable t = null;
        if (cls == null) {
            try {
                byte[] bts = createNamespaceWrapperInternal(postFix);
                className = "org.apache.cxf.jaxb.NamespaceMapper" + postFix;
                return loadClass(className, NamespaceClassCreator.class, bts);
            } catch (RuntimeException ex) {
                // continue
                t = ex;
            }
        }
        if (cls == null
                && (!mcls.getName().contains(".internal.") && mcls.getName().contains("com.sun"))) {
            try {
                cls = ClassLoaderUtils.loadClass("org.apache.cxf.common.jaxb.NamespaceMapper",
                        NamespaceClassCreator.class);
            } catch (Throwable ex2) {
                // ignore
                t = ex2;
            }
        }
        LOG.log(Level.INFO, "Could not create a NamespaceMapper compatible with Marshaller class " + mcls.getName(), t);
        return cls;
    }

    private Class<?> createEclipseNamespaceMapper(Class<?> mcls, Map<String, String> map) {
        String className = "org.apache.cxf.jaxb.EclipseNamespaceMapper";
        Class<?> cls = findClass(className, NamespaceClassCreator.class);
        if (cls != null) {
            return cls;
        }
        byte[] bts = createEclipseNamespaceMapper();
        //previous code use mcls instead of NamespaceClassGenerator.class
        return loadClass(className, NamespaceClassCreator.class, bts);
    }

    /*
    // This is the "prototype" for the ASM generated class below
    public static class MapNamespacePrefixMapper2
        extends org.eclipse.persistence.internal.oxm.record.namespaces.MapNamespacePrefixMapper {

        String[] nsctxt;

        public MapNamespacePrefixMapper2(Map<String, String> foo) {
            super(foo);
        }
        public String[] getPreDeclaredNamespaceUris() {
            String[] sup = super.getPreDeclaredNamespaceUris();
            if (nsctxt == null) {
                return sup;
            }
            List<String> s = new ArrayList<>(Arrays.asList(sup));
            for (int x = 1; x < nsctxt.length; x = x + 2) {
                s.remove(nsctxt[x]);
            }
            return s.toArray(new String[s.size()]);
        }
        public void setContextualNamespaceDecls(String[] f) {
            nsctxt = f;
        }
        public String[] getContextualNamespaceDecls() {
            return nsctxt;
        }
    }
    */
    //CHECKSTYLE:OFF
    //bunch of really long ASM based methods that cannot be shortened easily
    private byte[] createEclipseNamespaceMapper() {
        OpcodesProxy Opcodes = helper.getOpCodes();
        String slashedName = "org/apache/cxf/jaxb/EclipseNamespaceMapper";
        ASMHelper.ClassWriter cw = helper.createClassWriter();
        if (cw == null) {
            return null;
        }
        String superName = "org/eclipse/persistence/internal/oxm/record/namespaces/MapNamespacePrefixMapper";
        ASMHelper.FieldVisitor fv;
        ASMHelper.MethodVisitor mv;
        cw.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER,
                slashedName, null,
                superName, null);

        cw.visitSource("EclipseNamespaceMapper.java", null);

        fv = cw.visitField(Opcodes.ACC_PRIVATE, "nsctxt", "[Ljava/lang/String;", null, null);
        fv.visitEnd();


        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/util/Map;)V",
                "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;)V", null);
        mv.visitCode();
        ASMHelper.Label l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                superName, "<init>", "(Ljava/util/Map;)V", false);
        ASMHelper.Label l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitInsn(Opcodes.RETURN);
        ASMHelper.Label l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitMaxs(0, 0);
        mv.visitEnd();


        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "setContextualNamespaceDecls", "([Ljava/lang/String;)V",
                null, null);
        mv.visitCode();
        l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(47, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, slashedName, "nsctxt", "[Ljava/lang/String;");
        l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitLineNumber(48, l1);
        mv.visitInsn(Opcodes.RETURN);
        l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this", "L" + slashedName + ";", null, l0, l2, 0);
        mv.visitLocalVariable("contextualNamespaceDecls", "[Ljava/lang/String;", null, l0, l2, 1);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getContextualNamespaceDecls", "()[Ljava/lang/String;", null, null);
        mv.visitCode();
        l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(51, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, slashedName, "nsctxt", "[Ljava/lang/String;");
        mv.visitInsn(Opcodes.ARETURN);
        l1 = helper.createLabel();

        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "L" + slashedName + ";", null, l0, l1, 0);

        mv.visitMaxs(0, 0);
        mv.visitEnd();


        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getPreDeclaredNamespaceUris", "()[Ljava/lang/String;", null, null);
        mv.visitCode();
        l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(1036, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                superName,
                "getPreDeclaredNamespaceUris", "()[Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 1);
        l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitLineNumber(1037, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, slashedName, "nsctxt", "[Ljava/lang/String;");
        l2 = helper.createLabel();
        mv.visitJumpInsn(Opcodes.IFNONNULL, l2);
        ASMHelper.Label l3 = helper.createLabel();
        mv.visitLabel(l3);
        mv.visitLineNumber(1038, l3);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(l2);
        mv.visitLineNumber(1040, l2);
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] {"[Ljava/lang/String;"}, 0, null);
        mv.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "asList",
                "([Ljava/lang/Object;)Ljava/util/List;", false);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>",
                "(Ljava/util/Collection;)V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        ASMHelper.Label l4 = helper.createLabel();
        mv.visitLabel(l4);
        mv.visitLineNumber(1041, l4);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitVarInsn(Opcodes.ISTORE, 3);
        ASMHelper.Label l5 = helper.createLabel();
        mv.visitLabel(l5);
        ASMHelper.Label l6 = helper.createLabel();
        mv.visitJumpInsn(Opcodes.GOTO, l6);
        ASMHelper.Label l7 = helper.createLabel();
        mv.visitLabel(l7);
        mv.visitLineNumber(1042, l7);
        mv.visitFrame(Opcodes.F_APPEND, 2, new Object[] {"java/util/List", Opcodes.INTEGER}, 0, null);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, slashedName, "nsctxt", "[Ljava/lang/String;");
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitInsn(Opcodes.AALOAD);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "remove", "(Ljava/lang/Object;)Z", true);
        mv.visitInsn(Opcodes.POP);
        ASMHelper.Label l8 = helper.createLabel();
        mv.visitLabel(l8);
        mv.visitLineNumber(1041, l8);
        mv.visitIincInsn(3, 2);
        mv.visitLabel(l6);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD,
                slashedName,
                "nsctxt", "[Ljava/lang/String;");
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitJumpInsn(Opcodes.IF_ICMPLT, l7);
        ASMHelper.Label l9 = helper.createLabel();
        mv.visitLabel(l9);
        mv.visitLineNumber(1044, l9);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "size", "()I", true);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List",
                "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", true);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/String;");
        mv.visitInsn(Opcodes.ARETURN);
        ASMHelper.Label l10 = helper.createLabel();
        mv.visitLabel(l10);
        mv.visitLocalVariable("this", "L" + slashedName + ";",
                null, l0, l10, 0);
        mv.visitLocalVariable("sup", "[Ljava/lang/String;", null, l1, l10, 1);
        mv.visitLocalVariable("s", "Ljava/util/List;", "Ljava/util/List<Ljava/lang/String;>;", l4, l10, 2);
        mv.visitLocalVariable("x", "I", null, l5, l9, 3);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();

        byte[] bts = cw.toByteArray();
        return bts;
    }

    private byte[] createNamespaceWrapperInternal(String postFix) {

        //Liberty change begin
        String superName = "Glassfish".equals(postFix) ? 
                        "org/glassfish/jaxb/runtime/marshaller/NamespacePrefixMapper" : 
                        ("com/sun/xml/"
                        + ("RI".equals(postFix) ? "" : "internal/")
                        + "bind/marshaller/NamespacePrefixMapper");
        //Liberty change end
        String postFixedName = "org/apache/cxf/jaxb/NamespaceMapper" + postFix;
        ASMHelper.ClassWriter cw = helper.createClassWriter();
        if (cw == null) {
            return null;
        }
        ASMHelper.FieldVisitor fv;
        ASMHelper.MethodVisitor mv;
        OpcodesProxy Opcodes= helper.getOpCodes();
        cw.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER,
                postFixedName, null,
                superName, null);

        cw.visitSource("NamespaceMapper.java", null);

        fv = cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL,
                "nspref", "Ljava/util/Map;",
                "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;", null);
        fv.visitEnd();

        fv = cw.visitField(Opcodes.ACC_PRIVATE, "nsctxt", "[Ljava/lang/String;", null, null);
        fv.visitEnd();

        fv = cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC,
                "EMPTY_STRING", "[Ljava/lang/String;", null, null);
        fv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        ASMHelper.Label l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(30, l0);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
        mv.visitFieldInsn(Opcodes.PUTSTATIC, postFixedName, "EMPTY_STRING", "[Ljava/lang/String;");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                "(Ljava/util/Map;)V",
                "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;)V", null);
        mv.visitCode();
        l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(32, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        ASMHelper.Label l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitLineNumber(29, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETSTATIC, postFixedName, "EMPTY_STRING", "[Ljava/lang/String;");
        mv.visitFieldInsn(Opcodes.PUTFIELD, postFixedName, "nsctxt", "[Ljava/lang/String;");
        ASMHelper.Label l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitLineNumber(33, l2);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, postFixedName, "nspref", "Ljava/util/Map;");
        ASMHelper.Label l3 = helper.createLabel();
        mv.visitLabel(l3);
        mv.visitLineNumber(34, l3);
        mv.visitInsn(Opcodes.RETURN);
        ASMHelper.Label l4 = helper.createLabel();
        mv.visitLabel(l4);
        mv.visitLocalVariable("this", "L" + postFixedName + ";", null, l0, l4, 0);
        mv.visitLocalVariable("nspref",
                "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;",
                l0, l4, 1);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getPreferredPrefix",
                "(Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;",
                null, null);
        mv.visitCode();
        l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(39, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, postFixedName, "nspref", "Ljava/util/Map;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map",
                "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
        mv.visitVarInsn(Opcodes.ASTORE, 4);
        l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitLineNumber(40, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        l2 = helper.createLabel();
        mv.visitJumpInsn(Opcodes.IFNULL, l2);
        l3 = helper.createLabel();
        mv.visitLabel(l3);
        mv.visitLineNumber(41, l3);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(l2);
        mv.visitLineNumber(43, l2);
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] {"java/lang/String"}, 0, null);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARETURN);
        l4 = helper.createLabel();
        mv.visitLabel(l4);
        mv.visitLocalVariable("this", "L" + postFixedName + ";", null, l0, l4, 0);
        mv.visitLocalVariable("namespaceUri", "Ljava/lang/String;", null, l0, l4, 1);
        mv.visitLocalVariable("suggestion", "Ljava/lang/String;", null, l0, l4, 2);
        mv.visitLocalVariable("requirePrefix", "Z", null, l0, l4, 3);
        mv.visitLocalVariable("prefix", "Ljava/lang/String;", null, l1, l4, 4);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "setContextualNamespaceDecls", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(47, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, postFixedName, "nsctxt", "[Ljava/lang/String;");
        l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitLineNumber(48, l1);
        mv.visitInsn(Opcodes.RETURN);
        l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this", "L" + postFixedName + ";", null, l0, l2, 0);
        mv.visitLocalVariable("contextualNamespaceDecls", "[Ljava/lang/String;", null, l0, l2, 1);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getContextualNamespaceDecls", "()[Ljava/lang/String;", null, null);
        mv.visitCode();
        l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(51, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, postFixedName, "nsctxt", "[Ljava/lang/String;");
        mv.visitInsn(Opcodes.ARETURN);
        l1 = helper.createLabel();

        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "L" + postFixedName + ";", null, l0, l1, 0);

        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();
    }
    //CHECKSTYLE:ON
}
