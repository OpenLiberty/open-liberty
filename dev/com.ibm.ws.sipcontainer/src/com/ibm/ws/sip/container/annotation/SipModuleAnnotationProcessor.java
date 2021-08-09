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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
//TODO Liberty Annotation import org.apache.xml.serialize.XMLSerializer;
//TODO Liberty Annotation - check if those 3 imports are ok in Liberty or not
//import org.apache.tools.ant.Project;
//import org.apache.tools.ant.taskdefs.Delete;
//import org.apache.tools.ant.taskdefs.Zip;

//TODO Liberty find the rplace Logginghelper in Liberty
//import com.ibm.ws.logging.LoggerHelper;
//import com.ibm.ws.logging.WsLogger;
//TODO Liberty use Liberty annnotation reading process instead of tWas ECS
//import com.ibm.wsspi.ecs.exception.ModuleOpenException;
//import com.ibm.wsspi.ecs.module.ModuleFactory;
//import com.ibm.wsspi.ecs.module.WebModule;
//import com.ibm.wsspi.ecs.target.PackageAnnotationTarget;

/**
 * This class is used to enhance sip.xml of SIP module 
 * with annotation meta data specified in it's classes.
 * @author Roman Mandeleil
 */
public class SipModuleAnnotationProcessor {
	//marker file that is added to the web module if we found and serialized sip annotations to the web/sip.xml
	private static final String SIP_ANNOTATION_MARKER_FILE = "/WEB-INF/sipAnnotation.read";
	
	//using logger directly from classes that are related to the deploy process. 
	//we cannot use our custom logger since it is not working with client side tracing when using CTS deployer
    private static final String className = SipModuleAnnotationProcessor.class.getName(); 
	private static Logger c_logger = null;
    static 
	{
    	c_logger = Logger.getLogger(className);
    	/*TODO Liberty if(c_logger instanceof WsLogger){
    		LoggerHelper.addLoggerToGroup(c_logger, "Webui");
    	}*/
	}
	
	private String    pathToModule   = null;
	//TODO Libertyprivate WebModule sipModule      = null;
	private Document  sipXmlDocument = null;
	private Document  webXmlDocument = null;
	private String    sipAppName     = "";
	private boolean   is289          = false;

	/**
	 * @param pathToModule - path to the sip module jar
	 * @throws Exception - if sip module is not a valid sip module.  
	 */
	public SipModuleAnnotationProcessor(@SuppressWarnings("hiding") String pathToModule) throws  NotSipModuleException,  IOException, ParserConfigurationException/*TODO Liberty, ModuleOpenException*/{
		
		if (c_logger.isLoggable(Level.FINER)){
			c_logger.entering(className, "SipModuleAnnotationProcessor", pathToModule);
		}
		
		this.pathToModule = pathToModule;
		
		// Specify factory for ECS factory.
        /*System.setProperty (ModuleFactory.class.getName(),
                "com.ibm.ws.ecs.internal.wtp.module.impl.WTPModuleFactory");*/

   	    InputStream webInputStream = null;
		File moduleFile = new File(pathToModule);
		JarFile jarFile = new JarFile(moduleFile);
		
		ZipEntry webInf = jarFile.getEntry("WEB-INF");
		if (webInf == null) throw new NotSipModuleException("No WEB-INF directory, it not a SIP module file");
		
		ZipEntry webxml = jarFile.getEntry("WEB-INF/web.xml");
		
		if (webxml != null) {
			webInputStream = jarFile.getInputStream(webxml);
		}
		
		if (webxml == null){
			File webinf = new File(pathToModule + "dir/WEB-INF");
			webinf.mkdirs();
			
			File webxmlfile = new File(pathToModule + "dir/WEB-INF/web.xml");
			
			File moduleJar           = new File(pathToModule);
			File moduleJarUpdateDir  = new File(pathToModule + "dir");
			
			
			FileOutputStream fos = new FileOutputStream(webxmlfile);
			fos.write(SipAnnotationUtil.getBaicWebXml().getBytes());
			fos.close();
			
			jarFile.close();
			
			//set the time of this file 60 seconds earlier, if we will need to replace this file later on in the process
			//we need it to be 2 seconds older from the new web.xml file so ant will identify it as new file and will 
			//replace it.
			webxmlfile.setLastModified(System.currentTimeMillis() - 60000L);
			
			/*TODO Liberty Project antProject = new Project();
			Zip zipTask = new Zip();
			zipTask.setBasedir(moduleJarUpdateDir);
			zipTask.setDestFile(moduleJar);
			zipTask.setProject(antProject);
			zipTask.setUpdate(true);
			
			zipTask.execute();
			
			Delete delete = new Delete();
			delete.setDir(moduleJarUpdateDir);
			delete.setProject(antProject);
			
			delete.execute();
			jarFile = new JarFile(moduleFile);*/
			
			webInputStream = new ByteArrayInputStream(SipAnnotationUtil.getBaicWebXml().getBytes());
		}
		
		//hold the web.xml dom document for future use
		DocumentBuilderFactory xmlParser = DocumentBuilderFactory.newInstance();
		xmlParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		xmlParser.setNamespaceAware(true);
		DocumentBuilder builder = xmlParser.newDocumentBuilder();
		DefaultHandler dh = new DefaultHandler (){
			public void error(SAXParseException saxparseexception) throws SAXException {
				throw saxparseexception;
			}
		};
		
		 builder.setErrorHandler(dh);
		 try {
			this.webXmlDocument = builder.parse(webInputStream);
			webInputStream.close();
		} catch (SAXException e) {
			jarFile.close();
			throw new NotSipModuleException("Not a sip container module, can not parse web.xml");
		}
		
		//read the sip.xml and store it for future use, if not exist use default value
		InputStream sipInputStream = null;
		ZipEntry jarEntry = jarFile.getEntry("WEB-INF/sip.xml");
		if (jarEntry != null) {
			sipInputStream = jarFile.getInputStream(jarEntry);
		}
		
		//TODO Liberty replace current implementation 
		/*ModuleFactory moduleFactory = ModuleFactory.getInstance();
	    this.sipModule = (WebModule) moduleFactory.openWebModule(pathToModule);*/
			
		if (sipInputStream == null){			
			// If no file was opened look for annotation define sip application
			/*Map<String, PackageAnnotationTarget>  sipAppAnnotationMap = 
				sipModule.getAnnotationScanner().getPackageAnnotationTargets(SipApplication.class);

			if (sipAppAnnotationMap.values().isEmpty()) {
				if (c_logger.isLoggable(Level.FINEST)){
					c_logger.finest("closing resources for none SIP module");
				}
				sipModule.close();
				jarFile.close();
				throw new NotSipModuleException("Not a sip container module, define sip.xml or use @SipApplication annotation.");
			}*/
			
			sipInputStream = new ByteArrayInputStream(SipAnnotationUtil.getBaicSipXml().getBytes());
		}
		
        try {
        	//Parsing sip.xml with schema validation
	        DocumentBuilder builder2 = xmlParser.newDocumentBuilder();
	        builder2.setErrorHandler(dh);
	        
		    this.sipXmlDocument = builder2.parse(sipInputStream);
		    
		    //do the actual validation against the parsed document
    		SipAnnotationUtil.validateJsr289(sipXmlDocument);
		    setIs289(true);

        } catch (SAXException exception) {
        	if (c_logger.isLoggable(Level.FINEST)){
				c_logger.finest("Failed to parse Sip.xml with jsr1.1 schema validation, trying without schema validation, message is: " + exception.getMessage());
			}
        	
        	// Try parsing without schema
        	DocumentBuilder builder2 = xmlParser.newDocumentBuilder();
	        builder2.setErrorHandler(dh);
	        
	        jarEntry = jarFile.getEntry("WEB-INF/sip.xml");
			if (jarEntry != null) {
				sipInputStream = jarFile.getInputStream(jarEntry);
			}
	        
	        setIs289(false);

	        try {
				this.sipXmlDocument = builder2.parse(sipInputStream);
			} catch (SAXException saxExc) {
				if (c_logger.isLoggable(Level.FINE)){
					c_logger.log(Level.FINE, className, "Unable to parse sipXml -> SAXException: " + saxExc);
				}
				throw new NotSipModuleException("Unable to parse sipXml", saxExc);
			}
			catch (Exception exs) {
				if (c_logger.isLoggable(Level.FINE)){
					c_logger.log(Level.FINE, className, "Unable to parse sipXml -> Exception: " + exs);
				}
				throw new NotSipModuleException("Exception: Unable to parse sipXml", exs);
			}
		}

        sipInputStream.close();
		jarFile.close();
		
		// Anat: The following code was remove since sipModule should be always created.
		// Fixed in PM62532
		//if (is289() && sipModule == null) {
		//	ModuleFactory moduleFactory = ModuleFactory.getInstance();
		//	sipModule = (WebModule) moduleFactory.openWebModule(pathToModule);
		//}
		
		if (c_logger.isLoggable(Level.FINER)){
			c_logger.exiting(className, "SipModuleAnnotationProcessor");
		}
	}
	
	// No default ctor
	@SuppressWarnings("unused")
	private SipModuleAnnotationProcessor(){}
	
	/**
	 * Save the enhanced file to the disc
	 */
	public void saveSipXml() {
		
		// TODO Liberty - remove the comment
		
//		try {
//			
//			if (c_logger.isLoggable(Level.FINER)) {
//                c_logger.log(Level.FINER, className, "Open File at -> " + pathToModule + "dir/WEB-INF");
//            }
//			File xmlFile = new File(pathToModule + "dir/WEB-INF");
//			xmlFile.mkdirs();
//			
//			//create the sip annotation marker file
//			if (c_logger.isLoggable(Level.FINER)) {
//                c_logger.log(Level.FINER, className, "sipMarker File here->" + pathToModule + "dir" + SIP_ANNOTATION_MARKER_FILE);
//            }
//			File sipMarker = new File(pathToModule + "dir" + SIP_ANNOTATION_MARKER_FILE);
//			sipMarker.createNewFile();
//			
//			XMLSerializer serializer = new XMLSerializer();
//			if (c_logger.isLoggable(Level.FINER)) {
//                c_logger.log(Level.FINER, className, "fileWriter File here->" + pathToModule + "dir" + "/WEB-INF/sip.xml");
//            }
//			FileWriter fileWriter = new FileWriter(pathToModule + "dir" + "/WEB-INF/sip.xml");
//			serializer.setOutputCharStream(fileWriter);
//			serializer.serialize(sipXmlDocument);
//			
//			File moduleJar           = new File(pathToModule);
//			File moduleJarUpdateDir  = new File(pathToModule + "dir");
//			/*TODO Liberty if(this.sipModule == null){
//				if (c_logger.isLoggable(Level.SEVERE)) {
//	                c_logger.logp(Level.SEVERE, className, "saveSipXml", "this.sipModule is null");
//	            }
//			}
//			else{
//				this.sipModule.close();
//			}*/
//			fileWriter.close();
//			
//			
//			/*TODO Liberty Project antProject = new Project();
//			Zip zipTask = new Zip();
//			zipTask.setBasedir(moduleJarUpdateDir);
//			zipTask.setDestFile(moduleJar);
//			zipTask.setProject(antProject);
//			zipTask.setUpdate(true);
//			
//			zipTask.execute();
//			
//			Delete delete = new Delete();
//			delete.setDir(moduleJarUpdateDir);
//			delete.setProject(antProject);
//			
//			delete.execute();*/
//			
//		} catch (IOException exception) {
//			if(c_logger.isLoggable(Level.SEVERE)){
//				c_logger.severe("Can't save a sip.xml file = " + exception.getStackTrace());
//            }
//		}
//		catch (Exception exception) {
//			if (c_logger.isLoggable(Level.SEVERE)) {
//                c_logger.logp(Level.SEVERE, className, "Exception in saveSipXml", "Can't save a sip.xml file", exception.getStackTrace());
//			}
//		}
		
	}
	
	/**
	 * Save the enhanced files to the disc (sip and web xmls)
	 */
	public void saveSipAndWebXml() {
		// TODO Liberty - remove the comment
		
		
		// try {
//			
//			File xmlFile = new File(pathToModule + "dir/WEB-INF");
//			xmlFile.mkdirs();
//			
//			//create the sip annotation marker file
//			File sipMarker = new File(pathToModule + "dir" + SIP_ANNOTATION_MARKER_FILE);
//			sipMarker.createNewFile();
//			
//			XMLSerializer serializer = new XMLSerializer();
//			FileWriter sipFileWriter = new FileWriter(pathToModule + "dir" + "/WEB-INF/sip.xml");
//			FileWriter webFileWriter = new FileWriter(pathToModule + "dir" + "/WEB-INF/web.xml");
//			
//			serializer.setOutputCharStream(sipFileWriter);
//			serializer.serialize(sipXmlDocument);
//			
//			serializer.setOutputCharStream(webFileWriter);
//			serializer.serialize(webXmlDocument);
//			
//			File moduleJar           = new File(pathToModule);
//			File moduleJarUpdateDir  = new File(pathToModule + "dir");
//			
//			/*TODO Liberty this.sipModule.close();*/
//			sipFileWriter.close();
//			webFileWriter.close();
//			
//			/*TODO Liberty Project antProject = new Project();
//			Zip zipTask = new Zip();
//			zipTask.setBasedir(moduleJarUpdateDir);
//			zipTask.setDestFile(moduleJar);
//			zipTask.setProject(antProject);
//			zipTask.setUpdate(true);
//
//			
//			zipTask.execute();
//			
//			Delete delete = new Delete();
//			delete.setDir(moduleJarUpdateDir);
//			delete.setProject(antProject);
//			
//			delete.execute();*/
//			
//		} catch (IOException exception) {
//			if(c_logger.isLoggable(Level.SEVERE)){
//				c_logger.severe("Can't save a sip.xml and/or web.xml file = "+exception);
//			}
//		}
	}

	/**
	 * setter
	 * @param sipAppName
	 */
	public void setSipAppName(String sipAppName) {
		this.sipAppName = sipAppName;
	}

	/**
	 * getter
	 */
	public String getSipAppName() {
		return sipAppName;
	}

	/**
	 * setter 
	 * @param is289
	 */
	public void setIs289(boolean is289) {
		this.is289 = is289;
	}

	/**
	 * getter
	 */
	public boolean is289() {
		return is289;
	}

}
