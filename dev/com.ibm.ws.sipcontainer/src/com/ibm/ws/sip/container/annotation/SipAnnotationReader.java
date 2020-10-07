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

// TODO Liberty Annotation import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
//import org.apache.xml.serialize.XMLSerializer;
//import org.apache.xml.serialize.XMLSerializer;


//TODO Liberty Annotation - remove the comment

//import org.eclipse.jst.j2ee.commonarchivecore.internal.ModuleFile;
//import com.ibm.ws.ecs.internal.wtp.module.impl.OpenedArchiveModuleOptions;
//import com.ibm.wsspi.ecs.exception.ModuleOpenException;
//import com.ibm.wsspi.ecs.module.ModuleFactory;
//import com.ibm.wsspi.ecs.module.WebModule;
/**
 * This class is used for parsing annotations during SIP module startup time
 * and adding them to the sip.xml stream so it will be parsed correctly later
 * on in the process, it is only used if sar-to-war was skipped and the module 
 * has sip annotations. (e.g. when deploying from RAD)
 * 
 * @author asafz
 *
 */
public class SipAnnotationReader {
	private static final LogMgr c_logger = Log.get(SipAnnotationReader.class);

	//object representation of the SIP module that we working on
	//TODO Liberty replace all the current annotation reading mechanism (ECS) with Liberty mechanism (WebModule, ModuleFile, ModuleFactory
	/*private WebModule _sipModule = null;*/

	/**
	 * Constructor
	 * 
	 * @param moduleFile
	 * @param warCtx - the module context
	 * 
	 * @throws StopDeploymentException
	 */
	public SipAnnotationReader(/*ModuleFile moduleFile,*/ String warCtx) throws StopDeploymentException{
		if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "constructor", new Object[] {/*moduleFile,*/ warCtx});
		}
		
		// Specify factory for ECS factory.
		/*System.setProperty(ModuleFactory.class.getName(),
							"com.ibm.ws.ecs.internal.wtp.module.impl.WTPModuleFactory");*/

		/*ModuleFactory moduleFactory = ModuleFactory.getInstance();*/
		try {
			//try to read the open module file if exist, otherwise re open it
			/*if (moduleFile != null && moduleFile.isOpen()){
				if (c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug(this, "constructor", "The module file is already opened, going to use the opened module");
				}
				OpenedArchiveModuleOptions options = new OpenedArchiveModuleOptions(moduleFile);
				_sipModule = (WebModule) moduleFactory.openWebModule(options);
			}else{
				if (c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug(this, "constructor", "The module file is not opened, going to open it again");
				}
				_sipModule = (WebModule) moduleFactory.openWebModule(warCtx);
			}*/
			
			
		} catch (Exception e/*ModuleOpenException e*/) {
			if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug(this, "constructor", "ModuleOpenException was thrown, msg=" + e.getMessage());
			}
			throw new StopDeploymentException();
		}
		
		if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this, "constructor");
		}
	}

	/**
	 * add the sip annotations to the given inputStream 
	 * 
	 * @param sipInputStream - the input stream that we want to add annotations to (the original sip.xml)
	 * @return - the sip.xml input stream with the annotations added to it
	 * @throws StopDeploymentException
	 */
	public InputStream addSipAnnotations(InputStream sipInputStream) throws StopDeploymentException {
	
		// TODO Liberty - remove the comment
		
//		if (c_logger.isTraceEntryExitEnabled()){
//			c_logger.traceEntry(this, "addSipAnnotations");
//		}
//		
//		boolean isDefaultXml = false;
//		//if there is no sip.xml we need to create the default one
//		if (sipInputStream == null){
//			if (c_logger.isTraceDebugEnabled()){
//				c_logger.traceDebug(this, "addSipAnnotations", "Sip module does not have a sip.xml file, creating default one");
//			}
//			sipInputStream = new ByteArrayInputStream(SipAnnotationUtil.getBaicSipXml().getBytes());
//			isDefaultXml = true;
//		}
//
//		InputStream outStream = sipInputStream;
//		
//		try {
//			DocumentBuilderFactory xmlParser = DocumentBuilderFactory.newInstance();
//			xmlParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
//			xmlParser.setNamespaceAware(true);
//
//			//Parsing sip.xml with schema validation
//			DocumentBuilder builder2 = xmlParser.newDocumentBuilder();
//			DefaultHandler dh = new DefaultHandler() {
//				public void error(SAXParseException saxparseexception)
//				throws SAXException {
//					throw saxparseexception;
//				}
//			};
//			
//			builder2.setErrorHandler(dh);
//			Document sipXmlDocument = builder2.parse(sipInputStream);
//
//			//run schema validation to make sure if this is 289 application
//			boolean enableSchemaValidation = PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.ENABLE_JSR289_SCHEMA_VALIDATION);
//			
//			//do JSR289 schema validation if needed, there is no need to validate if this is the default XML, we already know that
//			//it is valid
//			if (enableSchemaValidation && ! isDefaultXml){
//				SipAnnotationUtil.validateJsr289(sipXmlDocument);
//			}
//			
//			String appName = SipAnnotationUtil.processSipApplicationAnnotations(/*_sipModule,*/ sipXmlDocument, null);
//			if (appName == null && !SipAnnotationUtil.checkIfAppNameExist(sipXmlDocument)){
//				//this is not 289 app there is no need to read the other annotations
//				return sipInputStream;
//			}else{
//				SipAnnotationUtil.processListenerAnnotations(/*_sipModule,*/ sipXmlDocument);
//				SipAnnotationUtil.processServletAnnotations(/*_sipModule,*/ sipXmlDocument, null);
//			}
//
//			//write the result back from DOM to input stream
//			XMLSerializer serializer = new XMLSerializer();
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			serializer.setOutputByteStream(baos);
//
//			serializer.serialize(sipXmlDocument);
//			byte[] array = baos.toByteArray();
//
//			outStream = new ByteArrayInputStream(array);
//
//			baos.close();
//		} catch (ParserConfigurationException e) {
//			if (c_logger.isTraceDebugEnabled()){
//				c_logger.traceDebug(this, "addSipAnnotations", "failed to add sip annotations, exception is: " + e.getMessage());
//			}
//		} catch (SAXException e) {
//			if (c_logger.isTraceDebugEnabled()){
//				c_logger.traceDebug(this, "addSipAnnotations", "failed to add sip annotations, exception is: " + e.getMessage());
//			}
//		} catch (IOException e) {
//			if (c_logger.isTraceDebugEnabled()){
//				c_logger.traceDebug(this, "addSipAnnotations", "failed to add sip annotations, exception is: " + e.getMessage());
//			}
//		}
//
//		if (c_logger.isTraceEntryExitEnabled()){
//			c_logger.traceExit(this, "addSipAnnotations");
//		}
//		
//		return outStream;
		return null;
	}
}