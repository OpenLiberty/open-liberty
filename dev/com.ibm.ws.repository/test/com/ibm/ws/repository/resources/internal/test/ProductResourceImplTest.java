/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resources.internal.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.Test;

import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.exceptions.RepositoryResourceCreationException;
import com.ibm.ws.repository.resources.internal.ProductResourceImpl;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.WlpInformation;

public class ProductResourceImplTest {

    @Test
    public void testProductVanityUrlNoAttachment() throws RepositoryResourceCreationException {
        Asset asset = new Asset();
        asset.setType(ResourceType.INSTALL);
        WlpInformation wlp = new WlpInformation();
        asset.setWlpInformation(wlp);
        asset.setName("fooName");
        ProductResourceImpl product = new ProductResourceImpl(null, asset);
        product.updateGeneratedFields(true);
        String vanity = product.getVanityURL();
        assertEquals("The vanity url wasn't what was expected", "runtimes-fooName", vanity);
    }

    @Test
    public void testProductVanityUrlWithAttachment() throws Exception {
        Asset asset = new Asset();
        asset.setType(ResourceType.INSTALL);
        WlpInformation wlp = new WlpInformation();
        asset.setWlpInformation(wlp);
        asset.setName("fooName");
        ProductResourceImpl product = new ProductResourceImpl(null, asset);

        // Need to have an attachment file, but content is unimportant
        File dummy = File.createTempFile("dummy", null);
        FileOutputStream foo = new FileOutputStream(dummy);
        foo.write(12345);
        foo.close();
        dummy.deleteOnExit();

        product.addAttachment(dummy, AttachmentType.CONTENT, "what a good-name-not this bit");
        product.updateGeneratedFields(true);

        String vanity = product.getVanityURL();
        assertEquals("The vanity url wasn't what was expected", "runtimes-what_a_good-name", vanity);
    }

}
