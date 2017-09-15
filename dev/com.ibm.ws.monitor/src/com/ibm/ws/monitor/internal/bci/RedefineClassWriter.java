package com.ibm.ws.monitor.internal.bci;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

/***
 * Portions adapted from the ASM framework.
 * 
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2007 INRIA, France Telecom
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
public class RedefineClassWriter extends ClassWriter {

    final ClassLoader classLoader;

    public RedefineClassWriter(Class<?> clazz, ClassReader classReader, int flags) {
        this(clazz.getClassLoader(), classReader, flags);
    }

    public RedefineClassWriter(Class<?> clazz, int flags) {
        this(clazz.getClassLoader(), flags);
    }

    public RedefineClassWriter(ClassLoader classLoader, ClassReader classReader, int flags) {
        super(classReader, flags);
        this.classLoader = classLoader;
    }

    public RedefineClassWriter(ClassLoader classLoader, int flags) {
        super(flags);
        this.classLoader = classLoader;
    }

    protected String getCommonSuperClass(final String type1, final String type2) {
        Class<?> class1;
        Class<?> class2;
        try {
            class1 = Class.forName(type1.replace('/', '.'), false, classLoader);
            class2 = Class.forName(type2.replace('/', '.'), false, classLoader);
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
        if (class1.isAssignableFrom(class2)) {
            return type1;
        }
        if (class2.isAssignableFrom(class1)) {
            return type2;
        }
        if (class1.isInterface() || class2.isInterface()) {
            return Type.getInternalName(Object.class);
        } else {
            do {
                class1 = class1.getSuperclass();
            } while (class1.isAssignableFrom(class2) == false);
            return Type.getInternalName(class1);
        }
    }
}
