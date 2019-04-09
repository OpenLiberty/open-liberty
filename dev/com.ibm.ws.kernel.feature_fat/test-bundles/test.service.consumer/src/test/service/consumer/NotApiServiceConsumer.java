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
package test.service.consumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.ws.kernel.feature.test.api.NotApiService;

/**
 *
 */
@Component(service = {})
public class NotApiServiceConsumer {
    private NotApiService notApiService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setNotApiService(NotApiService apiService) {
        this.notApiService = apiService;
    }

    @Activate
    protected void activate() {
        if (notApiService == null) {
            // this is the expected outcome because NotApiService should be hidden
            System.out.println("NotApiService - SUCCESS");
        } else {
            System.out.println(notApiService.doTest());
        }
    }
}
