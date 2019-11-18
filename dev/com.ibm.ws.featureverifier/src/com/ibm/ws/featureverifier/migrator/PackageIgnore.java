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
package com.ibm.ws.featureverifier.migrator;

import com.ibm.ws.featureverifier.migrator.IgnoreMigrator.Section;

class PackageIgnore extends Ignore {
    final boolean baseline;
    final boolean runtime;
    final String packageValue;

    public PackageIgnore(Section section, String feature, String value, boolean b, boolean r) {
        super(section, feature);
        this.packageValue = value;
        this.baseline = b;
        this.runtime = r;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null)
            return false;

        if (!(o instanceof PackageIgnore))
            return false;

        PackageIgnore pi = (PackageIgnore) o;

        return pi.toString().equals(toString());

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((packageValue == null) ? 0 : packageValue.hashCode());
        result = prime * result + ((feature == null) ? 0 : feature.hashCode());
        result = prime * result + (Boolean.valueOf(baseline).hashCode());
        result = prime * result + (Boolean.valueOf(runtime).hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<package baseline=");
        sb.append(IgnoreConstants.QUOTE);
        sb.append(baseline);
        sb.append(IgnoreConstants.QUOTE);
        sb.append(" runtime=");
        sb.append(IgnoreConstants.QUOTE);
        sb.append(runtime);
        sb.append(IgnoreConstants.QUOTE);
        if (feature != null) {
            sb.append(" forFeature=");
            sb.append(IgnoreConstants.QUOTE);
            sb.append(feature);
            sb.append(IgnoreConstants.QUOTE);
        }
        sb.append(">");
        sb.append(packageValue);
        sb.append("</package>");

        return sb.toString();
    }
}