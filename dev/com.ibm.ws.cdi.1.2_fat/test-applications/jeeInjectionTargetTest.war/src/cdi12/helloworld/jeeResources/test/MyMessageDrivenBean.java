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

package cdi12.helloworld.jeeResources.test;

import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;

@MessageDriven
public class MyMessageDrivenBean implements MessageListener {

    @Inject
    HelloWorldExtensionBean2 bean;

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
     */
    @Override
    public void onMessage(Message arg0) {
        // TODO Auto-generated method stub

    }

}
