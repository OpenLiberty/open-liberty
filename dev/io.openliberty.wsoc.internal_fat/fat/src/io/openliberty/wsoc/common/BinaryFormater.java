/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty 20.0.0.6
 * Copyright 2014, 2015 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
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
