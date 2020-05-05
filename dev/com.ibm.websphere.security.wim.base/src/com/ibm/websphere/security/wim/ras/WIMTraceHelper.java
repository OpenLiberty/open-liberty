/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim.ras;

import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.security.wim.SchemaConstants;

@Trivial
public class WIMTraceHelper {

    /**
     * The string used for display password in the trace. For security reasons, password can not be printed out in the trace or message.
     */
    private final static String DUMMY_VALUE = "*****";

    /** Cache for {@link JAXBContext}s by class. */
    private final static Map<Class<?>, JAXBContext> JAXB_CONTEXT_CACHE = new HashMap<Class<?>, JAXBContext>();

    /**
     * Marshal a JAXB object and obtain a human readable formatted XML string useful for log output. Note that this String can not be
     * unmarshalled as some of the XML has been modified for readability and obfuscated for confidentiality.
     *
     * @param jaxbObject The JAXB object to trace.
     * @return A human readable XML string that is not suitable for unmarshalling.
     */
    @Trivial // Never enable entry / exit trace - will cause StackOverflowErrors
    public static String traceJaxb(final Object jaxbObject) {
        return AccessController.doPrivileged(new JaxbContextPrivilegedAction(jaxbObject));
    }

    /**
     * Return a string that is equivalent to an Object[].
     *
     * @param array The Object[] to convert to a string.
     * @return The string representation of the array.
     */
    public static String printObjectArray(Object[] array) {
        if (array == null)
            return null;
        StringBuffer result = new StringBuffer();

        result.append("[");
        for (int i = 0; i < array.length; i++) {
            Object obj = array[i];
            if (obj != null) {
                if (obj instanceof Object[]) {
                    result.append(printObjectArray((Object[]) obj));
                } else {
                    result.append(obj);
                }
            } else {
                result.append("null");
            }
            if (i != array.length - 1)
                result.append(", ");
        }
        result.append("]");
        return result.toString();
    }

    /**
     * Return a string that is equivalent to an a primitive object array.
     *
     * @param obj The primitive array to convert to a string.
     * @return The string representation of the array.
     */
    public static String printPrimitiveArray(Object obj) {
        if (obj == null)
            return null;

        Object[] oArray = null;

        if (obj instanceof byte[]) {
            byte[] pArray = (byte[]) obj;
            oArray = new Byte[pArray.length];
            for (int idx = 0; idx < pArray.length; idx++) {
                oArray[idx] = pArray[idx];
            }
        } else if (obj instanceof char[]) {
            char[] pArray = (char[]) obj;
            oArray = new Character[pArray.length];
            for (int idx = 0; idx < pArray.length; idx++) {
                oArray[idx] = pArray[idx];
            }
        } else if (obj instanceof double[]) {
            double[] pArray = (double[]) obj;
            oArray = new Double[pArray.length];
            for (int idx = 0; idx < pArray.length; idx++) {
                oArray[idx] = pArray[idx];
            }
        } else if (obj instanceof float[]) {
            float[] pArray = (float[]) obj;
            oArray = new Float[pArray.length];
            for (int idx = 0; idx < pArray.length; idx++) {
                oArray[idx] = pArray[idx];
            }
        } else if (obj instanceof int[]) {
            int[] pArray = (int[]) obj;
            oArray = new Integer[pArray.length];
            for (int idx = 0; idx < pArray.length; idx++) {
                oArray[idx] = pArray[idx];
            }
        } else if (obj instanceof short[]) {
            short[] pArray = (short[]) obj;
            oArray = new Short[pArray.length];
            for (int idx = 0; idx < pArray.length; idx++) {
                oArray[idx] = pArray[idx];
            }
        } else if (obj instanceof long[]) {
            long[] pArray = (long[]) obj;
            oArray = new Long[pArray.length];
            for (int idx = 0; idx < pArray.length; idx++) {
                oArray[idx] = pArray[idx];
            }
        } else if (obj instanceof boolean[]) {
            boolean[] pArray = (boolean[]) obj;
            oArray = new Boolean[pArray.length];
            for (int idx = 0; idx < pArray.length; idx++) {
                oArray[idx] = pArray[idx];
            }
        }

        return printObjectArray(oArray);
    }

    @Trivial
    private static class JaxbContextPrivilegedAction implements PrivilegedAction<String> {
        private final Object jaxbObject;

        JaxbContextPrivilegedAction(Object jaxbObject) {
            this.jaxbObject = jaxbObject;
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public String run() {
            try {
                /*
                 * Creating a new context can be expensive. Cache them off.
                 */
                JAXBContext context = JAXB_CONTEXT_CACHE.get(jaxbObject.getClass());
                if (context == null) {
                    context = JAXBContext.newInstance(jaxbObject.getClass());
                    JAXB_CONTEXT_CACHE.put(jaxbObject.getClass(), context);
                }

                /*
                 * Get a new Marshaller instance and configure the output format.
                 */
                Marshaller marshaller = context.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

                /*
                 * Get the qualified name for the class.
                 */
                QName qname = new QName(SchemaConstants.WIM_NS_URI, jaxbObject.getClass().getSimpleName());

                StringWriter sw = new StringWriter();
                marshaller.marshal(new JAXBElement(qname, jaxbObject.getClass(), null, jaxbObject), sw);

                /*
                 * I would use an XmlAdapter on the password field in LoginAccount, but it changes the type to xs:string and it seems confusing. Will
                 * just manually modify the string here until I have a better idea.
                 */
                String output = sw.toString().replaceAll("<wim:password>.+</wim:password>", "<wim:password>" + DUMMY_VALUE + "</wim:password>");
                return output;
            } catch (JAXBException e) {
                /*
                 * If this occurs it is most likely an issue with our JAXB structures.
                 */
                return "WIMTraceHelper.traceJaxb(): Unable to marshal instance of class '" + jaxbObject.getClass().getName() + "': " + e;
            }
        }

    }
}