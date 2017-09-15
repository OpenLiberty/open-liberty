/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.schema;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.w3c.dom.Document;

import test.server.BaseHttpTest;

import com.ibm.websphere.metatype.SchemaGenerator;
import com.ibm.websphere.metatype.SchemaGeneratorOptions;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;

public class TestRunner extends BaseHttpTest {

    private BundleContext bundleContext;
    private SchemaGenerator schemaGenerator;
    private WsLocationAdmin locationService;

    @Override
    protected void activate(ComponentContext context) throws Exception {
        super.activate(context);

        bundleContext = context.getBundleContext();

        registerServlet("/schema-test", new TestVerifierServlet(), null, null);
    }

    protected void setSchemaGenerator(SchemaGenerator ref) {
        this.schemaGenerator = ref;
    }

    protected void unsetSchemaGenerator(SchemaGenerator ref) {
        if (ref == this.schemaGenerator) {
            this.schemaGenerator = null;
        }
    }

    protected void setLocationService(WsLocationAdmin ref) {
        this.locationService = ref;
    }

    protected void unsetLocationService(WsLocationAdmin ref) {
        if (ref == this.locationService) {
            this.locationService = null;
        }
    }

    class TestVerifierServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        @Override
        public void doGet(HttpServletRequest rq, HttpServletResponse rsp) throws IOException {
            PrintWriter pw = rsp.getWriter();
            rsp.setContentType("text/plain");

            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                SchemaGeneratorOptions options = new SchemaGeneratorOptions();
                options.setEncoding("UTF-8");
                options.setBundles(bundleContext.getBundles());
                schemaGenerator.generate(out, options);
                out.flush();

                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = schemaFactory.newSchema(new StreamSource(new ByteArrayInputStream(out.toByteArray())));

                DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
                domFactory.setNamespaceAware(true);
                DocumentBuilder domBuilder = domFactory.newDocumentBuilder();

                WsResource resource = locationService.resolveResource(WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR + "test.xml");

                Document document = domBuilder.parse(resource.get());

                schema.newValidator().validate(new DOMSource(document));

                pw.println("OK");
            } catch (Exception e) {
                pw.println("FAILED");
                e.printStackTrace(pw);
            }
        }

    }

}
