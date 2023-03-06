/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package test.iiop.server;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import test.iiop.common.LogMessages;

@Component(immediate = true)
public class FeatureCanary {
    @Activate
    protected void activate() {
        System.out.println(LogMessages.TEST_FEATURE_ACTIVATING);
    }

    @Deactivate
    protected void deactivate() {
        System.out.println(LogMessages.TEST_FEATURE_DEACTIVATING);
    }
}
