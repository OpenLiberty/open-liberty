/* ============================================================================
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ============================================================================
 */
package com.ibm.ws.sib.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public enum SerializedObjectInfoHelper {
    ;

    private static final class InspectionStream extends ObjectInputStream {
        private static final class Report extends IOException {
            Report(ObjectStreamClass info) {
                super(info == null ? "null" : info.toString());
            }
        }

        InspectionStream(byte[] data) throws IOException {
            super(createInputStream(data));
        }

        private static InputStream createInputStream(byte[] data) throws IOException {
            if (null == data) throw new Report(null);
            return new ByteArrayInputStream(data);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass osc) throws Report {
            throw new Report(osc);
        }
    }

    /**
     * Lookup the description for the class of the outermost serialized object.
     * @param data the serialized data to inspect
     * @return the description of the class
     */
    public static String getObjectInfo(byte[] data) {
        try {
            final InspectionStream is = new InspectionStream(data);
            final Object o = is.readObject();
            return (o == null) ? "null" : ObjectStreamClass.lookupAny(o.getClass()).toString();
        } catch (InspectionStream.Report report) {
            return report.getMessage();
        } catch (Exception e) {
            return "not available";
        }
    }
}
