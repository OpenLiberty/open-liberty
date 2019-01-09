/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.digest;

import com.ibm.ws.kernel.instrument.serialfilter.util.Base64UrlEncoder;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;

import static org.objectweb.asm.ClassReader.SKIP_DEBUG;

public enum Checksums {
    INSTANCE;

    private static final int CHECKSUM_LENGTH = Base64UrlEncoder.getEncodedLength(Processor.newMessageDigest().getDigestLength());

    @SuppressWarnings("SameReturnValue")
    public static Checksums getInstance() {
        return INSTANCE;
    }

    public String forClass(Class<?> cls) throws IOException {
        // get the resource name of the class file relative to the class (i.e. no package)
        String resourceName = cls.getName().replaceFirst(".*\\.", "") + ".class";
        // open the stream for the class bytes
        InputStream in = cls.getResourceAsStream(resourceName);
        // create the ASM object that drives the visitors
        ClassReader reader = new ClassReader(in);
        ClassDigester cd = new ClassDigester();
        reader.accept(cd, SKIP_DEBUG + ClassReader.SKIP_FRAMES);
        return cd.getDigestAsString();
    }

    public static boolean isValidChecksum(String checksum) {
        return (checksum.length() == CHECKSUM_LENGTH)
                && Base64UrlEncoder.isEncodedString(checksum);
    }
}
