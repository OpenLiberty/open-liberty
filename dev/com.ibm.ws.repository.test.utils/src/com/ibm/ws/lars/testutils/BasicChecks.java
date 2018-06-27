/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.lars.testutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl.AttachmentResourceImpl;
import com.ibm.ws.repository.resources.writeable.RepositoryResourceWritable;

/**
 * This class includes several static methods for doing various checks within tests.
 */
public class BasicChecks {

    /**
     * Checks that the resource has a main attachment and that the filesize in the asset metadata
     * matches the filesize in the attachment metadata.
     *
     * @param res the resource to check
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    public void checkAttachment(RepositoryResource res) throws RepositoryBackendException, RepositoryResourceException {
        assertNotNull("No main attachment", res.getMainAttachment());
        assertEquals("Wrong file size", res.getMainAttachmentSize(), res.getMainAttachment().getSize());
    }

    /**
     * Reads the specified InputStream and returns a byte array containing all the bytes read.
     */
    public static byte[] slurp(InputStream is) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((length = is.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }

        return baos.toByteArray();
    }

    /**
     * Checks that <code>left.copyFieldsFrom(right)</code> (protected method) correctly copies
     * fields between massive resources and leaves the two resources equivalent without attachments
     * <p>
     * To do this, it tries to set all fields in <code>left</code> to specific values and then
     * copies fields from <codE>right</code>. It then checks that the two objects are equivalent.
     *
     * @param left the resource to copy into
     * @param right the resource to copy from
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IOException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InvocationTargetException
     */
    public static void checkCopyFields(RepositoryResourceImpl left,
                                       RepositoryResourceImpl right) throws IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, NoSuchMethodException, SecurityException, InvocationTargetException {

        ArrayList<String> methodsToIgnore = new ArrayList<String>();
        methodsToIgnore.add("setState");
        methodsToIgnore.add("setType");
        methodsToIgnore.add("setRepositoryConnection");
        for (Method m : left.getClass().getMethods()) {
            if (m.getName().startsWith("set")) {
                Class<?>[] parameterss = m.getParameterTypes();

                // Not a normal setter, ignore it
                if (parameterss.length != 1) {
                    continue;
                }

                if (methodsToIgnore.contains(m.getName())) {
                    continue;
                }

                Class<?> param = parameterss[0];

                Object p = null;
                if (param.isEnum()) {
                    p = param.getEnumConstants()[0];
                } else if (param.equals(Collection.class)) {
                    System.out.println("got a collection");
                    p = new ArrayList<Object>();
                } else if (param.isInterface()) {
                    continue;
                } else if (param.isPrimitive()) {
                    p = new Integer(4);
                } else if (param.equals(String.class)) {
                    p = new String("test string");
                } else {
                    p = param.newInstance();
                }

                m.invoke(left, p);
            }
        }

        Method m = null;

        try {
            m = left.getClass().getDeclaredMethod("copyFieldsFrom",
                                                  RepositoryResourceImpl.class, boolean.class);
        } catch (Exception e) {
            m = left.getClass().getSuperclass().getDeclaredMethod("copyFieldsFrom", RepositoryResourceImpl.class,
                                                                  boolean.class);
        }
        m.setAccessible(true);
        m.invoke(right, left, true);
        if (!left.equivalentWithoutAttachments(right)) {
            System.out.println("EQUIV FAILED: Left");
            left.dump(System.out);
            System.out.println("EQUIV FAILED: Right");
            right.dump(System.out);
            fail("Resources are not equivalent after copying fields");
        }

        if (!right.equivalentWithoutAttachments(left)) {
            System.out.println("EQUIV FAILED: Left");
            left.dump(System.out);
            System.out.println("EQUIV FAILED: Right");
            right.dump(System.out);
            fail("Resources are not equivalent after copying fields - though they were equivalent the other way around...");
        }
    }

    /**
     * Populates the name, provider, version and description fields of a MassiveResource with static
     * test values.
     *
     * @param res the resource to populate
     */
    public static void populateResource(RepositoryResourceWritable res) {
        res.setName("test resource");

        res.setProviderName("test provider");
        res.setProviderUrl("http://testhost/testfile");

        res.setVersion("1.0.0");
        res.setDescription("This is a test resource");
    }

    /**
     * A simple upload method which does not do any kind of replacement or state processing.
     * Suitable for using with test directory based repositories.
     *
     * @param resource the resource to upload
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    public static void simpleUpload(RepositoryResource resource) throws RepositoryBackendException, RepositoryResourceException {
        RepositoryResourceImpl resourceImpl = (RepositoryResourceImpl) resource;

        // Add the asset
        resourceImpl.addAsset();

        // ... and the attachments
        for (AttachmentResourceImpl attachment : resourceImpl.getAttachmentImpls()) {
            resourceImpl.addAttachment(attachment);
        }

        // read back any fields massive added during upload
        resourceImpl.refreshFromMassive();
    }

}
