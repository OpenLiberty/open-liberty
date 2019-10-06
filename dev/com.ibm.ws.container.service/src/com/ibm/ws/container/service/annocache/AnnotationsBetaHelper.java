/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.container.service.annocache;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class AnnotationsBetaHelper {
    private static final TraceComponent tc = Tr.register(AnnotationsBetaHelper.class);

    //

    /**
     * Setting of whether this is a liberty beta product.  That is, if the
     * product edition is set to "EARLY_ACCESS".  If available, the product
     * "com.ibm.websphere.appserver" is tested.  If that product is not
     * available, the product "io.openliberty" is tested instead.
     */
    public static final boolean IS_LIBERTY_BETA_PRODUCT = true;
        // setIsLibertyBetaProduct();

    // TODO: This is quite expensive, especially if there are multiple
    //       product files.  However, this is temporary code which will
    //       be removed after the beta period.

    public static final String OPEN_LIBERTY_PRODUCT_ID = "io.openliberty";
    public static final String OPEN_LIBERTY_CD_PRODUCT_ID = "com.ibm.websphere.appserver";

    public static final String EARLY_ACCESS = "EARLY_ACCESS";

    /**
     * Determine if the current product is a beta edition product.  That is,
     * if the product edition is set to "EARLY_ACCESS".  If available, the
     * product "com.ibm.websphere.appserver" is tested.  If that product is
     * not available, the product "io.openliberty" is tested instead.
     *
     * @return True or false telling if the current product is a beta
     *     edition product.
     */
    public static boolean setIsLibertyBetaProduct() {
        Map<String, ? extends ProductInfo> allProductInfo;

        try {
            allProductInfo = ProductInfo.getAllProductInfo();
        } catch ( Exception e ) {
            allProductInfo = null;
            // FFDC
        }

        boolean isBeta;
        String isBetaReason;

        if ( allProductInfo == null ) {
            isBeta = false;
            isBetaReason = "Failed to read any product information";

        } else {
            ProductInfo productInfo = allProductInfo.get(OPEN_LIBERTY_CD_PRODUCT_ID);
            if ( productInfo == null ) {
                productInfo = allProductInfo.get(OPEN_LIBERTY_PRODUCT_ID);
            }

            if ( productInfo == null ) {
                isBeta = false;
                isBetaReason = "No product information (" + OPEN_LIBERTY_PRODUCT_ID + " or " + OPEN_LIBERTY_CD_PRODUCT_ID + ")";

            } else {
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "<static init>", "Using product information [ " + productInfo.getId() + " ]");
                }

                String productEdition = productInfo.getEdition();
                if ( productEdition == null ) {
                    isBeta = false;
                    isBetaReason = "No edition in product information (" + ProductInfo.COM_IBM_WEBSPHERE_PRODUCTEDITION_KEY + ")";
                } else {
                    isBeta = productEdition.equals(EARLY_ACCESS);
                    isBetaReason = "Product edition (" + ProductInfo.COM_IBM_WEBSPHERE_PRODUCTEDITION_KEY + ") is (" + productEdition + ")";
                }
            }
        }

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "<static init>", "Annotations beta enablement [ " + isBeta + " ]: " + isBetaReason);
        }
        return isBeta;
    }

    // See "com.ibm.ws.artifact.zip.internal.SystemUtils"

    public static final String SOURCE_DEFAULTED = "defaulted";
    public static final String SOURCE_PROPERTY = "system property";

    /**
     * Run {@link System#getProperty(String)} as a privileged action.
     *
     * @param propertyName The name of the property which is to be retrieved.

     * @return The string property value.  Null if the property is not set.
     */
    public static String getProperty(final String propertyName) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged( new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(propertyName);
                }

            } );
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    // See: "com.ibm.ws.artifact.zip.cache.ZipCachingProperties"

    @Trivial
    public static Boolean getProperty(String methodName, String propertyName) {
        String propertyText = AnnotationsBetaHelper.getProperty(propertyName);

        Boolean propertyValue;
        if ( propertyText == null ) {
            propertyValue = null;
        } else {
            propertyValue = Boolean.valueOf(propertyText);
        }

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName +
                ": Property [ " + propertyName + " ]" + " [ " + propertyValue + " ]");
        }
        return propertyValue;
    }

    //

    /** Property used to override beta enablement, but only for the annotations function. */
    public static final String ANNO_BETA_PROPERTY_NAME = "anno.beta";

    public static final Boolean ANNO_BETA_PROPERTY_VALUE =
        getProperty("<static init>", ANNO_BETA_PROPERTY_NAME);

    /**
     * Tell if the current product is a liberty beta product.
     * 
     * If a property / JVM option has been set, use the value of that property
     * as an override.  Otherwise, answer the value obtained from the liberty
     * product information.
     *
     * @return True or false telling if the current product is a liberty beta product.
     */
    @Trivial
    public static boolean getLibertyBeta() {
        if ( ANNO_BETA_PROPERTY_VALUE != null ) {
            return ANNO_BETA_PROPERTY_VALUE.booleanValue();
        } else {
            return IS_LIBERTY_BETA_PRODUCT;
        }
    }

    //

    /**
     * Answer module annotations for a container.  Use the caching implementation when enabled
     * for the beta.  Otherwise, use the non-caching implementation.
     *
     * @param container The container for which to answer module annotations.
     *
     * @return Module annotations for the container.
     *
     * @throws UnableToAdaptException Thrown if module annotations could not be obtained.
     */
    public static com.ibm.ws.container.service.annotations.ModuleAnnotations getModuleAnnotations(Container container) 
        throws UnableToAdaptException {

        if ( AnnotationsBetaHelper.getLibertyBeta() ) {
            return container.adapt(com.ibm.ws.container.service.annocache.ModuleAnnotations.class);
        } else {
            return container.adapt(com.ibm.ws.container.service.annotations.ModuleAnnotations.class);
        }
    }

    /**
     * Answer web annotations for a container.  Use the caching implementation when enabled
     * for the beta.  Otherwise, use the non-caching implementation.
     *
     * @param container The container for which to answer web  annotations.
     *
     * @return Web annotations for the container.
     *
     * @throws UnableToAdaptException Thrown if web annotations could not be obtained.
     */
    public static com.ibm.ws.container.service.annotations.WebAnnotations getWebAnnotations(Container container) 
        throws UnableToAdaptException {

        if ( AnnotationsBetaHelper.getLibertyBeta() ) {
            return container.adapt(com.ibm.ws.container.service.annocache.WebAnnotations.class);
        } else {
            return container.adapt(com.ibm.ws.container.service.annotations.WebAnnotations.class);
        }
    }
}
