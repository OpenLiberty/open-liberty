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
package com.ibm.ws.cdi.impl.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.cdi.interfaces.ArchiveType;
import com.ibm.ws.cdi.interfaces.CDIArchive;
import com.ibm.ws.cdi.liberty.RuntimeFactory;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class LibertyArchiveImplTest {

    Mockery context = new Mockery();

    @Test
    public void testScanForAllClassNames() throws Exception {
        final ContainerInfo containerInfo = context.mock(ContainerInfo.class);
        final Container archiveContainer = context.mock(Container.class);
        final Entry webInfClasses = context.mock(Entry.class);
        final Container subContainer = context.mock(Container.class, "classesInWebInf");

        final Entry class1 = context.mock(Entry.class, "class1");
        final Entry class2 = context.mock(Entry.class, "class2");
        final Entry class3 = context.mock(Entry.class, "foo.properties");
        final Collection<Entry> classes = new ArrayList<Entry>();
        classes.add(class1);
        classes.add(class2);
        classes.add(class3);

        context.checking(new Expectations() {
            {
                allowing(containerInfo).getName();
                will(returnValue("TestContainerName"));

                allowing(containerInfo).getContainer();
                will(returnValue(archiveContainer));

                allowing(archiveContainer).getEntry("WEB-INF/classes/");
                will(returnValue(webInfClasses));

                allowing(webInfClasses).adapt(Container.class);
                will(returnValue(subContainer));

                allowing(subContainer).iterator();
                will(returnIterator(classes));

                allowing(class1).getName();
                will(returnValue("com.ibm.Clazz1.class"));
                allowing(class1).adapt(Container.class);
                will(returnValue(null));

                allowing(class2).getName();
                will(returnValue("com.ibm.Clazz2.class"));
                allowing(class2).adapt(Container.class);
                will(returnValue(null));

                allowing(class3).getName();
                will(returnValue("foo.properties"));
                allowing(class3).adapt(Container.class);
                will(returnValue(null));
            }
        });

        RuntimeFactory factory = new RuntimeFactory(null);
        CDIArchive archive = factory.newArchive(null, containerInfo, ArchiveType.WEB_MODULE, null);

        Set<String> result = archive.getClassNames();

        Assert.assertEquals("Wrong number of elements", result.size(), 2);
        Assert.assertTrue("com.ibm.Clazz1 not found", result.contains("com.ibm.Clazz1"));
        Assert.assertTrue("com.ibm.Clazz2 not found", result.contains("com.ibm.Clazz2"));
        Assert.assertFalse("foo.properties should not have been found", result.contains("foo.properties"));
    }
}
