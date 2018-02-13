/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resolver.internal;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.resolver.internal.resource.FeatureResource;
import com.ibm.ws.repository.resolver.internal.resource.IFixResource;
import com.ibm.ws.repository.resolver.internal.resource.ResourceImpl;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.resources.internal.IfixResourceImpl;

/**
 *
 */
public class FixFeatureComparatorTest {

    @Test
    public void testComparator() {
        List<ResourceImpl> testObject = new ArrayList<ResourceImpl>();
        EsaResourceImpl esa1 = new EsaResourceImpl(new RestRepositoryConnection("a", "b", "c", "d", "e", "f"));
        esa1.setProvideFeature("a");
        testObject.add(FeatureResource.createInstance(esa1));
        IfixResourceImpl iFix = new IfixResourceImpl(new RestRepositoryConnection("a", "b", "c", "d", "e", "f"));
        iFix.setProvideFix(Collections.singleton("foo"));
        testObject.add(IFixResource.createInstance(iFix));
        EsaResourceImpl esa2 = new EsaResourceImpl(new RestRepositoryConnection("a", "b", "c", "d", "e", "f"));
        esa2.setProvideFeature("b");
        testObject.add(FeatureResource.createInstance(esa2));
        Collections.sort(testObject, new FixFeatureComparator());
        Iterator<ResourceImpl> iterator = testObject.iterator();
        assertTrue("The fix should come out of the iterator first", iterator.next() instanceof IFixResource);
        assertTrue("The features should come out of the iterator last", iterator.next() instanceof FeatureResource);
        assertTrue("The features should come out of the iterator last but the second one wasn't", iterator.next() instanceof FeatureResource);
    }
}
