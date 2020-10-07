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
package com.ibm.ws.sip.container.annotation;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.ws.sip.container.properties.SarToWarProperties;
import com.ibm.ws.sip.properties.CoreProperties;
//TODO Liberty find the class in Liberty that provide the LoggerHelper functionality.ther's a LoggerHelper but with different methods.
//import com.ibm.ws.logging.LoggerHelper;
//import com.ibm.ws.logging.WsLogger;
//TODO Liberty replace ECS annotation reading process to Liberty
//import com.ibm.wsspi.ecs.module.WebModule;
//import com.ibm.wsspi.ecs.target.ClassAnnotationTarget;
//import com.ibm.wsspi.ecs.target.PackageAnnotationTarget;

public class SipAnnotationUtil {
	/**
	 * Class Logger.
	 * using logger directly from classes that are related to the deploy process. 
	 * we cannot use our custom logger since it is not working with client side tracing when using CTS deployer
	 */
	private static final String className = SipAnnotationUtil.class.getName(); 
	private static Logger c_logger = null;
    static 
	{
    	c_logger = Logger.getLogger(className);
    	/*if(c_logger instanceof WsLogger){
    		LoggerHelper.addLoggerToGroup(c_logger, "Webui");
    	}*/
	}
    
	/**
	 * When there is a need to create version 289 sip.xml from scratch, 
	 * using this string
	 */
	private static String basicSipXML = "<?xml version=\"1.0\"?>" +
	"<sip-app xmlns=\"http://www.jcp.org/xml/ns/sipservlet\" " +
	"xmlns:javaee=\"http://java.sun.com/xml/ns/javaee\">" +
	"</sip-app>";

	/**
	 * When there is a need to create version 2.5 web.xml from scratch, 
	 * using this string
	 */
	private static String basicWebXML =
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?> 	<web-app id=\"WebApp_1\" version=\"2.5\" xmlns=\"http://java.sun.com/xml/ns/javaee\" 	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\">	" +
		"</web-app>";		

	/**
	 * Get app name node from sip.xml
	 * @param sipXml
	 * @return
	 */
	public static Node getAppName(Document sipXml) {
		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new SipXmlNameSpace());

		Node sipAppNode = null;
		try {
			sipAppNode = (Node)xPath.evaluate(
					"/:sip-app/:app-name", 
					sipXml, XPathConstants.NODE);
		} catch (XPathExpressionException exception) {
			// TODO Auto-generated catch block
			System.out.println(exception.getMessage());
		}

		return sipAppNode; 
	}

	/**
	 * @param sipXml - sip.xml to check 
	 * @return true if app name node exist in sip.xml
	 */
	public static boolean checkIfAppNameExist(Document sipXml) {

		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new SipXmlNameSpace());

		NodeList nodeList = null;
		try {
			nodeList = (NodeList)xPath.evaluate(
					"/:sip-app/:app-name", 
					sipXml, XPathConstants.NODESET);
		} catch (XPathExpressionException exception) {
			// TODO Auto-generated catch block
			System.out.println(exception.getMessage());
		}

		return nodeList != null && nodeList.item(0) != null; 
	}

	/**
	 * @param webXml - web.xml to check 
	 * @return true if display name node exist in web.xml
	 */
	public static boolean checkIfDisplayNameExistInWar(Document webXml) {

		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new WebXmlNameSpace());

		NodeList nodeList = null;
		try {
			nodeList = (NodeList)xPath.evaluate(
					"/:web-app/:display-name", 
					webXml, XPathConstants.NODESET);
		} catch (XPathExpressionException exception) {
			if (c_logger.isLoggable(Level.SEVERE)) {
                c_logger.logp(Level.SEVERE, className, "checkIfDisplayNameExistInWar", "failed to parse web.xml", exception);
            }
		}

		return nodeList != null && nodeList.item(0) != null; 
	}

	/**
	 * @param sipXml - sip.xml to check 
	 * @return true if description node exist in sip.xml
	 */
	public static boolean checkIfDescriptionExist(Document sipXml) {

		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new SipXmlNameSpace());

		NodeList nodeList = null;
		try {
			nodeList = (NodeList)xPath.evaluate(
					"/:sip-app/javaee:description", 
					sipXml, XPathConstants.NODESET);
		} catch (XPathExpressionException exception) {
			// TODO Auto-generated catch block
			System.out.println(exception.getMessage());
		}

		return nodeList != null && nodeList.item(0) != null; 
	}

	/**
	 * @param sipXml - sip.xml to check 
	 * @return true if display name node exist in sip.xml
	 */
	public static boolean checkIfDisplayNameExist(Document sipXml) {

		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new SipXmlNameSpace());

		NodeList nodeList = null;
		try {
			nodeList = (NodeList)xPath.evaluate(
					"/:sip-app/javaee:display-name", 
					sipXml, XPathConstants.NODESET);
		} catch (XPathExpressionException exception) {
			// TODO Auto-generated catch block
			System.out.println(exception.getMessage());
		}

		return nodeList != null && nodeList.item(0) != null; 
	}


	/**
	 * @param sipXml - sip.xml to check 
	 * @return true if icon node exist in sip.xml
	 */
	public static boolean checkIfIconExist(Document sipXml) {

		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new SipXmlNameSpace());

		NodeList nodeList = null;
		try {
			nodeList = (NodeList)xPath.evaluate(
					"/:sip-app/javaee:icon", 
					sipXml, XPathConstants.NODESET);
		} catch (XPathExpressionException exception) {
			// TODO Auto-generated catch block
			System.out.println(exception.getMessage());
		}

		return nodeList != null && nodeList.item(0) != null; 
	}

	/**
	 * @param webXml - web.xml to check 
	 * @return true if icon node exist in web.xml
	 */
	public static boolean checkIfIconExistInWeb(Document webXml) {

		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new WebXmlNameSpace());

		NodeList nodeList = null;
		try {
			nodeList = (NodeList)xPath.evaluate(
					"/:web-app/:icon", 
					webXml, XPathConstants.NODESET);
		} catch (XPathExpressionException exception) {
			if (c_logger.isLoggable(Level.SEVERE)) {
                c_logger.logp(Level.SEVERE, className, "checkIfIconExistInWeb", "failed to parse web.xml", exception);
            }
		}

		return nodeList != null && nodeList.item(0) != null; 
	}

	/**
	 * @param sipXml - sip.xml to check 
	 * @return true if distributable node exist in sip.xml
	 */
	public static boolean checkIfDistributableExist(Document sipXml) {

		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new SipXmlNameSpace());

		NodeList nodeList = null;
		try {
			nodeList = (NodeList)xPath.evaluate(
					"/:sip-app/:distributable", 
					sipXml, XPathConstants.NODESET);
		} catch (XPathExpressionException exception) {
			// TODO Auto-generated catch block
			System.out.println(exception.getMessage());
		}

		return nodeList != null && nodeList.item(0) != null; 
	}

	/**
	 * @param webXml - web.xml to check 
	 * @return true if distributable node exist in web.xml
	 */
	public static boolean checkIfDistributableExistInWeb(Document webXml) {

		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new WebXmlNameSpace());

		NodeList nodeList = null;
		try {
			nodeList = (NodeList)xPath.evaluate(
					"/:web-app/:distributable", 
					webXml, XPathConstants.NODESET);
		} catch (XPathExpressionException exception) {
			if (c_logger.isLoggable(Level.SEVERE)) {
                c_logger.logp(Level.SEVERE, className, "checkIfDistributableExistInWeb", "failed to parse web.xml", exception);
            }
		}

		return nodeList != null && nodeList.item(0) != null; 
	}

	/**
	 * @param sipXml - sip.xml to check 
	 * @return true if proxy config node exist in sip.xml
	 */
	public static boolean checkIfProxyConfigExist(Document sipXml) {

		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new SipXmlNameSpace());

		NodeList nodeList = null;
		try {
			nodeList = (NodeList)xPath.evaluate(
					"/:sip-app/:proxy-config", 
					sipXml, XPathConstants.NODESET);
		} catch (XPathExpressionException exception) {
			// TODO Auto-generated catch block
			System.out.println(exception.getMessage());
		}

		return nodeList != null && nodeList.item(0) != null; 
	}

	/**
	 * @param sipXml - sip.xml to check 
	 * @return true if servlet selection wich contains main servlet node exist in sip.xml
	 */
	public static boolean checkIfMainServletExist(Document sipXml) {

		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new SipXmlNameSpace());

		NodeList nodeList = null;
		try {
			nodeList = (NodeList)xPath.evaluate(
					"/:sip-app/:servlet-selection", 
					sipXml, XPathConstants.NODESET);
		} catch (XPathExpressionException exception) {
			// TODO Auto-generated catch block
			System.out.println(exception.getMessage());
		}

		return nodeList != null && nodeList.item(0) != null; 
	}


	/**
	 * return the main-servlet name from the sip.xml document
	 * 
	 * @param sipXml - sip.xml to check 
	 * @return 
	 */
	public static String getMainServlet(Document sipXml) {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new SipXmlNameSpace());

		String mainServlet = null;
		try {
			mainServlet = (String)xPath.evaluate(
					"/:sip-app/:servlet-selection/:main-servlet/text()", 
					sipXml, XPathConstants.STRING);
		} catch (XPathExpressionException exception) {
			if (c_logger.isLoggable(Level.FINEST)){
				c_logger.logp(Level.FINEST, className, "getMainServlet", "Failed to get main servlet from sip.xml, error is: " + exception.getMessage());
			}
		}

		return mainServlet; 
	}

	/**
	 * @param sipXml - sip.xml to check 
	 * @return true if session config node exist in sip.xml
	 */
	public static boolean checkIfSessionConfigExist(Document sipXml) {

		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new SipXmlNameSpace());

		NodeList nodeList = null;
		try {
			nodeList = (NodeList)xPath.evaluate(
					"/:sip-app/:session-config", 
					sipXml, XPathConstants.NODESET);
		} catch (XPathExpressionException exception) {
			// TODO Auto-generated catch block
			System.out.println(exception.getMessage());
		}

		return nodeList != null && nodeList.item(0) != null; 
	}

	/**
	 * @param sipXml - sip.xml to check
	 * @param listenerClassName - name of listener to check  
	 * @return true if listener node exist in sip.xml
	 */
	public static boolean checkIfListenerExist(Document sipXml, String listenerClassName){

		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new SipXmlNameSpace());

		NodeList nodeList = null;

		try {
			nodeList = (NodeList)xPath.evaluate(
					"/:sip-app/:listener/javaee:listener-class[text()='" + listenerClassName + "']", 
					sipXml, XPathConstants.NODESET);
		} catch (XPathExpressionException exception) {
			System.out.println(exception.getMessage());
		}


		return nodeList != null && nodeList.item(0) != null; 
	}

	/**
	 * @param sipXml - sip.xml to check
	 * @param servletName - servlet name to check 
	 * @param servletClassName - servlet class to check 
	 * @return - true if servlet for that name or that class exist
	 */
	public static boolean checkIfServletExist(Document sipXml, String servletName, String servletClassName) {

		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new SipXmlNameSpace());


		NodeList nodeList = null;
		try {
			nodeList = (NodeList)xPath.evaluate("/:sip-app/:servlet/javaee:servlet-class[text()='"+ 
					servletClassName + "']", 
					sipXml, XPathConstants.NODESET);
		} catch (XPathExpressionException exception) {
			System.out.println(exception.getMessage());
		}

		if (nodeList != null && nodeList.item(0) != null) return true;

		try{
			nodeList = (NodeList)xPath.evaluate("/:sip-app/:servlet/javaee:servlet-name[text()='"+ 
					servletName + "']", 
					sipXml, XPathConstants.NODESET);

		} catch (XPathExpressionException exception) {
			System.out.println(exception.getMessage());
		}

		return nodeList != null && nodeList.item(0) != null; 
	}


	/**
	 * @param webXml - web.xml to check
	 * @param servletName - servlet name to check 
	 * @param servletClassName - servlet class to check 
	 * @return - true if servlet for that name or that class exist
	 */
	public static boolean checkIfServletExistInWeb(Document webXml, String servletName, String servletClassName) {

		XPathFactory  factory=XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		xPath.setNamespaceContext(new WebXmlNameSpace());

		NodeList nodeList = null;
		try {
			nodeList = (NodeList)xPath.evaluate("/:web-app/:servlet/:servlet-class[text()='"+ 
					servletClassName + "']", 
					webXml, XPathConstants.NODESET);
		} catch (XPathExpressionException exception) {
			System.out.println(exception.getMessage());
		}

		if (nodeList != null && nodeList.item(0) != null) return true;

		try{
			nodeList = (NodeList)xPath.evaluate("/:web-app/:servlet/:servlet-name[text()='"+ 
					servletName + "']", 
					webXml, XPathConstants.NODESET);

		} catch (XPathExpressionException exception) {
			if (c_logger.isLoggable(Level.SEVERE)) {
                c_logger.logp(Level.SEVERE, className, "checkIfServletExistInWeb", "failed to parse web.xml", exception);
            }
		}

		return nodeList != null && nodeList.item(0) != null; 
	}

	public static boolean isOffspringOfSipServlet(Class clazz){

		if (clazz.getSuperclass() == null) return false;
		if (clazz.getSuperclass().equals(javax.servlet.sip.SipServlet.class))  return true;

		return isOffspringOfSipServlet(clazz.getSuperclass());
	}

	public static boolean isOffspringOfClass(Class clazz, Class ancesstor){

		if (clazz.equals(ancesstor)) return true;
		if (clazz.getSuperclass() == null) return false;

		return isOffspringOfSipServlet(clazz.getSuperclass());
	}

	/**
	 * Check if the class or one of ancestors implements one of sip listeners  
	 * @param clazz
	 * @return 
	 */
	public static boolean isOffspringOfOneOfListeners(Class clazz){

		if (clazz.getSuperclass() == null) return false;

		for (Class interfaceClazz : clazz.getInterfaces()){

			if (isOffspringOfClass(interfaceClazz, javax.servlet.sip.SipServletListener.class) ||
					isOffspringOfClass(interfaceClazz, javax.servlet.sip.SipApplicationSessionActivationListener.class) ||
					isOffspringOfClass(interfaceClazz, javax.servlet.sip.SipApplicationSessionAttributeListener.class) ||
					isOffspringOfClass(interfaceClazz, javax.servlet.sip.SipApplicationSessionBindingListener.class) ||
					isOffspringOfClass(interfaceClazz, javax.servlet.sip.SipApplicationSessionListener.class) ||
					isOffspringOfClass(interfaceClazz, javax.servlet.sip.SipErrorListener.class) ||
					isOffspringOfClass(interfaceClazz, javax.servlet.sip.SipSessionActivationListener.class) ||
					isOffspringOfClass(interfaceClazz, javax.servlet.sip.SipSessionAttributeListener.class) ||
					isOffspringOfClass(interfaceClazz, javax.servlet.sip.SipSessionBindingListener.class) ||
					isOffspringOfClass(interfaceClazz, javax.servlet.sip.SipSessionListener.class) ||
					isOffspringOfClass(interfaceClazz, javax.servlet.sip.TimerListener.class)||
					isOffspringOfClass(interfaceClazz, com.ibm.websphere.sip.unmatchedMessages.UnmatchedMessageListener.class) ){

				return true;
			}
		}
		return isOffspringOfSipServlet(clazz.getSuperclass());
	}


	public static String getBaicSipXml(){
		return basicSipXML;
	}

	public static String getBaicWebXml(){
		return basicWebXML;
	}

	/**
	 * Look for @SipListener annotations in the code, and add the information to the sip.xml
	 * 
	 * @param sipModule - ECS sip module representation 
	 * @param sipXmlDocument - sip xml document
	 */
	public static void processListenerAnnotations(/*WebModule sipModule,*/ Document  sipXmlDocument) {
		if (c_logger.isLoggable(Level.FINER)){
			c_logger.entering(className, "processListenerAnnotations");
		}

		//get all SipListener annotations using ECS
		//TODO Liberty replace the implementation to match Liberty annotation reading
		/*Map<String, ClassAnnotationTarget>  sipListenerAnnotationMap = 
			sipModule.getAnnotationScanner().getClassAnnotationTargets(SipListener.class);*/

		/*Collection<ClassAnnotationTarget> listOfAnnClasses =  sipListenerAnnotationMap.values();*/

		// Iterate over class @SipListener annotations
		/*for (ClassAnnotationTarget classAnnotationTarget : listOfAnnClasses){
			Class clazz = null;
			String listenerDescription = null;

			try {
				clazz = classAnnotationTarget.getApplicableClass().getInstance();
				SipListener sipListener = (SipListener) clazz.getAnnotation(SipListener.class);
				listenerDescription = sipListener.description();
			} catch (ClassNotFoundException exception) {
				if (c_logger.isLoggable(Level.FINEST)){
					c_logger.logp(Level.FINEST, className, "processListenerAnnotations", 
							"Can't init object for " + classAnnotationTarget.getApplicableClass().getName());
				}
				continue;
			}

			String listenerClassName = classAnnotationTarget.getApplicableClass().getName();

			if (SipAnnotationUtil.checkIfListenerExist(sipXmlDocument, listenerClassName)){
				if(c_logger.isLoggable(Level.FINEST)){
					c_logger.logp(Level.FINEST, className, "processListenerAnnotations", "Sip listener already defined in sip.xml for listener class - " + listenerClassName);
				}
				continue;
			}

			// Validate that listener class implements listener interface. 
			if (!SipAnnotationUtil.isOffspringOfOneOfListeners(clazz)){

				//TODO:  trace error to the log and not debug 	
				if(c_logger.isLoggable(Level.FINEST)){
					c_logger.logp(Level.FINEST, className, "processListenerAnnotations", clazz.getCanonicalName() + " does not extends any of listeners class");
				}
				continue;
			}

			SipXmlUtil.addListenerToSipXml(sipXmlDocument, listenerClassName, listenerDescription);
			if(c_logger.isLoggable(Level.FINEST)){
				c_logger.logp(Level.FINEST, className, "processListenerAnnotations", "Adding Sip listener to sip.xml for listener class - " + listenerClassName);
			}
		}*/

		if (c_logger.isLoggable(Level.FINER)){
			c_logger.exiting(className, "processListenerAnnotations");
		}
	}

	/**
	 * Look for @SipServlet annotations in the code, and add the information to the sip.xml and web.xml
	 * 
	 * @param sipModule - ECS sip module representation 
	 * @param sipXmlDocument - sip xml document
	 * @param webXmlDocument - web xml document
	 */
	@SuppressWarnings("unchecked")
	public static void processServletAnnotations(/*WebModule sipModule,*/ Document  sipXmlDocument, Document webXmlDocument) {
		if (c_logger.isLoggable(Level.FINER)){
			c_logger.entering(className, "processServletAnnotations");
		}

		//get all sipServlet annotations using ECS
		//TODO Liberty replace the implementation to match Liberty annotation reading
		/*Map<String, ClassAnnotationTarget>  sipServletAnnotationMap = 
			sipModule.getAnnotationScanner().getClassAnnotationTargets(SipServlet.class);*/

		/*Collection<ClassAnnotationTarget> listOfAnnClasses =  sipServletAnnotationMap.values();*/

		// Iterate over class @SipServlet annotations
		/*for (ClassAnnotationTarget classAnnotationTarget : listOfAnnClasses){

			Class clazz = null;
			try {
				clazz = classAnnotationTarget.getApplicableClass().getInstance();
			} catch (ClassNotFoundException exception) {
				if (c_logger.isLoggable(Level.FINEST)){
					c_logger.logp(Level.FINEST, className, "processServletAnnotations", 
							"Can't init object for " + classAnnotationTarget.getApplicableClass().getName());
				}

				continue;
			}

			SipServlet sipServletAnnotation = (SipServlet)clazz.getAnnotation(SipServlet.class);

			String servletClassName 	   = classAnnotationTarget.getApplicableClass().getName();
			String servletClassShortName = clazz.getSimpleName();

			int loadOnStartup       	   = sipServletAnnotation.loadOnStartup();
			String servletName      	   = sipServletAnnotation.name();
			String servletDesc      	   = sipServletAnnotation.description();

			//get the servlet name, if its empty use the class short name
			if(servletName.equals("")) {
				servletName = servletClassShortName;
			}

			if (c_logger.isLoggable(Level.FINEST)){
				c_logger.logp(Level.FINEST, className, "processServletAnnotations", 
						"Adding new siplet, name=" + servletName);
			}

			// Validate that servlet class extends SipServlet 
			if (!SipAnnotationUtil.isOffspringOfSipServlet(clazz)){

				//TODO:  trace error to the log and not debug 	
				if(c_logger.isLoggable(Level.FINEST)){
					c_logger.logp(Level.FINEST, className, "processServletAnnotations", clazz.getCanonicalName() + " does not extends SipServlet class");
				}
				continue;
			}

			boolean isServletExistInSip = SipAnnotationUtil.checkIfServletExist(sipXmlDocument, servletName, servletClassName);

			//add the servlet declaration to the sip xml if missing
			if (!isServletExistInSip){
				// Record to sip.xml
				SipXmlUtil.addServletToSipXml(sipXmlDocument, servletName, 
						servletClassName, loadOnStartup, servletDesc);
			}else{
				if(c_logger.isLoggable(Level.FINEST)){
					c_logger.logp(Level.FINEST, className, "processServletAnnotations", "Sip servlet already defined in sip.xml for servlet class - " + servletClassName
							+ " servlet name - " + servletName );
				}
			}

			//add the servlet declaration to the web xml if missing only if we have a web dom object
			if (webXmlDocument != null){
				boolean isServletExistinWeb = SipAnnotationUtil.checkIfServletExistInWeb(webXmlDocument, servletName, servletClassName);
				if (!isServletExistinWeb){
					// Record to web.xml
					SipXmlUtil.addServletToWebXml(webXmlDocument, servletName, 
							servletClassName, loadOnStartup, servletDesc);
				}else{
					if(c_logger.isLoggable(Level.FINEST)){
						c_logger.logp(Level.FINEST, className, "processServletAnnotations", "Sip servlet already defined in web.xml for servlet class - " + servletClassName
								+ " servlet name - " + servletName );
					}
				}
			}
		}*/

		if (c_logger.isLoggable(Level.FINER)){
			c_logger.exiting(className, "processServletAnnotations");
		}
	}

	/**
	 * Look for @SipApplication in the code, and add the information to the sip.xml and web.xml
	 * 
	 * @param sipModule - ECS sip module representation 
	 * @param sipXmlDocument - sip xml document
	 * @param webXmlDocument - web xml document
	 * @return - the application name
	 * @throws StopDeploymentException
	 */
	public static String processSipApplicationAnnotations(/*WebModule sipModule,*/ Document  sipXmlDocument, Document webXmlDocument) throws StopDeploymentException {
//		if (c_logger.isLoggable(Level.FINER)){
//			c_logger.entering(className, "processSipApplicationAnnotations");
//		}
//
//		boolean addToWeb = (webXmlDocument != null);
//		if (c_logger.isLoggable(Level.FINEST)){
//			c_logger.logp(Level.FINEST, className, "processSipApplicationAnnotations", "web.xml was found: " + addToWeb);
//		}
//
//		//use ECS to get all package level SipApplication annotations from the module
//		String applicationName = null;
//		/*Map<String, PackageAnnotationTarget>  sipAppAnnotationMap = 
//			sipModule.getAnnotationScanner().getPackageAnnotationTargets(SipApplication.class);*/
//
//		/*TODO: sipAppAnnotationMap.values() > 1*/
//		if (true/*!sipAppAnnotationMap.values().isEmpty()*/){
//
//			// Found package annotation
//			/*PackageAnnotationTarget packageAnnotationTarget = sipAppAnnotationMap.values().iterator().next();*/
//
//			Class appPackage = null;
//			try {
//				/*appPackage = packageAnnotationTarget.getApplicablePackage().getInstance();*/
//			}
//			catch (Exception exception/*ClassNotFoundException exception*/) {
//				if (c_logger.isLoggable(Level.FINEST)){
//					c_logger.logp(Level.FINEST, className, "processSipApplicationAnnotations", "Can't init object for " + 
//							/*packageAnnotationTarget.getApplicablePackage().getName() + */".package-info");
//				}
//				return null;
//			}
//
//			SipApplication sipAppAnnotation = (SipApplication)appPackage.getAnnotation(SipApplication.class);
//
//			// Handle application name first.
//			applicationName = sipAppAnnotation.name();
//
//			if (sipAppAnnotation.name().equals("")) {
//				/*applicationName = packageAnnotationTarget.getApplicablePackage().getName(); */
//			}
//
//			//add app name to sip.xml
//			if (!SipAnnotationUtil.checkIfAppNameExist(sipXmlDocument)){
//				SipXmlUtil.addSipAppToSipXml(sipXmlDocument, applicationName); 
//			} 
//
//			//add display name to the web.xml
//			if (addToWeb && !SipAnnotationUtil.checkIfDisplayNameExistInWar(webXmlDocument)){
//				SipXmlUtil.addDisplayNameToWebXml(webXmlDocument, applicationName); 
//			} 
//
//			if (c_logger.isLoggable(Level.FINEST)){
//				c_logger.logp(Level.FINEST, className, "processSipApplicationAnnotations", "found application name: " + applicationName);
//			}
//
//			// Add distributable tag.
//			boolean isDistributable  = sipAppAnnotation.distributable();
//			if (isDistributable){
//
//				//add the distribution tag to the sip xml if missing
//				if (!SipAnnotationUtil.checkIfDistributableExist(sipXmlDocument)){
//					SipXmlUtil.addDistributableToSipXml(sipXmlDocument);        	
//				}
//
//				//add the distribution tag to the web xml if missing
//				if (addToWeb && !SipAnnotationUtil.checkIfDistributableExistInWeb(webXmlDocument)){
//					SipXmlUtil.addDistributableToWebXml(webXmlDocument);        	
//				}
//
//				if (c_logger.isLoggable(Level.FINEST)){
//					c_logger.logp(Level.FINEST, className, "processSipApplicationAnnotations", "application is distributable");
//				}
//			}
//
//			//find the main servlet to use, we need to find only one main servlet from the sip.xml or SipApplication annotation
//			//and they must not conflict
//			String annotationMainServletName = sipAppAnnotation.mainServlet();
//			String xmlMainServletName = SipAnnotationUtil.getMainServlet(sipXmlDocument);
//
//			boolean xmlMainServletExist = (xmlMainServletName != null && xmlMainServletName.length() > 0); 
//			boolean annotaionMainServletExist = (annotationMainServletName != null && annotationMainServletName.length() > 0); 
//
//			if (c_logger.isLoggable(Level.FINEST)){
//				c_logger.logp(Level.FINEST, className, "processSipApplicationAnnotations", "annotationMainServletName=" + annotationMainServletName + ", xmlMainServletName=" + xmlMainServletName);
//			}
//			
//			if (! xmlMainServletExist && annotaionMainServletExist) {
//				// Add main-servlet tag.
//				SipXmlUtil.addMainServletToSipXml(sipXmlDocument, annotationMainServletName);
//				if (c_logger.isLoggable(Level.FINEST)){
//					c_logger.logp(Level.FINEST, className, "processSipApplicationAnnotations", "main servlet was found in SipApplication annotation and will be added to the sip.xml, value=" + annotationMainServletName);
//				}
//			}else {  
//				// if the main server declaration exists in both annotation and sip.xml, and is different, we have to fail the deployment   
//				if (xmlMainServletExist && annotaionMainServletExist && ! annotationMainServletName.equals(xmlMainServletName)) {
//					throw new StopDeploymentException("main server declaration exists in both annotation and sip.xml, stop deployment");
//				}
//			}
//
//			// Add proxy-timeout tag
//			if (!SipAnnotationUtil.checkIfProxyConfigExist(sipXmlDocument)) {
//				int proxyTimeout = sipAppAnnotation.proxyTimeout();
//				SipXmlUtil.addProxyTimeoutToSipXml(sipXmlDocument, proxyTimeout);
//				if (c_logger.isLoggable(Level.FINEST)){
//					c_logger.logp(Level.FINEST, className, "processSipApplicationAnnotations", "adding proxy-timeout to the sip.xml, value=" + proxyTimeout);
//				}
//			}
//
//			// Add session-timeout tag
//			if (!SipAnnotationUtil.checkIfSessionConfigExist(sipXmlDocument)) {
//				int sessionTimeout = sipAppAnnotation.sessionTimeout();
//				SipXmlUtil.addSessionTimeoutToSipXml(sipXmlDocument, sessionTimeout);
//				if (c_logger.isLoggable(Level.FINEST)){
//					c_logger.logp(Level.FINEST, className, "processSipApplicationAnnotations", "adding session-timeout to the sip.xml, value=" + sessionTimeout);
//				}
//			}
//
//			// Add description tag
//			if (!SipAnnotationUtil.checkIfDescriptionExist(sipXmlDocument)) {
//				String description = sipAppAnnotation.description();
//				SipXmlUtil.addDescriptionToSipXml(sipXmlDocument, description);
//				if (c_logger.isLoggable(Level.FINEST)){
//					c_logger.logp(Level.FINEST, className, "processSipApplicationAnnotations", "adding description to the sip.xml, value=" + description);
//				}
//			}
//
//			// Add display-name tag
//			if (!SipAnnotationUtil.checkIfDisplayNameExist(sipXmlDocument)) {
//				String displayName = sipAppAnnotation.displayName();
//				SipXmlUtil.addDisplayNameToSipXml(sipXmlDocument, displayName);
//				if (c_logger.isLoggable(Level.FINEST)){
//					c_logger.logp(Level.FINEST, className, "processSipApplicationAnnotations", "adding displayName to the sip.xml, value=" + displayName);
//				}
//			}
//
//			// Add icon tag to sip.xml
//			if (!SipAnnotationUtil.checkIfIconExist(sipXmlDocument)) {
//				String smallIcon = sipAppAnnotation.smallIcon();
//				String largeIcon = sipAppAnnotation.largeIcon();
//				SipXmlUtil.addIconToSipXml(sipXmlDocument, smallIcon, largeIcon);
//				if (c_logger.isLoggable(Level.FINEST)){
//					c_logger.logp(Level.FINEST, className, "processSipApplicationAnnotations", "adding large/small icons to the sip.xml, smallIcon=" + smallIcon + ", largeIcon=" + largeIcon);
//				}
//			}
//
//			// Add icon tag to web.xml
//			if (addToWeb && !SipAnnotationUtil.checkIfIconExistInWeb(webXmlDocument)) {
//				String smallIcon = sipAppAnnotation.smallIcon();
//				String largeIcon = sipAppAnnotation.largeIcon();
//				SipXmlUtil.addIconToWebXml(webXmlDocument, smallIcon, largeIcon);
//				if (c_logger.isLoggable(Level.FINEST)){
//					c_logger.logp(Level.FINEST, className, "processSipApplicationAnnotations", "adding large/small icons to the web.xml, smallIcon=" + smallIcon + ", largeIcon=" + largeIcon);
//				}
//			}
//
//		} else {
//			if (c_logger.isLoggable(Level.FINEST)){
//				c_logger.logp(Level.FINEST, className, "processSipApplicationAnnotations", "Did not find any SipApplication annotation, skipping SipApplication annotation processing");
//			}
//		}
//
//		if (c_logger.isLoggable(Level.FINER)){
//			c_logger.exiting(className, "processSipApplicationAnnotations");
//		}

//		return applicationName;
		return " ";
	}

	/**
	 * Validate sip.xml DOM representation against jsr289 schema
	 * 
	 * @param sipXmlDocument - the sip.xml to validate
	 * @throws SAXException
	 * @throws IOException
	 */
	public static void validateJsr289(Document sipXmlDocument) throws SAXException, IOException{
		//do the actual validation against the parsed document
		SchemaFactory factory =	SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		String wasRoot = System.getProperty("was.install.root");

		//the prefix is for the sar2war directory under WAS_HOME dir
		String prefix = wasRoot + java.io.File.separator + "sar2war_tool" + java.io.File.separator;
		File file = new File(prefix + "sip-app_1_1.xsd");

		//encode spaces if the directory includes spaces
		prefix = "file:////" + prefix.replaceAll(" ", "%20");

		String xmlXsdFile = prefix + "xml.xsd"; 
		String schemaXsdFile = prefix + "XMLSchema.xsd";
		String webXsdFile = prefix + "web-app_2_5.xsd";
		String sipXsdFile = prefix + "sip-app_1_1.xsd";

		//add local file system xsd reference, the validator should not go to the network for validation
		String value = "http://www.w3.org/XML/1998/namespace " + xmlXsdFile + 
		" http://www.w3.org/2001/XMLSchema " + schemaXsdFile +
		" http://www.jcp.org/xml/ns/sipservlet " + sipXsdFile+
		" http://java.sun.com/xml/ns/javaee " + webXsdFile;

		factory.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", value);
		Schema schema = factory.newSchema(file);
		Validator validator = schema.newValidator();
		validator.validate(new DOMSource(sipXmlDocument));
	}

	/**
	 * check if we are in amm support mode, try to read it from the configuration
	 * and from the java properties in case of a DMGR process
	 * 
	 * @return
	 */
	public static boolean supportAmm() {
		boolean supportAmm = false;
//		if (PropertiesStore.getInstance().getProperties() != null){
//			//try to get the property from the SIP container custom properties
//			supportAmm = PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.SUPPORT_AMM_ANNOTATION_READING);
//
//			if (c_logger.isLoggable(Level.FINEST)){
//				c_logger.logp(Level.FINEST, className, "supportAmm", "found support amm property in the custom properties");
//			}
//		}
		if (SarToWarProperties.isLoaded()) {
			supportAmm = SarToWarProperties.getSupportAmmAnnotationReading();
			if (c_logger.isLoggable(Level.FINEST)){
				c_logger.logp(Level.FINEST, className, "supportAmm", "found support amm property in the custom properties");
			}
		}else{
			//try to get the property from the JVM property if we running in the dmgr
			String supportSarToWarStr = System.getProperty(CoreProperties.SUPPORT_AMM_ANNOTATION_READING, "false");
			if (supportSarToWarStr.equalsIgnoreCase("true")){
				supportAmm = true;
			}
		}

		if (c_logger.isLoggable(Level.FINER)){
			c_logger.exiting(className, "supportAmm", supportAmm);
		}

		return supportAmm;
	}
}