/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jain.protocol.ip.sip;

import jain.protocol.ip.sip.address.AddressFactory;
import jain.protocol.ip.sip.header.HeaderFactory;
import jain.protocol.ip.sip.message.MessageFactory;

/**
 * The SipFactory is a singleton class by which applications
 * can obtain a proprietary (Peer) JAIN SIP Object.
 * The term 'peer' is Java nomenclature for "a particular platform-specific
 * implementation of a Java interface or API".
 * This term has the same meaning for the JAIN SIP API specification.<BR>
 * A Peer JAIN SIP Object can be obtained from the SipFactory
 * by invoking the appropriate create method e.g. to create a peer SipStack, invoke the
 * <a href="#createSipStack()">createSipStack()</a> method, <B>or</B>
 * <P>
 *
 * Note that the SipFactory utilises a naming convention defined for the JAIN SIP API
 * to identify the location of proprietary JAIN SIP Objects.<P>
 * Under this convention the <B>lower-level package structure and classname</B>
 * of a Peer JAIN SIP Object is
 * mandated by a convention defined within the JAIN SIP API. <P>
 * For example: the lower-level package structure and classname of
 * a proprietary implementation of the <I>jain.protocol.ip.sip.SipStack</I>
 * interface <B>must</B> be <I>jain.protocol.ip.sip.SipStackImpl</I>.<P>
 *
 * Under the JAIN naming convention, the <B>upper-level package structure</B> (pathname)
 * can be used to differentiate between proprietary implementations from different SIP Vendors.
 * The pathname used by each SIP Vendor <B>must be</B> the domain name assigned
 * to the Vendor in reverse order, e.g. Sun Microsystem's would be 'com.sun' <P>
 *
 * It follows that a proprietary implementation of a JAIN SIP Object can be located
 * at: <BR>
 * <I>'pathname'.'lower-level package structure and classname'</I><P>
 * <B>Where:</B><P>
 * <I>pathname</I> = reverse domain name, e.g.  com.sun'<P>
 * <I>lower-level package structure and classname</I> = mandated naming convention for the JAIN SIP
 * Object in question<P>
 * e.g. <I>jain.protocol.ip.sip.SipStackImpl</I> is the mandated naming convention
 * for <I>jain.protocol.ip.sip.SipStack</I>.<P>
 * The resulting Peer JAIN SIP Object would be located at: <I><B>com.sun</B>.jain.protocol.ip.sip.SipStackImpl</I><BR>
 *
 * Because the space of domain names is managed, this scheme ensures that collisions
 * between two different vendor's implementations will not happen. For example: a
 * different Vendor with a domain name 'foo.com' would have their Peer SipStack Object
 * located at <I><B>com.foo</B>.jain.protocol.ip.sip.SipStackImpl</I>.<BR>
 * This is a similar concept to the JAVA conventions used for managing package names.<P>
 *
 * The pathname is defaulted to 'com.dynamicsoft.ri' for the reference implementation
 * but may be set using the
 * <a href="#setPathName()">setPathName()</a> method. This allows a JAIN SIP application
 * to switch between different Vendor implementations, as well as providing the
 * capability to use the default or current pathname. Not though, that you cannot mix
 * different vendor's JAIN SIP objects.<P>
 *
 * The SipFactory is a <B>Singleton</B> class. This means that there will
 * only be one instance of the class with a global point of access to it. The single
 * instance of the SipFactory can be obtained using the
 * <a href="#getInstance()">getInstance()</a> method.
 * In this way, the SipFactory can acts a single point for obtaining JAIN SIP Objects.
 *
 *
 *
 *
 * @version 1.0
 *
 */
public class SipFactory
{
    
    /*
    * Creates an instance of a MessageFactory implementation
    * @throws SipPeerUnavailableException if peer class could not be found
    */
    public MessageFactory createMessageFactory()
                           throws SipPeerUnavailableException
    {
        return (MessageFactory)createSipObject("jain.protocol.ip.sip.message.MessageFactoryImpl");
    }
    
    /**
     * Sets the Pathname that identifies the location of a particular Vendor's
     * implementation of the JAIN SIP Objects.
     * The pathname <B>must be</B> the domain name assigned
     * to the Vendor providing the implementation in reverse order.
     * @param <var>pathName</var> the reverse domain name of the Vendor. e.g. Sun Microsystem's would be 'com.sun'
     */
    public void setPathName(String pathName)
    {
        this.pathName = pathName;
    }
    
    /**
     * Returns the current Pathname. The pathname identifies the location of a particular Vendor's
     * implementation of the JAIN SIP Objects.
     * The pathname <B>will always be</B> the domain name assigned
     * to the Vendor providing the implementation in reverse order.
     * @return the pathname
     */
    public String getPathName()
    {
        return pathName;
    }
    
    /*
    * Creates an instance of a HeaderFactory implementation
    * @throws SipPeerUnavailableException if peer class could not be found
    */
    public HeaderFactory createHeaderFactory()
                          throws SipPeerUnavailableException
    {
        return (HeaderFactory)createSipObject("jain.protocol.ip.sip.header.HeaderFactoryImpl");
    }
    
    /*
    * Creates an instance of an AddressFactory implementation
    * @throws SipPeerUnavailableException if peer class could not be found
    */
    public AddressFactory createAddressFactory()
                           throws SipPeerUnavailableException
    {
        return (AddressFactory)createSipObject("jain.protocol.ip.sip.address.AddressFactoryImpl");
    }
    
    /*
    * Creates an instance of a SipStack implementation
    * Note - the implementation determines what ListeningPoint(s) will be available
    * to the SipStack.
    * @throws SipPeerUnavailableException if peer class could not be found
    * @throws SipException if SipStack could not be created for any other reason
    */
    public SipStack createSipStack()
                     throws SipPeerUnavailableException,SipException
    {
        return (SipStack)createSipObject("jain.protocol.ip.sip.SipStackImpl");
    }
    
    /**
     * Returns an instance of a SipFactory.
     * This is a singleton type class so this method is the
     * global access point for the SipFactory.
     * @return the single instance of this singleton SipFactory
     */
    public synchronized static SipFactory getInstance()
    {
        if(myFactory == null)
        {
            myFactory = new SipFactory();
        }
        return myFactory;
    }
    private Object createSipObject(String objectClassName)
                    throws SipPeerUnavailableException
    {
        
        //  If the stackClassName is null, then throw an exception
        if(objectClassName == null)
        {
            throw new SipPeerUnavailableException();
        }
        try
        {
            Class peerObjectClass = Class.forName(getPathName() + "." + objectClassName);
            
            // Creates a new instance of the class represented by this Class object.
            Object newPeerObject = peerObjectClass.newInstance();
            return (newPeerObject);
        }
        catch(Exception e)
        {
            // @PMD:REVIEWED:AvoidConcatMoreThan2Strings: by Amirk on 9/19/04 11:53 AM
            String errmsg = "The Peer JAIN SIP Object: " + getPathName() + "." + objectClassName + " could not be instantiated. Ensure the Path Name has been set.";
            throw new SipPeerUnavailableException(errmsg + "\n" + e.getMessage());
        }
    }
    
    /**
     * Constructor for SipFactory class. This is private because
     * applications are not permitted to create an instance of the
     * SipFactory.
     */
    private SipFactory() 
    {
        
    }
    private String pathName = "com.ibm.ws";
    private static SipFactory myFactory;
}
