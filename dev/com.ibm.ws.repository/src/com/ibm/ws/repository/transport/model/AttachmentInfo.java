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

package com.ibm.ws.repository.transport.model;

public class AttachmentInfo extends AbstractJSON {
    private long _CRC;
    private ImageDetails imageDetails;

    public void setCRC(long crc) {
        this._CRC = crc;
    }

    public long getCRC() {
        return this._CRC;
    }

    public void setImageDetails(ImageDetails details) {
        this.imageDetails = details;
    }

    public ImageDetails getImageDetails() {
        return imageDetails;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (_CRC ^ (_CRC >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        AttachmentInfo other = (AttachmentInfo) obj;
        if (_CRC != other._CRC)
            return false;

        if (imageDetails == null) {
            if (other.imageDetails != null)
                return false;
        } else if (!imageDetails.equals(other.imageDetails))
            return false;

        return true;
    }

}
