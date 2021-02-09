/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.common;

import java.nio.ByteBuffer;

/**
 * BinaryFormater
 * 
 * @author Rashmi Hunt
 */
public class BinaryFormater {

    String data = "Initial String";

    public BinaryFormater(String data) {
        this.data = data;
    }

    public static BinaryFormater doDecoding(ByteBuffer byteBuffer) {
        return new BinaryFormater(new String(byteBuffer.array()));
    }

    public static ByteBuffer doEncoding(BinaryFormater formater) {
        String str = formater.getData();
        //TCK case where encoder throws RuntimeException
        if (str.equals("EXCEPTION")) {
            throw new IllegalStateException("EXCEPTION THROWN INTENTIONALLY FROM TEST CASE");
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(str.getBytes());
        return byteBuffer;
    }

    public String getData() {
        return this.data;
    }
}
