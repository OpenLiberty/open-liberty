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
package vistest.ejbAsAppClientLib;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import vistest.framework.TestingBean;
import vistest.framework.VisTester;

@ApplicationScoped
public class EjbAsAppClientLibTestingBean implements TestingBean {

    @Inject
    private BeanManager beanManager;

    /*
     * (non-Javadoc)
     * 
     * @see vistest.framework.TestingBean#doTest()
     */
    @Override
    public String doTest() {
        return VisTester.doTest(beanManager);
    }

}
