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
package test.service.provider;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.kernel.feature.test.api.ApiService;

/**
 *
 */
@Component(service = ApiService.class)
public class ApiServiceImpl implements ApiService {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.kernel.feature.test.api.ApiService#doTest()
     */
    @Override
    public String doTest() {
        return "ApiService - SUCCESS";
    }

}
