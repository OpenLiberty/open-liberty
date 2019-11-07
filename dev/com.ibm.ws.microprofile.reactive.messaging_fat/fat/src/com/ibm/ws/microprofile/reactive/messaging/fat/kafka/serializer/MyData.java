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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.serializer;

/**
 *
 */
public class MyData {

    private final String dataA;
    private final String dataB;

    public static final MyData NULL = new MyData("===NULL===", "===NULL===");

    /**
     * @param string
     */
    public MyData(String dataA, String dataB) {
        if (dataA == null) {
            throw new NullPointerException();
        }
        if (dataB == null) {
            throw new NullPointerException();
        }
        this.dataA = dataA;
        this.dataB = dataB;
    }

    public String getDataA() {
        return dataA;
    }

    public String getDataB() {
        return dataB;
    }

    @Override
    public String toString() {
        return dataA + ":" + dataB;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;
        if (!(other instanceof MyData))
            return false;
        MyData otherData = (MyData) other;
        String otherDataA = otherData.getDataA();
        String otherDataB = otherData.getDataB();
        return dataA.equals(otherDataA) && dataB.equals(otherDataB);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (dataA + dataB).hashCode();
    }

    public MyData reverse() {
        return reverse(this);
    }

    public static final MyData reverse(MyData in) {
        StringBuilder sbA = new StringBuilder(in.getDataA());
        sbA.reverse();
        StringBuilder sbB = new StringBuilder(in.getDataB());
        sbB.reverse();
        return new MyData(sbA.toString(), sbB.toString());
    }

}
