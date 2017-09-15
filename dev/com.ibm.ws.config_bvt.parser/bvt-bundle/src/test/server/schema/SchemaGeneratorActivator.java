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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import test.server.BaseHttpTest;

import com.ibm.websphere.metatype.SchemaGenerator;
import com.ibm.websphere.metatype.SchemaGeneratorOptions;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

public class SchemaGeneratorActivator extends BaseHttpTest {

    private BundleContext bundleContext;
    private SchemaGenerator schemaGenerator;
    private WsLocationAdmin locationService;

    @Override
    protected void activate(ComponentContext context) throws Exception {
        super.activate(context);
        this.bundleContext = context.getBundleContext();
        registerServlet("/schema", new SchemaGeneratorServlet(), null, null);
        System.out.println("Schema generator servlet started");
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

    class SchemaGeneratorServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        @Override
        public void doGet(HttpServletRequest rq, HttpServletResponse rsp) throws IOException {
            Bundle[] bundles = bundleContext.getBundles();

            SchemaGeneratorOptions options = new SchemaGeneratorOptions();
            options.setEncoding("UTF-8");
            options.setBundles(bundles);

            if ("file".equals(rq.getParameter("output"))) {
                // generate schema and output it to a file           
                WsResource resource = locationService.resolveResource("${server.config.dir}/schema.xsd");
                Writer writer = new OutputStreamWriter(resource.putStream(), "UTF-8");
                PrintWriter pw = rsp.getWriter();
                try {
                    schemaGenerator.generate(writer, options);

                    // Generation didn't die, woohoo!
                    rsp.setContentType("text/plain");
                    pw.println("OK. Wrote: " + resource.toExternalURI());
                } catch (Throwable t) {
                    System.err.println("Exception " + t);
                    t.printStackTrace();
                    pw.println("ERROR. Exception occurred: " + t.toString());
                } finally {
                    writer.close();
                    pw.close();
                }
            } else {
                // generate schema and output it back       
                rsp.setContentType("text/xml");
                rsp.setCharacterEncoding("UTF-8");
                PrintWriter pw = rsp.getWriter();
                schemaGenerator.generate(pw, options);
            }
        }
    }

}
