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

package com.ibm.ws.repository.resolver.internal.resource;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.VersionRange;
import org.osgi.resource.Requirement;

import wlp.lib.extract.ProductMatch;
import wlp.lib.extract.SelfExtractor;

import com.ibm.ws.repository.resolver.ProductRequirementInformation;
import com.ibm.ws.repository.resolver.internal.namespace.ProductNamespace;

/**
 * This is a requirement on a product edition/version.
 */
public class ProductRequirement extends RequirementImpl implements Requirement {

    private final Map<String, String> directives;

    // Information about this requirement, handy if we need to construct an exception and print which products we've tried.
    private final List<ProductRequirementInformation> productInformation;

    /**
     * Creates a new instance that just has a dependency on the version range of the installed product.
     * 
     * @param range
     */
    public ProductRequirement(VersionRange range) {
        super(range.toString());
        this.productInformation = Collections.singletonList(new ProductRequirementInformation(range.toString(), null, null, null, null));
        this.directives = this.createDirectives(range.toFilterString(ProductNamespace.CAPABILITY_VERSION_ATTRIBUTE));
    }

    /**
     * Creates an unmodifiable map of directives with a single directive - the filter string.
     * 
     * @param filter the filter string to put in the directive
     */
    private Map<String, String> createDirectives(String filter) {
        Map<String, String> directives = new HashMap<String, String>();
        directives.put(ProductNamespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
        return Collections.unmodifiableMap(directives);
    }

    /**
     * <p>Constructs a requirement on a product from an applies to string. It is a comma separated list of items in the form:</p>
     * <code>{product_id}; productVersion={product_version}; productInstallType={product_install_type}; productEdition={product_edition(s)}</code></p>
     * Note that the {product_edition(s)} can be either a single edition or a comma separated list of editions enclosed in quotes. For example the following is a valid
     * applies to string:</p>
     * <code>com.ibm.websphere.appserver; productVersion=8.5.next.beta; productInstallType=Archive; productEdition="BASE,DEVELOPERS,EXPRESS,ND"</code>
     * 
     * @param appliesTo the applies to string to parse, must not be null or empty
     */
    public ProductRequirement(String appliesTo) {
        super(appliesTo);
        if (appliesTo == null || appliesTo.isEmpty()) {
            throw new InvalidParameterException("Applies to must be set to a valid value but is " + appliesTo);
        }
        StringBuilder filterString = new StringBuilder("(|");
        List<ProductRequirementInformation> products = new ArrayList<ProductRequirementInformation>();
        List<ProductMatch> matchers = SelfExtractor.parseAppliesTo(appliesTo);
        for (ProductMatch match : matchers) {
            // All product must have their ID set so this should always produce a valid filter string
            filterString.append("(&");
            String productId = match.getProductId();
            appendAttributeEqualityCheck(filterString, ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE, productId);
            String version = match.getVersion();
            final String versionRange;
            if (version != null && version.endsWith("+")) {
                versionRange = version.substring(0, version.length() - 1);
                appendAttributeCheck(filterString, ProductNamespace.CAPABILITY_VERSION_ATTRIBUTE, ">=", versionRange);
            } else {
                appendAttributeEqualityCheck(filterString, ProductNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
                if (version != null) {
                    versionRange = Character.toString(VersionRange.LEFT_CLOSED) + version + ", " + version + Character.toString(VersionRange.RIGHT_CLOSED);
                } else {
                    versionRange = null;
                }
            }
            String installType = match.getInstallType();
            appendAttributeEqualityCheck(filterString, ProductNamespace.CAPABILITY_INSTALL_TYPE_ATTRIBUTE, installType);
            String licenseType = match.getLicenseType();
            appendAttributeEqualityCheck(filterString, ProductNamespace.CAPABILITY_LICENSE_TYPE_ATTRIBUTE, licenseType);

            // The editions is a list of strings
            List<String> editions = match.getEditions();
            if (editions != null && !editions.isEmpty()) {
                filterString.append("(|");
                for (String edition : editions) {
                    appendAttributeEqualityCheck(filterString, ProductNamespace.CAPABILITY_EDITION_ATTRIBUTE, edition);
                }
                filterString.append(")");
            }

            filterString.append(")");
            products.add(new ProductRequirementInformation(versionRange, productId, installType, licenseType, editions));
        }
        filterString.append(")");
        this.productInformation = products;
        this.directives = this.createDirectives(filterString.toString());
    }

    /**
     * <p>This method will append an attribute equality check to the filterString in the form:</p>
     * <p>"(<code>attributeName</code>=<code>value</code>)"</p>
     * <p>If the value is not null or empty</p>
     * 
     * 
     * @param filterString
     * @param attributeName
     * @param value
     */
    private void appendAttributeEqualityCheck(StringBuilder filterString, String attributeName, String value) {
        appendAttributeCheck(filterString, attributeName, "=", value);
    }

    /**
     * <p>This method will append an attribute check using the supplied operator to the filterString in the form:</p>
     * <p>"(<code>{attributeName}{operator}{value}</code>)"</p>
     * <p>If the value is not null or empty</p>
     * 
     * @param operator
     * @param filterString
     * @param attributeName
     * @param value
     */
    private void appendAttributeCheck(StringBuilder filterString, String attributeName, String operator, String value) {
        if (value != null && !value.isEmpty()) {
            filterString.append("(").append(attributeName).append(operator).append(value).append(")");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Requirement#getNamespace()
     */
    @Override
    public String getNamespace() {
        return ProductNamespace.PRODUCT_NAMESPACE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Requirement#getDirectives()
     */
    @Override
    public Map<String, String> getDirectives() {
        return this.directives;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.resource.Requirement#getAttributes()
     */
    @Override
    public Map<String, Object> getAttributes() {
        return Collections.emptyMap();
    }

    /**
     * Returns a list of the products that this requirement references.
     * 
     * @return the productInformation
     */
    public List<ProductRequirementInformation> getProductInformation() {
        return productInformation;
    }

}
