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
package com.ibm.ws.featureverifier.report;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ErrorTool extends JFrame {

    private final JPanel contentPane;
    private final JTextField textField;
    private final JList<String> configList;
    private final JTextPane txtpnErrors;
    private final JTextPane txtpnIgnores;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    ErrorTool frame = new ErrorTool();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public ErrorTool() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 735, 614);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        JPanel pathPanel = new JPanel();
        contentPane.add(pathPanel, BorderLayout.NORTH);
        pathPanel.setLayout(new BorderLayout(0, 0));

        textField = new JTextField("/home/ajo1/LIBERTY/CDOPEN/build.featureverify/build/tmp/apiCheckerBase/servers");
        pathPanel.add(textField, BorderLayout.CENTER);
        textField.setColumns(10);

        JLabel path = new JLabel("Path");

        pathPanel.add(path, BorderLayout.WEST);

        JButton goButton = new JButton("Go");

        goButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                File dir = new File(textField.getText());
                Vector<String> configNames = new Vector<String>();
                if (dir.exists() && dir.isDirectory()) {
                    for (File f : dir.listFiles()) {
                        if (!f.getName().startsWith(".")) {
                            configNames.add(f.getName());
                        }
                    }
                }
                configList.setListData(configNames);
                configList.setVisibleRowCount(Math.max(configNames.size(), 25));
                ErrorTool.this.pack();
                ErrorTool.this.setMinimumSize(ErrorTool.this.getSize());
            }

        });

        pathPanel.add(goButton, BorderLayout.EAST);

        JPanel mainContent = new JPanel();
        contentPane.add(mainContent, BorderLayout.CENTER);
        mainContent.setLayout(new BorderLayout(0, 0));

        configList = new JList<String>();
        configList.setBorder(new TitledBorder(null, "Configs", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        configList.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension d = e.getComponent().getSize();
                if (d.width < 100)
                    d.width = 100;
                e.getComponent().setSize(d);
            }
        });

        configList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                if (!lsm.isSelectionEmpty()) {
                    int idx = e.getFirstIndex();
                    String selected = configList.getModel().getElementAt(idx);

                    File junitXml = new File(textField.getText() + File.separator + selected + File.separator + "junit.xml");
                    if (junitXml.exists()) {
                        try {
                            String content = new String(Files.readAllBytes(junitXml.toPath()));
                            txtpnErrors.setText(content);
                        } catch (IOException e1) {
                            txtpnErrors.setText("Unable to read errors from " + junitXml.getAbsolutePath() + " Due to " + e1.getClass().getName() + " " + e1.getMessage());
                        }
                    }

                    Map<String, Set<String>> featureNameToIssuesMap = new HashMap<String, Set<String>>();

                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db;
                    try {
                        db = dbf.newDocumentBuilder();
                        Document doc = db.parse(junitXml);

                        NodeList nl = doc.getElementsByTagName("failure");

                        int nodecount = nl.getLength();
                        for (int i = 0; i < nodecount; i++) {
                            Node n = nl.item(i);
                            String feature = n.getAttributes().getNamedItem("message").getTextContent();
                            String text = n.getTextContent();
                            if (!featureNameToIssuesMap.containsKey(feature)) {
                                featureNameToIssuesMap.put(feature, new TreeSet<String>());
                            }
                            if (text.endsWith("For More Information, visit http://was.pok.ibm.com/xwiki/bin/view/Build/FeatureChecker")) {
                                text = text.substring(0, text.length() - "For More Information, visit http://was.pok.ibm.com/xwiki/bin/view/Build/FeatureChecker".length());
                            }

                            featureNameToIssuesMap.get(feature).add(text);
                        }
                    } catch (ParserConfigurationException ex) {
                        System.err.println("junitXml : " + junitXml.getAbsolutePath());
                        ex.printStackTrace();
                    } catch (SAXException ex) {
                        System.err.println("junitXml : " + junitXml.getAbsolutePath());
                        ex.printStackTrace();
                    } catch (IOException ex) {
                        System.err.println("junitXml : " + junitXml.getAbsolutePath());
                        ex.printStackTrace();
                    }

                    StringBuffer ignores = new StringBuffer();
                    for (Map.Entry<String, Set<String>> featureIssues : featureNameToIssuesMap.entrySet()) {
                        for (String issue : featureIssues.getValue()) {
                            if (issue.startsWith("[")) {
                                int closeIdx = issue.indexOf("]");
                                if (closeIdx == -1) {
                                    closeIdx = issue.length();
                                }
                                String shortErr = issue.substring(1, closeIdx);
                                ignores.append("<message feature=\"" + featureIssues.getKey() + "\">\\[" + shortErr + "\\]</message>\n");
                            }
                        }
                    }

                    txtpnIgnores.setText(ignores.toString());
                }
            }
        });

        mainContent.add(configList, BorderLayout.WEST);

        JPanel errorsAndIgnores = new JPanel();
        mainContent.add(errorsAndIgnores, BorderLayout.CENTER);
        errorsAndIgnores.setLayout(new GridLayout(2, 1));

        txtpnErrors = new JTextPane();
        txtpnErrors.setText("Errors");
        txtpnErrors.setBorder(new TitledBorder(null, "Errors", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        errorsAndIgnores.add(txtpnErrors);

        txtpnIgnores = new JTextPane();
        txtpnIgnores.setText("Ignores");
        txtpnIgnores.setBorder(new TitledBorder(null, "Ignores", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        errorsAndIgnores.add(txtpnIgnores);

        pack();
    }

}
