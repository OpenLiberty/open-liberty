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
package com.ibm.ws.repository.resolver;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.VersionRange;

import com.ibm.ws.repository.resolver.internal.LibertyVersionRange;

import wlp.lib.extract.ProductMatch;
import wlp.lib.extract.SelfExtractor;

/**
 * Contains information about a product requirement that could not be resolved.
 */
public class ProductRequirementInformation {

    /**
     * <p>The version range for this product requirement using the versioning rules of Liberty product versions.</p>
     */
    public final LibertyVersionRange versionRange;
    /** The ID of the product, will not be <code>null</code>. */
    public final String productId;
    /** The install type of the product. May be <code>null</code>. */
    public final String installType;
    /** The license type of the product. May be <code>null</code>. */
    public final String licenseType;
    /** The editions for the product, may be empty but won't be <code>null</code> */
    public final List<String> editions;

    /**
     * Construct an instance of this class supplying the relevant information.
     */
    public ProductRequirementInformation(String versionRange, String productId, String installType, String licenseType, List<String> editions) {
        super();
        this.versionRange = LibertyVersionRange.valueOf(versionRange);
        this.productId = productId;
        this.installType = installType;
        this.licenseType = licenseType;
        this.editions = editions;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((editions == null) ? 0 : editions.hashCode());
        result = prime * result + ((installType == null) ? 0 : installType.hashCode());
        result = prime * result + ((licenseType == null) ? 0 : licenseType.hashCode());
        result = prime * result + ((productId == null) ? 0 : productId.hashCode());
        result = prime * result + ((versionRange == null) ? 0 : versionRange.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProductRequirementInformation other = (ProductRequirementInformation) obj;
        if (editions == null) {
            if (other.editions != null)
                return false;
        } else if (!editions.equals(other.editions))
            return false;
        if (installType == null) {
            if (other.installType != null)
                return false;
        } else if (!installType.equals(other.installType))
            return false;
        if (licenseType == null) {
            if (other.licenseType != null)
                return false;
        } else if (!licenseType.equals(other.licenseType))
            return false;
        if (productId == null) {
            if (other.productId != null)
                return false;
        } else if (!productId.equals(other.productId))
            return false;
        if (versionRange == null) {
            if (other.versionRange != null)
                return false;
        } else if (!versionRange.equals(other.versionRange))
            return false;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ProductInformation [versionRange=" + versionRange + ", productId=" + productId + ", installType=" + installType + ", licenseType=" + licenseType + ", editions="
               + editions + "]";
    }

    /**
     * Parse an appliesTo string to produce a list of product requirements
     *
     * @param appliesTo the appliesTo string
     * @return the product requirements information
     */
    @SuppressWarnings("unchecked") // SelfExtractor has no generics
    public static List<ProductRequirementInformation> createFromAppliesTo(String appliesTo) {
        if (appliesTo == null || appliesTo.isEmpty()) {
            throw new InvalidParameterException("Applies to must be set to a valid value but is " + appliesTo);
        }
        List<ProductRequirementInformation> products = new ArrayList<ProductRequirementInformation>();
        List<ProductMatch> matchers = SelfExtractor.parseAppliesTo(appliesTo);
        for (ProductMatch match : matchers) {
            // All product must have their ID set so this should always produce a valid filter string
            String productId = match.getProductId();
            String version = match.getVersion();
            final String versionRange;
            if (version != null && version.endsWith("+")) {
                versionRange = version.substring(0, version.length() - 1);
            } else {
                if (version != null) {
                    versionRange = Character.toString(VersionRange.LEFT_CLOSED) + version + ", " + version + Character.toString(VersionRange.RIGHT_CLOSED);
                } else {
                    versionRange = null;
                }
            }
            String installType = match.getInstallType();
            String licenseType = match.getLicenseType();

            // The editions is a list of strings
            List<String> editions = match.getEditions();

            products.add(new ProductRequirementInformation(versionRange, productId, installType, licenseType, editions));
        }

        return products;
    }

}