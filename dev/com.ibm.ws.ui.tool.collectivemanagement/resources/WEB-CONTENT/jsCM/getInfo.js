/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/* jshint loopfunc:true */
define(
		[ 'jsCM/utilOps', 'dojo/request', 'dijit/form/CheckBox', 'dojo/dom-construct',
				'dojo/i18n!./nls/cmMessages', 'dojo/parser', 'dijit/Dialog',
				'js/widgets/TextBox', 'dojox/mobile/Button', 'dijit/registry',
				'dijit/layout/AccordionContainer', 'dijit/layout/ContentPane',
				'dojo/dom', 'dojo/domReady!' ],
		function(utilOps, request, CheckBox, domConstruct, i18n, parser, Dialog, TextBox,
				Button, registry, AccordionContainer, ContentPane, dom) {
			"use strict";
			parser.parse();

			return {
				initPage : function() {
					var url = "/ibm/api/collective/v1/hosts";
					var options = {
						handleAs : 'json',
						preventCache : true
					};
					request.get(url, options).then(
							function(response) {
								var boxes = getBox("");
								var reg = createRegBox("", boxes[0], boxes[1],
										boxes[2]);
								reg.startup();
								var regButton = new Button({
									id : "regButton",
									label : i18n.REGISTER_BUTTON_LABEL,
									onClick : function(evt) {
										reg.show();
									}
								});
								registry.byId("mainContain")
										.addChild(regButton);
								
              });
        }
			};

			function getBox(cpID) {
				var box1 = new TextBox({
					id : cpID + "box1",
					value : "",
					placeHolder : i18n.HOST_NAME
				});

				var box2 = new TextBox({
					id : cpID + "box2",
					value : "",
					placeHolder : i18n.USER_NAME
				});

				var box3 = new TextBox({
					id : cpID + "box3",
					value : "",
					type : "password",
					placeHolder : i18n.USER_PWD
				});
				return [ box1, box2, box3 ];
			}

			function addToList(host, aContain) {
				var cpID = host.name + "CP";

				var boxes = getBox(cpID);
				var upd = createUpdBox(host, cpID, boxes[1], boxes[2]);
				upd.startup();
				var updButton = new Button({
					id : cpID + "updButton",
					label : i18n.UPDATE_BUTTON_LABEL,
					onClick : function(evt) {
						upd.show();
					}
				});

				var unr = createUnrBox(host, cpID);
				unr.startup();
				var unrButton = new Button({
					id : cpID + "unrButton",
					label : i18n.UNREGISTER_BUTTON_LABEL,
					onClick : function(evt) {
						unr.show();
					}
				});

				aContain.addChild(new ContentPane({
					id : cpID,
					title : host.name,
					content : "",
					doLayout : false
				}));
				registry.byId(cpID).addChild(updButton);
				registry.byId(cpID).addChild(unrButton);

				var rServList = [];
				if (host.servers.list.length > 0) {
					for ( var i = 0; i < host.servers.list.length; i++) {
						rServList[i] = host.servers.list[i];
						var cBox = new CheckBox({
							id : host.servers.list[i].name + 'cb',
							checked : false
						});
						cBox.startup();
						cBox.domNode.appendChild(domConstruct.create("label", {
							"for" : cBox.id,
							innerHTML : host.servers.list[i].name
						}));
						registry.byId(cpID).addChild(cBox);
						var rem = createRemBox(host, cpID, rServList);
						rem.startup();
						var remButton = new Button({
							id : cpID + "remButton",
							label : i18n.REMOVE_BUTTON_LABEL,
							onClick : function(evt) {
								rem.show();
							}
						});
						registry.byId(cpID).addChild(remButton);
					}
				}
			}
			function createCanButton(cpID, dialog) {
				var canButton = new Button({
					id : cpID + "DcanButton" + dialog.id,
					label : i18n.CANCEL_BUTTON_LABEL,
					onClick : function(evt) {
						dialog.hide();
					}

				});
				return canButton;
			}
			function createRegBox(cpID, box1, box2, box3) {
				var regButton = new Button({
					id : cpID + "DregButton",
					label : i18n.REGISTER_BUTTON_LABEL,
					onClick : function(evt) {
						utilOps.register(box1.value, box2.value, box3.value);
					}
				});

				var regDialog = new Dialog({
					id : cpID + "regDialog",
					style : "width: 200px"
				});
				var canButton = createCanButton(cpID, regDialog);

				regDialog.addChild(box1);
				regDialog.addChild(box2);
				regDialog.addChild(box3);
				regDialog.addChild(regButton);
				regDialog.addChild(canButton);

				return regDialog;
			}

			function createUpdBox(host, cpID, box2, box3) {
				var updButton = new Button({
					id : cpID + "DupdButton",
					label : i18n.UPDATE_BUTTON_LABEL,
					onClick : function(evt) {
						utilOps.update(host.name, box2.value, box3.value);
					}
				});

				var updDialog = new Dialog({
					id : cpID + "updDialog",
					style : "width: 200px"
				});
				var canButton = createCanButton(cpID, updDialog);
				updDialog.addChild(box2);
				updDialog.addChild(box3);
				updDialog.addChild(updButton);
				updDialog.addChild(canButton);
				return updDialog;
			}

			function createUnrBox(host, cpID) {
				var unrButton = new Button({
					id : cpID + "DunrButton",
					label : i18n.UNREGISTER_BUTTON_LABEL,
					onClick : function(evt) {
						utilOps.unregister(host.name);
					}
				});

				var unrDialog = new Dialog({
					id : cpID + "unrDialog",
					style : "width: 175px",
					content : "Are you sure you want to unregister " + host.name + "?"
				});
				var canButton = createCanButton(cpID, unrDialog);
				unrDialog.addChild(unrButton);
				unrDialog.addChild(canButton);
				return unrDialog;
			}

			function createRemBox(host, cpID, servList) {
				var remButton = new Button({
					id : cpID + "DremButton",
					label : i18n.REMOVE_BUTTON_LABEL,
					onClick : function(evt) {
						for ( var i = 0; i < servList.length; i++) {
							utilOps.remove(servList[i]);
						}
					}
				});

				var remDialog = new Dialog(
						{
							id : cpID + "remDialog",
							// style : "width: 175px",
							content : "proceeding will result in the removal of the following: \n"
						});
				var canButton = createCanButton(cpID, remDialog);
				remDialog.addChild(remButton);
				remDialog.addChild(canButton);
				return remDialog;
			}
		});