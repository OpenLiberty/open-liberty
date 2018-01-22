/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011, 2017
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.artifact.fat_bvt.bundle.adapters;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class ContainerAdapterString implements ContainerAdapter<String> {

    @Override
    public String adapt(
        Container rootContainer,
        OverlayContainer rootOverlayContainer,
        ArtifactContainer artifactContainer,
        Container adaptedContainer) throws UnableToAdaptException {

        String rootContainerID = ContainerUtils.getContainerID(rootContainer);
        if ( !rootContainer.isRoot() ) {
            return "FAIL: Designated container is not a root container: [ " + rootContainerID + " ]";
        } else  if ( !rootContainer.getPath().equals("/") ) {
            return "FAIL: Path of designated root container must be [ / ]: [ " + rootContainerID + " ]";
        }

        String adaptedContainerID = ContainerUtils.getContainerID(adaptedContainer);
        if ( adaptedContainer.isRoot() ) {
            if ( !adaptedContainer.getPath().equals("/") ) {
                return "FAIL: Root container path must be [ / ]: [ " + adaptedContainerID + " ]";
            }
        } else {
            String adaptedPath = adaptedContainer.getPath();
            Entry adaptedEntry = rootContainer.getEntry(adaptedPath);
            if ( adaptedEntry == null ) {
                return "FAIL: Root [ " + rootContainerID + " ]" +
                       " does not contain entry [ " + adaptedPath + " ]" +
                      " for adapted container [ " + adaptedContainerID + " ]";
            }
        }

        String adaptedContainerName = adaptedContainer.getName();
        if ( "aa".equals(adaptedContainerName) ||
             "ab".equals(adaptedContainerName) ||
             "ba".equals(adaptedContainerName) ||
             "baa".equals(adaptedContainerName) ) {

            if ( !adaptedContainer.isRoot() ) {
                return "FAIL: Adapted container [ " + adaptedContainerID + " ] should be made root by structure helper";
            }
        }

        return "Verified adapted container [ " + adaptedContainerID + " ]";
    }
}
