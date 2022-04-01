/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.internal.config;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class allows the two cdi configuration elements (cdi12 and cdi) to be aggregated together to provide
 * a single set of configuration. An error message will be displayed if the two configuration elements are
 * conflicting.
 */
public abstract class AggregatedConfiguration implements CDIConfiguration {
    private static final TraceComponent tc = Tr.register(AggregatedConfiguration.class);

    public static final String ENABLE_IMPLICIT_BEAN_ARCHIVES = "enableImplicitBeanArchives";
    public static final String EMPTY_BEANS_XML_EXPLICIT_BEAN_ARCHIVE = "emptyBeansXMLExplicitBeanArchive";

    private static final Boolean ENABLE_IMPLICIT_BEAN_ARCHIVES_DEFAULT = Boolean.TRUE; //default value is set here rather that in config metatype
    private static final Boolean EMPTY_BEANS_XML_EXPLICIT_BEAN_ARCHIVE_DEFAULT = Boolean.FALSE; //default value is set here rather that in config metatype

    private Boolean cdi12EnableImplicitBeanArchives = null;
    private Boolean cdiEnableImplicitBeanArchives = null;
    private Boolean cdiEmptyBeansXMLExplicitBeanArchive = null;

    private boolean enableImplicitBeanArchives = ENABLE_IMPLICIT_BEAN_ARCHIVES_DEFAULT;
    private boolean emptyBeansXMLExplicitBeanArchive = EMPTY_BEANS_XML_EXPLICIT_BEAN_ARCHIVE_DEFAULT;

    /**
     * Set the value of enableImplicitBeanArchives as set in the cdi12 configuration element
     *
     * @param enableImplicitBeanArchives
     */
    public void setCdi12Config(Boolean enableImplicitBeanArchives) {
        this.cdi12EnableImplicitBeanArchives = enableImplicitBeanArchives;
        updateAggregateConfig();
    }

    /**
     * Set the values of enableImplicitBeanArchives and emptyBeansXMLExplicitBeanArchive as set in the cdi configuration element
     *
     * @param enableImplicitBeanArchives
     * @param emptyBeansXMLExplicitBeanArchive
     */
    public void setCdiConfig(Boolean enableImplicitBeanArchives, Boolean emptyBeansXMLExplicitBeanArchive) {
        this.cdiEnableImplicitBeanArchives = enableImplicitBeanArchives;
        this.cdiEmptyBeansXMLExplicitBeanArchive = emptyBeansXMLExplicitBeanArchive;
        updateAggregateConfig();
    }

    /**
     * Work out the aggregated configuration values based on both cdi12 and cdi
     */
    private void updateAggregateConfig() {
        if (this.cdiEnableImplicitBeanArchives != null) {
            //if enableImplicitBeanArchives was set on the cdi element, use that value
            this.enableImplicitBeanArchives = this.cdiEnableImplicitBeanArchives;

            //if it was also set to a different value on the cdi12 element, output a warning
            if (this.cdi12EnableImplicitBeanArchives != null &&
                this.cdiEnableImplicitBeanArchives != this.cdi12EnableImplicitBeanArchives) {
                if (tc.isWarningEnabled()) {
                    Tr.warning(tc, "enableImplicitBeanArchives.conflict.CWOWB1017W");
                }
            }
        } else if (this.cdi12EnableImplicitBeanArchives != null) {
            //if enableImplicitBeanArchives was set on the cdi12 element, use that value
            this.enableImplicitBeanArchives = this.cdi12EnableImplicitBeanArchives;
        } else {
            //if neither one was set, use the default
            this.enableImplicitBeanArchives = ENABLE_IMPLICIT_BEAN_ARCHIVES_DEFAULT;
        }

        if (tc.isWarningEnabled() && !this.enableImplicitBeanArchives) {
            Tr.warning(tc, "implicit.bean.scanning.disabled.CWOWB1009W");
        }

        //if emptyBeansXMLExplicitBeanArchive was explicitly set in server.xml then use that value, otherwise use the default (false)
        if (this.cdiEmptyBeansXMLExplicitBeanArchive != null) {
            this.emptyBeansXMLExplicitBeanArchive = this.cdiEmptyBeansXMLExplicitBeanArchive;
        } else {
            this.emptyBeansXMLExplicitBeanArchive = EMPTY_BEANS_XML_EXPLICIT_BEAN_ARCHIVE_DEFAULT;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Aggregated Config: " + ENABLE_IMPLICIT_BEAN_ARCHIVES + ": " + this.enableImplicitBeanArchives + " " + EMPTY_BEANS_XML_EXPLICIT_BEAN_ARCHIVE + ": "
                         + this.emptyBeansXMLExplicitBeanArchive);
        }

    }

    //note that this method is backwards to the original config property (disabled -> true)
    @Override
    public boolean isImplicitBeanArchivesScanningDisabled() {
        return !this.enableImplicitBeanArchives;
    }

    @Override
    public boolean emptyBeansXMLExplicitBeanArchive() {
        return this.emptyBeansXMLExplicitBeanArchive;
    }

}
