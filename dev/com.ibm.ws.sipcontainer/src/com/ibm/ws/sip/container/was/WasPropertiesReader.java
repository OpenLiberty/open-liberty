/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
//package com.ibm.ws.sip.container.was;
//
//import javax.xml.parsers.ParserConfigurationException;
//
//import com.ibm.sip.util.log.Log;
//import com.ibm.sip.util.log.LogMgr;
//import com.ibm.ws.sip.container.properties.BaseReader;
//import com.ibm.ws.sip.properties.CustPropSource;
//import com.ibm.ws.sip.properties.SipPropertiesMap;
////TODO Liberty - change to Liberty configuration reading method
////import com.ibm.websphere.management.AdminService;
////import com.ibm.websphere.management.AdminServiceFactory;
////import com.ibm.ws.ctmodels.config.process.CT_Server;
////import com.ibm.wsspi.runtime.config.ConfigObject;
////import com.ibm.wsspi.runtime.config.ConfigScope;
////import com.ibm.wsspi.runtime.config.ConfigService;
////import com.ibm.wsspi.runtime.service.WsServiceRegistry;
///**
// * @author yaronr
// * 
// * Read properties from configuration files in a WAS standalone (no LWP)
// * configuration
// * 
// * Should read properties from three locations:
// *  1) sipcellcfg.xml located in config\cells\cell name\
// *  2) sipnodegfg.xml located in config\cells\cell name\nodes\node name
// *  3) sipservercfg.xml located in
// * 			 config\cells\cell name\nodes\node name\servers\server name
// */
//public class WasPropertiesReader extends BaseReader
//{
//	protected static WasPropertiesReader  s_instance=null;
//
//    /**
//     * Used to get the was installation root
//     */
//    private static final String USER_INSTALL_ROOT = "user.install.root";
//
//    /**
//     * Used while building paths to configuration files
//     */
//    private static final String CONFIG_CELLS  = "/config/cells/";
//    private static final String CLUSTERS = "/clusters/"; 
//    private static final String NODES  = "/nodes/";
//    private static final String SERVERS  = "/servers/";
//    private static final String CELL_CFG  = "sipcellcfg.xml";
//    private static final String NODE_CFG  = "sipnodecfg.xml";
//    private static final String CLUSTER_CFG  = "sipclustercfg.xml";
//    private static final String SERVER_CFG  = "sipservercfg.xml";
//    private static final String SERVER_XML_FILE = "server.xml";
//    
//    /**
//     * "Number of threads" key name in lwpsip.xml. The nubmer of dispatching
//     * thread that will be used to send requests/responses into the Websphere
//     * App Server.
//     */
//    protected static final String THREADS_NUMBER_WCCM_KEY = "dispatchThreads";
//    
//    /**
//     * Class Logger.
//     */
//    private static final LogMgr c_logger = Log.get(WasPropertiesReader.class);
//
//    /**
//     * Holds cell name
//     */
//    protected String m_cell = null;
//
//    /**
//     * Holds node name
//     */
//    protected String m_node = null;
//    
//    /**
//     * Holds cluster name
//     */
//    protected String m_cluster = null;
//
//    /**
//     * Holds server name
//     */
//    protected String m_server = null;
//
//    /**
//     * SIP configuration reader which parse the xml file
//     */
//    SipConfigurationFileReader m_reader;
//	    
//    /**
//     * CTor
//     * 
//     * @throws ParserConfigurationException
//     */
//    protected WasPropertiesReader() throws ParserConfigurationException
//    {
//        // Create the file reader
//        m_reader = new SipConfigurationFileReader();
//
//        // get the cell,node and server names from the admin service
//        getNames();
//    }
//
//    /**
//     * get the cell, node and server names from the admin service
//     */
//    private void getNames()
//    {
//    	/*TODO Liberty AdminService admin = null;
//    	//	Get WAS admin service
//        try {
//        	admin = AdminServiceFactory.getAdminService();
//        }
//        catch(NoClassDefFoundError e) {
//        	//Should only get here in standalone env
//        	e.printStackTrace();
//        }
//        
//        if (null != admin)
//        {
//            m_cell = admin.getCellName();
//            m_node = admin.getNodeName();
//            m_server = admin.getProcessName();
//        }
//        
//        try {
//        	//try to get the cluster name for this server
//			ConfigService configService = (ConfigService)WsServiceRegistry.getService(this, ConfigService.class);
//			List list = configService.getDocumentObjects(configService.getScope(ConfigScope.SERVER), SERVER_XML_FILE);
//			
//			//get the current server configObject
//			ConfigObject configObject = (ConfigObject) list.get(0);
//			
//			//if the cluster name is not found the default value in null
//			m_cluster = configObject.getString(CT_Server.CLUSTERNAME_NAME, null);
//		} catch (Exception e) {
//			//Should only get here in standalone env
//			if (c_logger.isTraceDebugEnabled())	{
//				c_logger.traceDebug(this, "getNames", 
//						"error on getting the cluster name from the configuration", e);
//			}
//		}
//        
//        if (c_logger.isTraceDebugEnabled())
//        {
//            if ((null == m_cell) || (null == m_node) || (null == m_server))
//            {
//                c_logger.traceDebug(this, "WasPropertiesReader",
//                        "ERROR!! unable to get names");
//            }
//            else
//            {
//                StringBuffer buffer = new StringBuffer("cell: ");
//                buffer.append(m_cell);
//                buffer.append(", node: ");
//                buffer.append(m_node);
//                buffer.append(", server: ");
//                buffer.append(m_server);
//                buffer.append(", cluster: ");
//                buffer.append(m_cluster);
//                c_logger.traceDebug(this, "WasPropertiesReader", buffer.toString());
//            }
//        }*/
//    }
//    
//    /**
//     * Read the properties
//     * 
//     * @return the properties list
//     */
//   public SipPropertiesMap getProperties()
//   {
//       // Have we been here?
//       if (null == m_properties)
//       {
//           // create the properties
//           m_properties = new SipPropertiesMap();
//
//           // load default properties
//           loadDefaultProperties(); 
////           readSystemProps(m_properties);
//           //TODO Liberty - delete WCCM configuration reading:
//           //readWCCMConfiguration();
//           
//           // read configuration 
//           //readConfiguration();
//           
//           // updates the class SarToWarProperties with the value of the property "SUPPORT_SAR_TO_WAR"
//           // and property "SUPPORT_AMM_ANNOTATION_READING"
//           // defect #722864
//           //TODO Liberty - not sure we need those properties anymore
////       	   boolean supportSarToWar = m_properties.getBoolean(CoreProperties.SUPPORT_SAR_TO_WAR);
////       	   SarToWarProperties.setSupportSarToWar(supportSarToWar);
////       	   boolean supportAmmAnnotationReading = m_properties.getBoolean(CoreProperties.SUPPORT_AMM_ANNOTATION_READING);
////       	   SarToWarProperties.setSupportAmmAnnotationReading(supportAmmAnnotationReading);
//
//           // load (override) properties from the sipcontainer.xml file
//           //loadPropertiesFromFile(SIP_CONTAINER_PROPERTIES);
//           if (c_logger.isTraceDebugEnabled())
//           {
////               c_logger.traceDebug(this, "getProperties", 
////               "Properties set by the configuration files: " + m_properties.logProprs(CustPropSource.CONFIG_FILE));
//               
//               c_logger.traceDebug(this, "getProperties", 
//               "Properties set by default values: " + m_properties.logProprs(CustPropSource.DEFAULT));
//           }
//       }
//
//       return m_properties;
//   }
//
//    /**
//     * Read the properties from all configuration file
//     */
//   //TODO Liberty - we don't need this method in Liberty 
//   /*private void readConfiguration()
//    {
//    	if (c_logger.isTraceEntryExitEnabled()) {
//			c_logger.traceEntry(WasPropertiesReader.class.getName(),
//					"readConfiguration");
//		}
//    	
//        // Get the WAS installation directory    
//        StringBuffer path = new StringBuffer(System.getProperty(USER_INSTALL_ROOT));
//        
//        // Build path to cell
//        path.append(CONFIG_CELLS);
//        path.append(m_cell);
//        path.append(System.getProperty("file.separator"));
//        
//        // Get cell properties
//        String fileName = path.toString() + CELL_CFG;
//        appendPropertiesFromFile(fileName);
//        
//        // Build path to node
//        path.append(NODES);
//        path.append(m_node);
//        path.append(System.getProperty("file.separator"));
//        
//        // Get cell properties
//        fileName = path.toString() + NODE_CFG;
//        appendPropertiesFromFile(fileName);
//        
//        // Build path to servers
//        path.append(SERVERS);
//        path.append(m_server);
//        path.append(System.getProperty("file.separator"));
//        
//        // Get cell properties
//        fileName = path.toString() + SERVER_CFG;
//        appendPropertiesFromFile(fileName);
//    }*/
//
//    //TODO Liberty : we don't need this method in Liberty
////    /**
////     * Appends properties read from the specified file. 
////     * @param fileName
////     */
////    private void appendPropertiesFromFile(String fileName) 
////    {
////        Properties prop = getPropertiesFromFile(fileName);
////        if(null != prop)
////        {
////            if (c_logger.isTraceDebugEnabled()) {
////                c_logger.traceDebug(this, "appendPropertiesFromFile", 
////                    "Appending from " + fileName + "\n" + prop.toString());
////            }
////            
////            if(prop.containsKey(LISTENING_POINT_1))
////        	{
////        		removeDefaultListeningPoints();
////        	}
////            
////            m_properties.putAll(prop, CustPropSource.CONFIG_FILE);
////        }
////        else
////        {
////            if (c_logger.isTraceDebugEnabled()) {
////                c_logger.traceDebug(this, "appendPropertiesFromFile", 
////                    "Unable to read configuration from : " + fileName);
////            }
////        }
////    }
//
////    /**
////     * @param string path to configuration file
////     * @return Properties in this file
////     */
////    private Properties getPropertiesFromFile(String path)
////    {
////        InputStream instream;
////        String msg;
////       
////        try
////        {
////            // open the file and parse it
////            instream = new FileInputStream(new File(path));
////            return m_reader.parse(instream);
////        }
////        catch (FileNotFoundException enfe)
////        {
////           msg = new String(path + " was not found");
////        }
////        catch (SAXException saxe)
////        {
////            msg = new String("sax exception in " + path);
////        }
////        catch (IOException ioe)
////        {
////            msg = new String("io exception in " + path);
////        }
////        
////        if(c_logger.isTraceDebugEnabled())
////        {
////            c_logger.traceDebug(this, "getPropertiesFromFile", msg);
////            
////        }
////        
////        return null;
////    }
//
////    public static void main(String[] args)
////    {
////        try
////        {
////            // Create a reader
////            WasPropertiesReader reader = new WasPropertiesReader();
////
////            Properties p;
////    
////            //	Set names dor debugging
////            reader.setNames("poncho", "poncho", "poncho");
////            p = reader.getProperties();
////
////            System.out.println("********  Properties *******");
////            System.out.println(p.toString());
////
////            // Set names dor debugging
////            reader.setNames("poncho", "poncho", "other");
////            p = reader.getProperties();
////
////            System.out.println("********  Properties *******");
////            System.out.println(p.toString());
////            
//// 
////        }
////        catch (ParserConfigurationException e)
////        {
////            // TODO Auto-generated catch block
////            e.printStackTrace();
////        }
////    }
////    
////    /**
////     * For debug purpose
////     */
////    private void setNames(String cell, String node, String server)
////    {
////        m_cell = cell;
////        m_node = node;
////        m_server = server;
////    }
//
//      public static WasPropertiesReader getReader() throws ParserConfigurationException{
//		if(s_instance==null){
//			s_instance = new WasPropertiesReader ();
//		}
//		return s_instance;
//      }
//      
//      public void readSystemProps(SipPropertiesMap properties){
//    	//Empty - should be implemented by each WAS driver 
//        // due to different implementations
//      }
//      
////      /**
////       * WCCM configuration - should be read first before any property file 
////       */
////     protected void readWCCMConfiguration(){ 
////         //Empty - should be implemented by each WAS driver that uses WCCM
////         // due to different implementations
////     }
//}
//
//  