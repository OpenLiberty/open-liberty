/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resolver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.osgi.framework.Version;

import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;

/**
 * Utilities for testing the Massive Resolver
 */
public class ResolverTestUtils {

    /**
     * Mocks a {@link ProvisioningFeatureDefinition}.
     *
     * @param mockery
     * @param provideFeature
     * @param version
     * @param shortName
     * @return
     */
    public static ProvisioningFeatureDefinition mockSimpleFeatureDefinition(Mockery mockery, final String provideFeature, final Version version, final String shortName) {
        final ProvisioningFeatureDefinition featureDefinition = mockery.mock(ProvisioningFeatureDefinition.class, provideFeature);
        mockery.checking(new Expectations() {
            {
                allowing(featureDefinition).getSymbolicName();
                will(returnValue(provideFeature));
                allowing(featureDefinition).getIbmShortName();
                will(returnValue(shortName));
                allowing(featureDefinition).getVersion();
                will(returnValue(version));
                allowing(featureDefinition).getHeader(with(any(String.class)));
            }
        });
        return featureDefinition;
    }

    /**
     * @param id required
     * @param edition required
     * @param version required
     * @param license optional
     * @param installType optional
     * @return
     * @throws IOException
     * @throws ProductInfoParseException
     */
    public static ProductInfo createProductInfo(String id, String edition, String version, String license, String installType) throws IOException, ProductInfoParseException {
        Properties properties = new Properties();
        properties.put(ProductInfo.COM_IBM_WEBSPHERE_PRODUCTID_KEY, id);

        // Every product info needs a name but we don't use it so just set to ID
        properties.put(ProductInfo.COM_IBM_WEBSPHERE_PRODUCTNAME_KEY, id);
        properties.put(ProductInfo.COM_IBM_WEBSPHERE_PRODUCTVERSION_KEY, version);
        properties.put(ProductInfo.COM_IBM_WEBSPHERE_PRODUCTEDITION_KEY, edition);
        if (license != null) {
            properties.put("com.ibm.websphere.productLicenseType", license);
        }
        if (installType != null) {
            properties.put("com.ibm.websphere.productInstallType", installType);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        properties.store(outputStream, null);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        ProductInfo productInfo1 = ProductInfo.parseProductInfo(new InputStreamReader(inputStream), null);
        return productInfo1;
    }

}
