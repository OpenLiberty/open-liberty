/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resources.internal.test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;

import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.WlpInformation;

public class EsaResourceImplTest {

    @Test
    public void testAddAndSetRequireFeature() {
        EsaResourceImpl toTest = new EsaResourceImpl(null);

        // Test adding an 'old' style requireFeature
        toTest.addRequireFeature("foo");
        Collection<String> features = toTest.getRequireFeature();
        assertThat(features, hasSize(1));
        assertThat(features, hasItem("foo"));

        Map<String, Collection<String>> fwt = toTest.getRequireFeatureWithTolerates();
        assertEquals("Wrong number of features", 1, fwt.size());
        Collection<String> tolerates = fwt.get("foo");
        assertNotNull("The feature foo didn't appear in the collection", tolerates);
        assertThat(tolerates, hasSize(0));

        // Test adding a new style require feature with tolerates
        Collection<String> tolerated = Collections.singleton("tolerated");
        toTest.addRequireFeatureWithTolerates("bar", tolerated);

        Collection<String> features2 = toTest.getRequireFeature();
        assertThat(features2, hasSize(2));
        assertThat(features2, hasItem("bar"));

        Map<String, Collection<String>> fwt2 = toTest.getRequireFeatureWithTolerates();
        assertEquals("Wrong number of features", 2, fwt2.size());
        Collection<String> tolerates2 = fwt2.get("bar");
        assertNotNull("The feature bar didn't appear in the collection", tolerates2);
        assertThat(tolerates2, hasItem("tolerated"));

        // Test setting an old style feature, should overwrite the previous data.
        toTest.setRequireFeature(Collections.singleton("set_foo_feature"));
        Collection<String> features3 = toTest.getRequireFeature();
        assertThat(features3, hasSize(1));
        assertThat(features3, hasItem("set_foo_feature"));

        Map<String, Collection<String>> fwt3 = toTest.getRequireFeatureWithTolerates();
        assertEquals("Wrong number of features", 1, fwt3.size());
        Collection<String> tolerates3 = fwt3.get("set_foo_feature");
        assertNotNull("The feature set_foo_feature didn't appear in the collection", tolerates3);
        assertThat(tolerates3, hasSize(0));

        // Test setting a new style feature with tolerates info
        Collection<String> tolerated4 = Collections.singleton("tolerated4");
        Map<String, Collection<String>> something4 = Collections.singletonMap("feature4", tolerated4);
        toTest.setRequireFeatureWithTolerates(something4);
        Collection<String> features4 = toTest.getRequireFeature();
        assertThat(features4, hasSize(1));
        assertThat(features4, hasItem("feature4"));

        Map<String, Collection<String>> fwt4 = toTest.getRequireFeatureWithTolerates();
        assertEquals("The wrong number of features", 1, fwt4.size());
        Collection<String> tolerates4 = fwt4.get("feature4");
        assertNotNull("There should have been some tolerates", tolerates4);
        assertThat(tolerates4, hasItem("tolerated4"));

    }

    @Test
    public void testCopyRequireFeatureField() {
        Asset asset = new Asset();
        WlpInformation wlp = new WlpInformation();
        ArrayList<String> required = new ArrayList<String>();
        required.add("la di dah");
        wlp.setRequireFeature(required);
        asset.setWlpInformation(wlp);
        EsaResourceImpl esa = new EsaResourceImpl(null, asset);
        Collection<String> features = esa.getRequireFeature();
        assertThat(features, hasSize(1));
        // This should return one feature, from the requireFeature field
        Map<String, Collection<String>> features3 = esa.getRequireFeatureWithTolerates();
        assertEquals("There should have been 1 feature in the collection", 1, features3.size());
        Collection<String> tolerates = features3.get("la di dah");
        assertThat(tolerates, hasSize(0));
        assertNull("The requireFeatureWithTolerates field should not have been set", wlp.getRequireFeatureWithTolerates());

        // If a new require feature is added, the requireFeatureWithTolerates field
        // should also be updated.
        esa.addRequireFeature("a new feature");
        Collection<String> features2 = esa.getRequireFeature();
        assertThat(features2, hasSize(2));
        assertThat(features2, hasItem("a new feature"));
        Map<String, Collection<String>> rfwt2 = esa.getRequireFeatureWithTolerates();
        assertEquals("Wrong number of features", 2, rfwt2.size());
        assertNotNull("The expected feature was not found", rfwt2.get("a new feature"));
        assertNotNull("The expected feature was not found", rfwt2.get("la di dah"));
    }

    @Test
    public void testCopyRequireFeatureField2() {
        // This time testing by adding requirement via the addRequireFeatureWithTolerates method
        Asset asset = new Asset();
        WlpInformation wlp = new WlpInformation();
        ArrayList<String> required = new ArrayList<String>();
        required.add("la di dah");
        wlp.setRequireFeature(required);
        asset.setWlpInformation(wlp);
        EsaResourceImpl esa = new EsaResourceImpl(null, asset);
        Collection<String> features = esa.getRequireFeature();
        assertThat(features, hasSize(1));

        // If a new require feature is added, the requireFeatureWithTolerates field
        // should also be updated.
        esa.addRequireFeatureWithTolerates("a new feature", Collections.singleton("tolerated"));
        Collection<String> features2 = esa.getRequireFeature();
        assertThat(features2, hasSize(2));
        assertThat(features2, hasItem("a new feature"));
        Map<String, Collection<String>> rfwt2 = esa.getRequireFeatureWithTolerates();
        assertEquals("Wrong number of features", 2, rfwt2.size());
        assertNotNull("The expected feature was not found", rfwt2.get("la di dah"));
        Collection<String> tolerates2 = rfwt2.get("a new feature");
        assertThat(tolerates2, hasItem("tolerated"));
    }

    @Test
    public void testOverwritePreviousInfo() {
        // test that if you make a second call to addReqireFeatureWithTolerates, where
        // the second call uses the same feature name as the first, then the first entry
        // will be overwritten, even if the tolerates info is different

        EsaResourceImpl toTest = new EsaResourceImpl(null);
        toTest.addRequireFeatureWithTolerates("feature1", Collections.singleton("tolerate1"));

        // Add the same feature again, check that it overwrites
        toTest.addRequireFeatureWithTolerates("feature1", Collections.singleton("tolerate1"));
        Map<String, Collection<String>> rfwt = toTest.getRequireFeatureWithTolerates();
        assertEquals("Wrong number of features", 1, rfwt.size());
        assertThat(rfwt, Matchers.hasEntry("feature1", (Collection<String>) Collections.singleton("tolerate1")));
        Collection<String> rf = toTest.getRequireFeature();
        assertThat(rf, hasSize(1));
        assertThat(rf, hasItem("feature1"));

        // Add the same feature with different tolerates info, check that it overwrites
        toTest.addRequireFeatureWithTolerates("feature1", Collections.singleton("tolerate2"));
        rfwt = toTest.getRequireFeatureWithTolerates();
        assertEquals("Wrong number of features", 1, rfwt.size());
        assertThat(rfwt, Matchers.hasEntry("feature1", (Collection<String>) Collections.singleton("tolerate2")));
        rf = toTest.getRequireFeature();
        assertThat(rf, hasSize(1));
        assertThat(rf, hasItem("feature1"));

        // Add the same feature but using addRequireFeature instead
        // This has no tolerates info, and that should cause it to overwrite the old entry
        toTest.addRequireFeature("feature1");
        rfwt = toTest.getRequireFeatureWithTolerates();
        assertEquals("Wrong number of features", 1, rfwt.size());
        assertThat(rfwt, Matchers.hasEntry("feature1", (Collection<String>) Collections.<String> emptySet()));
        rf = toTest.getRequireFeature();
        assertThat(rf, hasSize(1));
        assertThat(rf, hasItem("feature1"));

    }

}
