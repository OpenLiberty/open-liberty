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

package com.ibm.ws.repository.test;

import static com.ibm.ws.lars.testutils.BasicChecks.checkCopyFields;
import static com.ibm.ws.repository.common.enums.ResourceType.PRODUCTSAMPLE;
import static com.ibm.ws.repository.common.enums.State.PUBLISHED;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.common.enums.DownloadPolicy;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.SimpleProductDefinition;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.SampleResource;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.resources.internal.SampleResourceImpl;
import com.ibm.ws.repository.resources.writeable.SampleResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;
import com.ibm.ws.repository.strategies.writeable.AddNewStrategy;

public class SampleResourceTest {

    @Rule
    public final RepositoryFixture fixture = FatUtils.getRestFixture();

    private final RepositoryConnection repoConnection = fixture.getAdminConnection();

    @Test
    public void testIsDownloadable() throws IOException {
        SampleResource sample = WritableResourceFactory.createSample(repoConnection, PRODUCTSAMPLE);
        assertEquals("Samples should be downloadable",
                     DownloadPolicy.ALL, sample.getDownloadPolicy());
    }

    @Test
    public void testCopyFields() throws IllegalArgumentException, IllegalAccessException, InstantiationException, NoSuchMethodException, SecurityException, InvocationTargetException, IOException {
        checkCopyFields(new SampleResourceImpl(repoConnection), new SampleResourceImpl(repoConnection));
    }

    /**
     * Test for defect 168805 to make sure getMatchingSamples returns both OSI and product samples.
     *
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     * @throws URISyntaxException
     */
    @Test
    public void testGetMatchingSamples() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {
        SampleResourceWritable osi = WritableResourceFactory.createSample(repoConnection, ResourceType.OPENSOURCE);
        osi.setName("OSI Sample");
        osi.setAppliesTo("abc");
        uploadResource(osi);

        SampleResourceWritable product = WritableResourceFactory.createSample(repoConnection, PRODUCTSAMPLE);
        product.setName("Product Sample");
        product.setAppliesTo("abc");
        uploadResource(product);

        Collection<SampleResource> matchingSamples = new RepositoryConnectionList(repoConnection).getMatchingSamples(new SimpleProductDefinition("abc", null, null, null, null));
        assertThat(matchingSamples, containsInAnyOrder((SampleResource) osi, product));
    }

    /**
     * This test sees if the {@link SampleResourceImpl#getMatchingSamples(String, RepositoryConnectionList)} method works.
     *
     * @throws URISyntaxException
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    @Test
    public void testFilteringSamples() throws URISyntaxException, RepositoryResourceException, RepositoryBackendException {
        // Add ESAs that would match on the 3 different attributes (and one that doesn't) and make sure we get back the right one when we filter by that attribute

        String filterString = "foo";
        SampleResourceWritable byShortName = WritableResourceFactory.createSample(repoConnection, PRODUCTSAMPLE);
        byShortName.setShortName(filterString);
        byShortName.setName(filterString);
        uploadResource(byShortName);

        SampleResourceWritable byLowerCaseShortName = WritableResourceFactory.createSample(repoConnection, PRODUCTSAMPLE);
        String byLowerCaseFilterString = "wobble";
        byLowerCaseShortName.setShortName(byLowerCaseFilterString.toUpperCase());
        byLowerCaseShortName.setName(byLowerCaseFilterString);
        uploadResource(byLowerCaseShortName);

        SampleResourceWritable noMatch = WritableResourceFactory.createSample(repoConnection, PRODUCTSAMPLE);
        noMatch.setShortName("baz");
        noMatch.setName("baz");
        uploadResource(noMatch);

        testMatch(FilterableAttribute.SHORT_NAME, filterString, byShortName);
        testMatch(FilterableAttribute.LOWER_CASE_SHORT_NAME, byLowerCaseFilterString, byLowerCaseShortName);
    }

    private void uploadResource(SampleResourceWritable sample) throws RepositoryBackendException, RepositoryResourceException {
        sample.uploadToMassive(new AddNewStrategy(PUBLISHED, PUBLISHED));
    }

    /**
     * Tests that you can run the {@link EsaResourceImpl#getMatchingEsas(FilterableAttribute, String, RepositoryConnectionList)} method correctly and get the expected result
     *
     * @param attribute
     * @param filterString
     * @param expectedResource
     * @throws RepositoryBackendException
     */
    private void testMatch(FilterableAttribute attribute, String filterString, SampleResource expectedResource) throws RepositoryBackendException {
        Collection<SampleResource> matched = new RepositoryConnectionList(repoConnection).getMatchingSamples(attribute, filterString);
        assertThat("The correct samples should be matched", matched, contains(expectedResource));
    }
}
