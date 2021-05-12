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
define(
    [ 'jsExplore/widgets/shared/ActionButtons', 'dojo/i18n!../../nls/explorerMessages', 'gridx/core/model/cache/Sync', 'gridx/Grid',
        'gridx/modules/HeaderMenu', 'gridx/modules/HiddenColumns',
        'gridx/modules/Filter', 
        'gridx/modules/select/Column', 
        'gridx/modules/select/Row', 'gridx/modules/move/Column', 'gridx/modules/dnd/Column',
        'gridx/modules/SingleSort', 'gridx/core/model/extensions/FormatSort', 'gridx/modules/CellWidget', 
        'gridx/modules/ColumnResizer', 'gridx/modules/Body',
        'dojo/store/Memory', 'dojo/_base/lang', 'dojo/on',
        'dojo/Deferred', 'dojo/dom', 'jsExplore/widgets/shared/ListViewChart',
        'gridx/modules/Pagination', 'gridx/modules/pagination/PaginationBar', 'dojo/dom-construct', "dojo/dom-style", "dojo/dom-class",
        'jsExplore/resources/utils', 'dojox/string/BidiComplex',
        "dijit/form/ToggleButton", 'dojo/dom-attr',
        'jsExplore/widgets/shared/ListViewAlertIcon', 'jsExplore/widgets/TagPane', 'dijit/layout/ContentPane',
        'jsExplore/widgets/shared/ListViewTextWithImage', 'jsExplore/utils/ID', 'jsShared/utils/imgUtils', 
        'dijit/registry', 'dojo/keys', 'dojo/aspect'],
    function(ActionButtons, i18n, Cache, Grid, HeaderMenu, HiddenColumns, Filter,  
        selectColumn, selectRow, moveColumn,
        dndColumn, Sort, FormatSort, CellWidget, ColumnResizer, Body, Memory, lang, on,
        Deferred, dom, ListViewChart, Pagination, PaginationBar, domConstruct, domStyle, domClass,
        utils, BidiComplex, ToggleButton, domAttr,
        ListViewAlertIcon, TagPane, ContentPane, 
        ListViewTextWithImage, ID, imgUtils, registry, keys, aspect) {
      

      return { 
          getServerGrid : __getServerGrid,
          getServerData : __getServerData,
          getApplicationGrid : __getApplicationGrid,
          getApplicationData : __getApplicationData,
          getClusterGrid : __getClusterGrid,
          getClusterData : __getClusterData,
          getHostGrid : __getHostGrid,
          getHostData : __getHostData,
          getRuntimeGrid : __getRuntimeGrid,
          getRuntimeData : __getRuntimeData,
          setRowBackgroundColor : __setRowBackgroundColor,
          clearRowSelection : __clearRowSelection
      };

      function __getServerStructure() {

        var structure = [
            {
              id : ID.getAlert(),
              field : 'alert',
              name : i18n.ALERT,
              width : '5%',
              widgetsInCell : true,
              decorator : function() {
                return "<div style='text-align: center;' data-dojo-attach-point='alertIcon'></div>";
              },
              setCellValue : function (listViewAlertIcon) {
                this.alertIcon.innerHTML = listViewAlertIcon.getHTML();
              },
              comparator : function(obj1, obj2) {
                return __alertComparator(obj1, obj2);
              }
            },
            {
                id : ID.getName(),
                field : 'name',
                name : i18n.GRID_HEADER_NAME,
                width : '15%',
                widgetsInCell : true,
                decorator : function() {
                  return "<div class='listViewTitleText'><span data-dojo-attach-point='name'></span></div>";
                },
                setCellValue : function(obj) {
                  if (obj.isCollectiveController) {
                    var cellValue = new ListViewTextWithImage ({
                      resource: obj
                    });
                    this.name.innerHTML = cellValue.domNode.innerHTML;
                  } else {
                    this.name.innerHTML = '<span dir="' + utils.getStringTextDirection(obj.name) + '"><a href="#explore/servers/' + obj.id + '" title="' + obj.name + '">' + obj.name + '</a></span>';
                  }
                  if (this._onKeydownHandler) {
                    // Remove previously connected events to avoid memory leak.
                    this._onKeydownHandler.remove();
                  }
                  this._onKeydownHandler = on(this.name, "keydown", function(evt) {
                    if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
                      evt.srcElement.click();
                    }
                  });
                },
                comparator : function(obj1, obj2) {
                  return __serverNameComparator(obj1, obj2);
                }
            }, {
                id : ID.getUserDirNoCaps(),
                field : 'userdir',
                name : i18n.GRID_HEADER_USERDIR,
                display : true,
                width : '20%',
                widgetsInCell : true,
                decorator : function() {
                  return "<div class='listViewTitleText' data-dojo-attach-point='userdir'></div>";
                },
                setCellValue : function(data) {
                  // need to handle userdir correctly for bidi; only process if not ltr
                    if (utils.getBidiTextDirectionSetting() !== "ltr") {
                      this.userdir.innerHTML = BidiComplex.createDisplayString(data, "FILE_PATH");
                      this.userdir.title = BidiComplex.createDisplayString(data, "FILE_PATH");
                    } else {
                      this.userdir.innerHTML = data;
                      this.userdir.title = data;
                    }
                },
                comparator : function(obj1, obj2) {
                  if (utils.getBidiTextDirectionSetting() !== "ltr") {
                    return BidiComplex.createDisplayString(obj1, "FILE_PATH").localeCompare(BidiComplex.createDisplayString(obj2, "FILE_PATH"));
                  } else {
                    return obj1.localeCompare(obj2);
                  }
                }
            }, {
                id : ID.getState(),
                field : 'state',
                name : i18n.SEARCH_RESOURCE_STATE,
                display : true,
                width : '15%',
                widgetsInCell : true,
                decorator : function() {
                  return "<div class='listViewState'><span data-dojo-attach-point='state'></span></div>";
                },
                setCellValue : function(stateIcon) {
                  this.state.innerHTML = stateIcon.getHTML();
                },
                comparator : function(obj1, obj2) {
                  return __stateComparator(obj1, obj2);
                }
            }, {
                id : ID.getApps(),
                field : 'apps',
                name : i18n.APPLICATIONS,
                display : true,
                width : '17%',
                widgetsInCell : true,
                decorator : function(s) {
                  return "<div class='listViewApps'><span data-dojo-attach-point='apps'></span></div>";
                },
                sortFormatted: true,
                setCellValue : function(server) {
                  this.apps.innerHTML = "";
                  //var me = this;
                  this.apps.innerHTML = __getAppsDisplayString(server);

                },
                comparator : function(obj1, obj2) {
                  return __compareAppsDisplayString(obj1, obj2);
                }
            }, {
                id : 'host',
                field : 'host',
                name : i18n.HOST,
                display : true,
                width : '17%',
                widgetsInCell : true,
                decorator : function(s) {
                  return "<div class='listViewTitleText' data-dojo-attach-point='host'></div>";
                },              
                setCellValue : function(server) {
                  var hostName = server.host;
                  this.host.innerHTML = '<span dir="' + utils.getStringTextDirection(hostName) + '"><a href="#explore/hosts/' + hostName + '" title="' + hostName + '">' + hostName + '</a></span>';
                  if (this._onKeydownHandler) {
                    // Remove previously connected events to avoid memory leak.
                    this._onKeydownHandler.remove();
                  }
                  this._onKeydownHandler = on(this.host, "keydown", function(evt) {
                    if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
                      evt.srcElement.click();
                    }
                  });
                },
                comparator : function(obj1, obj2) {
                  return obj1.host.localeCompare(obj2.host);
                }
            }, {
                id : ID.getTags(),
                field : 'tags',
                name : i18n.TAGS,
                display : true,
                width : '20%',
                widgetsInCell : true,
                decorator : function(s) {
                  return "<div class='listViewTags' data-dojo-attach-point='tagPane'></div>";
                },
                setCellValue : function(server) {
                  var cp = new TagPane([server, 'tag']);
                  domConstruct.empty(this.tagPane);
                  cp.placeAt(this.tagPane);
                }
            }, {
              id : ID.getContainer(),
              field : 'container',
              name : i18n.CONTAINER,
              display : false,
              width : '10%',
              widgetsInCell : true,
              decorator : function(s) {
                return "<div class='listViewTitleText' data-dojo-attach-point='container'></div>";
              },
              setCellValue : function(type) {
                if (constants.CONTAINER_DOCKER === type) {
                  this.container.innerHTML = i18n.SEARCH_RESOURCE_CONTAINER_DOCKER;
                } else {
                  this.container.innerHTML = i18n.SEARCH_RESOURCE_CONTAINER_NONE;
                }
              }
            }, {
                id : ID.getActions(),
                field : 'actions',
                name : i18n.ACTIONS,
                display : true,
                width : '8%',
                widgetsInCell : true,
                decorator : function(s) {
                  return "<div class='listViewAction'><span data-dojo-attach-point='action'></span></div>";
                },
                setCellValue : function(data, resource, cellWidget) {
                  __setActionCell(resource, this.action, cellWidget);
                }
            }
                ];

        return structure;
      };

      function __getServerData(list) {
        var store = new Memory();

        if (list) {
          list.query().forEach(function(server) {
            var alertIcon = new ListViewAlertIcon({
              resource: server
            });

            var stateIcon = new StateIcon({
                parentId : ID.dashDelimit(ID.getSearchView(), server.id),
                resource : server,
                size : '20',
                cardState : true,
                showLabel : true,
                labelClass : 'listViewStateLabel'
            });
            
            var rowData = {
                id : server.id,
                alert : alertIcon,
                name: server,
                userdir : server.userdir,
                state : stateIcon,
                apps : server,
                host : server,
                tags : server,
                container : server.containerType,
                actions : server
            };

            store.put(rowData);
          });
        }
        return store;
      }
      ;

      function __getServerGrid(rowList, persistedData) {
        var titleText = __formatTitleImageAndText('server', lang.replace(i18n.NUMBER_SERVERS, [ rowList.data.length ]));
        var hideableColumns = [2, 3, 4, 5, 6, 7, 8];
        var nonSortableColumns = [6, 7, 8];
        return(__getGrid(rowList, ID.getServerGrid(), "servers", __getServerData, __getServerStructure, titleText, hideableColumns, nonSortableColumns, persistedData));
      }
      ;

      function __getApplicationStructure() {

        var structure = [
            {
                id : ID.getAlert(),
                field : 'alert',
                name : i18n.ALERT,
                width : '5%',
                widgetsInCell : true,
                decorator : function() {
                  return "<div style='text-align: center;' data-dojo-attach-point='alertIcon'></div>";
                },
                setCellValue : function (listViewAlertIcon) {
                  this.alertIcon.innerHTML = listViewAlertIcon.getHTML();
                },
                comparator : function(obj1, obj2) {
                  return __alertComparator(obj1, obj2);
                }
            },
            {
                id : ID.getName(),
                field : 'name',
                name : i18n.GRID_HEADER_NAME,
                width : '20%',
                widgetsInCell : true,
                decorator : function() {
                  return "<div class='listViewTitleText'><span data-dojo-attach-point='name'></span></div>";
                },
                setCellValue : function(app) {
                  var appName = app.name;
                  var appLink = "";
                  if (app.type === "appOnServer") {
                     appLink = "servers/" + app.server.id; + "/apps/" + appName;
                  } else {
                     appLink = "clusters/" + app.cluster.id + "/apps/" + appName;
                  }
                  this.name.innerHTML = '<span dir="' + utils.getStringTextDirection(appName) + '"><a href="#explore/' + appLink + '" title="' + appName + '">' + appName + '</a></span>';
                  if (this._onKeydownHandler) {
                    // Remove previously connected events to avoid memory leak.
                    this._onKeydownHandler.remove();
                  }
                  this._onKeydownHandler = on(this.name, "keydown", function(evt) {
                    if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
                      evt.srcElement.click();
                    }
                  });
                },
                comparator : function(obj1, obj2) {
                  return __nameComparator(obj1, obj2);
                }               
            },
            {
                id : 'location',
                field : 'location',
                name : i18n.GRID_LOCATION_NAME,
                display : true,
                width : '14%',
                widgetsInCell : true,
                decorator : function(s) {
                  return "<div class='listViewTitleText'><span data-dojo-attach-point='location'></span></div>";
                },
                setCellValue : function(app) {
                  var locationCellValue = new ListViewTextWithImage({
                    resource: app
                  });
                  this.location.innerHTML = locationCellValue.domNode.innerHTML;
                  if (this._onKeydownHandler) {
                    // Remove previously connected events to avoid memory leak.
                    this._onKeydownHandler.remove();
                  }
                  this._onKeydownHandler = on(this.location, "keydown", function(evt) {
                    if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
                      evt.srcElement.click();
                    }
                  });
                },
                comparator : function(obj1, obj2) {
                  return __locationComparator(obj1, obj2);
                }
            },
            {
                id : ID.getState(),
                field : 'state',
                name : i18n.SEARCH_RESOURCE_STATE,
                display : true,
                width : '14%',
                widgetsInCell : true,
                decorator : function(s) {
                  return "<div class='listViewState'><span data-dojo-attach-point='state'></span></div>";
                },
                setCellValue : function(stateIcon) {
                  this.state.innerHTML = stateIcon.getHTML();
                },
                comparator : function(obj1, obj2) {
                  return __stateComparator(obj1, obj2);
                }
            },
            {
                id : ID.getInstances(),
                field : 'instances',
                name : i18n.INSTANCES,
                display : true,
                width : '24%',
                widgetsInCell : true,
                decorator : function() {
                  return "<span class='listViewInstances' data-dojo-attach-point='chartLabel'></span><div class='listViewGraphicStackBar' data-dojo-attach-point='chartGraphic'></div>";
                },
                setCellValue: function(gridDataApp, storeDataApp, cellWidget, isInit){
                    if (storeDataApp.type === "appOnServer") {
                      //domStyle.set(cellWidget.domNode, "display", "none");
                      domClass.replace(cellWidget.domNode, "displayListViewInstancesNone", "displayListViewInstances");
                      return;
                    }
                    if (cellWidget.chartLabel &&  cellWidget.chartGraphic) {
                      //domStyle.set(cellWidget.domNode, "display", "block");
                      domClass.replace(cellWidget.domNode, "displayListViewInstances", "displayListViewInstancesNone");
                      cellWidget.chartLabel.id = ID.dashDelimit(storeDataApp.id, ID.getChart(), ID.getLabel());
                      cellWidget.chartGraphic.id = ID.dashDelimit(storeDataApp.id, ID.getChart(), ID.getGraphic());
                      var chart = new ListViewChart({
                          resource : storeDataApp,
                          domElementLabel : cellWidget.chartLabel,
                          domElementGraphic :  cellWidget.chartGraphic,
                          height: 6,
                          animation: true
                      });
                      chart.updateLabel();
                      chart.updateChart();
                    }
                },
                comparator: function(obj1, obj2) {
                  return __appInstanceComparator(obj1, obj2);
                }
            },
            {
              id : ID.getTags(),
              field : 'tags',
              name : i18n.TAGS,
              display : true,
              width : '24%',
              widgetsInCell : true,
              decorator : function(s) {
                return "<div class='listViewTags' data-dojo-attach-point='tagPane'></div>";
              },
              setCellValue : function(app) {
                var cp = new TagPane([app, 'tag']);
                domConstruct.empty(this.tagPane);
                cp.placeAt(this.tagPane);
              }
            },
            {
                id : ID.getActions(),
                field : 'actions',
                name : i18n.ACTIONS,
                display : true,
                width : '8%',
                widgetsInCell : true,
                decorator : function(s) {
                  return "<div class='listViewAction'><span data-dojo-attach-point='action'></span></div>";
                },
                setCellValue : function(data, resource, cellWidget) {
                  __setActionCell(resource, this.action, cellWidget);
                }
            }
            ];

        return structure;
      }
      ;

      function __getApplicationData(list) {
        var store = new Memory();

        if (list) {
          list.query().forEach(function(app) {
            var alertIcon = new ListViewAlertIcon({
              resource: app
            });
            
            var stateIcon = new StateIcon({
                parentId : ID.dashDelimit(ID.getSearchView(), app.id),
                resource : app,
                size : '20',
                cardState : true,
                showLabel : true,
                labelClass : 'listViewStateLabel'
            });

            var rowData = {
                id : app.id,
                alert : alertIcon,
                name : app,
                location : app,
                state : stateIcon,
                instances : app,
                tags: app,
                actions : app
            };
            store.put(rowData);
          });
        }
        return store;
      }
      ;

      function __getApplicationGrid(rowList, persistedData) {
        var titleText = __formatTitleImageAndText('app', lang.replace(i18n.NUMBER_APPS, [ rowList.data.length ]));
        var hideableColumns = [2, 3, 4, 5, 6];
        var nonSortableColumns = [5, 6];
        return(__getGrid(rowList, ID.getApplicationGrid(), "applications", __getApplicationData, __getApplicationStructure, titleText, hideableColumns, nonSortableColumns, persistedData));
      };

      function __getGrid(rowList, gridId, resourceType, store, structure, titleText, hideableColumns, nonSortableColumns, persistedData) {
        
        var persistedStructure;
        if(persistedData &&  persistedData[gridId]){          
          var data = __setColumnOrderFromPersistence(gridId, structure(), hideableColumns, persistedData);
          persistedStructure = data.structure;
          hideableColumns = data.hideableColumns;          
        }

        var grid = new Grid({
            id : gridId,
            resourceType : resourceType,
            titleText : titleText,
            hideableColumns : hideableColumns,
            cacheClass : Cache,
            autoHeight : true,
            columnWidthAutoResize: true,
            store : store(rowList),
            structure : (persistedStructure) ? persistedStructure : structure(), // Use persisted structure if saved or default structure
            //selectRowTriggerOnCell : true,
            modules : [ selectRow, selectColumn, moveColumn, dndColumn, CellWidget, Sort, ColumnResizer, Pagination, PaginationBar, HeaderMenu, HiddenColumns, Body ],
            modelExtensions : [FormatSort]
        });
        
        // set sortable columns
        if (nonSortableColumns) {
          for (var i = 0; i < nonSortableColumns.length; i++) {
            grid.column(nonSortableColumns[i]).setSortable(false);
          }
        }
        // hide default columns
        __showAndHideColumns(grid, hideableColumns, persistedData);
        
        __setGridFunctions(grid);
        
        grid.startup();
//        grid.column(0).setWidth(58);
        return grid;
      };

      function __getClusterStructure() {

        var structure = [
            {
              id : ID.getAlert(),
              field : 'alert',
              name : i18n.ALERT,
              width : '5%',
              widgetsInCell : true,
              decorator : function() {
                return "<div style='text-align: center;' data-dojo-attach-point='alertIcon'></div>";
              },
              setCellValue : function (listViewAlertIcon) {
                this.alertIcon.innerHTML = listViewAlertIcon.getHTML();
              },
              comparator : function(obj1, obj2) {
                return __alertComparator(obj1, obj2);
              }
            },
            {
                id : ID.getName(),
                field : 'name',
                name : i18n.GRID_HEADER_NAME,
                width : '15%',
                widgetsInCell : true,
                decorator : function() {
                  return "<div class='listViewTitleText'><span data-dojo-attach-point='name'></span></div>";
                },
                setCellValue : function(clusterName) {
                  this.name.innerHTML = '<span dir="' + utils.getStringTextDirection(clusterName) + '"><a href="#explore/clusters/' + clusterName + '" title="' + clusterName + '">' + clusterName + '</a></span>';
                  if (this._onKeydownHandler) {
                    // Remove previously connected events to avoid memory leak.
                    this._onKeydownHandler.remove();
                  }
                  this._onKeydownHandler = on(this.name, "keydown", function(evt) {
                    if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
                      evt.srcElement.click();
                    }
                  });
                }
            },
            {
                id : ID.getState(),
                field : 'state',
                name : i18n.SEARCH_RESOURCE_STATE,
                display : true,
                width : '15%',
                widgetsInCell : true,
                decorator : function(s) {
                  return "<div class='listViewState'><span data-dojo-attach-point='state'></span></div>";
                },
                setCellValue : function(stateIcon) {
                  this.state.innerHTML = stateIcon.getHTML();
                },
                comparator : function(obj1, obj2) {
                  return __stateComparator(obj1, obj2);
                }
            },
            {
                id : ID.getServers(),
                field : 'servers',
                name : i18n.SERVERS,
                display : true,
                width : '15%',
                widgetsInCell : true,
                decorator : function() {
                  return "<span data-dojo-attach-point='chartLabel'></span><div class='listViewGraphicStackBar' data-dojo-attach-point='chartGraphic'></div>";
                },
                setCellValue: function(gridDataClusterServers, storeDataHostClusterServers, cellWidget, isInit){
                  storeDataHostClusterServers.getServers().then(function(servers) {
                    if (cellWidget.chartLabel &&  cellWidget.chartGraphic) {
                      cellWidget.chartLabel.id = ID.dashDelimit(storeDataHostClusterServers.id, ID.getChart(), ID.getLabel());
                      cellWidget.chartGraphic.id = ID.dashDelimit(storeDataHostClusterServers.id, ID.getChart(), ID.getGraphic());
                      var chart = new ListViewChart({
                          resource : servers,
                          domElementLabel : cellWidget.chartLabel,
                          domElementGraphic :  cellWidget.chartGraphic,
                          height: 6,
                          animation: true
                      });
                      chart.updateLabel();
                      chart.updateChart();
                    }
                  });
                },
                comparator : function(obj1, obj2) {
                  return __serverInstanceComparator(obj1, obj2);
                }
            }, {
                id : ID.getApps(),
                field : 'apps',
                name : i18n.APPLICATIONS,
                display : true,
                width : '19%',
                widgetsInCell : true,
                decorator : function(s) {
                  return "<div class='listViewApps'><span data-dojo-attach-point='apps'></span></div>";
                },
                setCellValue : function(cluster) {
                  this.apps.innerHTML = "";
                  var me = this;

                  cluster.getApps().then(function(apps) {
                    var nameList = [];
                    apps.list.forEach(function(rtp) {
                      nameList.push(rtp.name);
                    });
                    var template = (nameList.length > 2 || nameList.length < 1) ? i18n.APPS_LIST : i18n.EMPTY_MESSAGE;
                    var inserts = (nameList.length > 2 || nameList.length < 1) ? [ nameList.length ] : nameList;
                    me.apps.innerHTML = lang.replace(template, [ inserts ]);
                  });
                },
                comparator: function(obj1, obj2) {
                  return __compareAppsDisplayString(obj1, obj2);
                }
            }, {
                id : ID.getTags(),
                field : 'tags',
                name : i18n.TAGS,
                display : true,
                width : '23%',
                widgetsInCell : true,
                decorator : function(s) {
                  return "<div class='listViewTags' data-dojo-attach-point='tagPane'></div>";
                },
                setCellValue : function(cluster) {
                  var cp = new TagPane([cluster, 'tag']);
                  domConstruct.empty(this.tagPane);
                  cp.placeAt(this.tagPane);
                }
            }, {
                id : ID.getActions(),
                field : 'actions',
                name : i18n.ACTIONS,
                display : true,
                width : '8%',
                widgetsInCell : true,
                decorator : function(s) {
                  return "<div class='listViewAction'><span data-dojo-attach-point='action'></span></div>";
                },
                setCellValue : function(data, resource, cellWidget) {
                  __setActionCell(resource, this.action, cellWidget);
                }
            }
            ];

        return structure;
      }
      ;

      function __getClusterData(list) {
        var store = new Memory();

        if (list) {
          list.query().forEach(function(cluster) {

            var alertIcon = new ListViewAlertIcon({
              resource: cluster
            });

            var stateIcon = new StateIcon({
                parentId : ID.dashDelimit(ID.getSearchView(), cluster.id),
                resource : cluster,
                size : '20',
                cardState : true,
                showLabel : true,
                labelClass : 'listViewStateLabel'
            });
            
            var rowData = {
                id : cluster.id,
                alert : alertIcon,
                name : cluster.name,
                state : stateIcon,
                servers : cluster,
                apps : cluster,
                tags : cluster,
                actions : cluster
            };
            store.put(rowData);
          });
        }
        return store;
      }
      ;

      function __getClusterGrid(rowList, persistedData) {
        var titleText = __formatTitleImageAndText('cluster', lang.replace(i18n.NUMBER_CLUSTERS, [ rowList.data.length ]));
        var hideableColumns = [2, 3, 4, 5, 6];
        var nonSortableColumns = [5, 6];
        return(__getGrid(rowList, ID.getClusterGrid(), "clusters", __getClusterData, __getClusterStructure, titleText, hideableColumns, nonSortableColumns, persistedData));
      };

      function __getHostStructure() {

        var structure = [
            {
              id : ID.getAlert(),
              field : 'alert',
              name : i18n.ALERT,
              width : '5%',
              widgetsInCell : true,
              decorator : function() {
                return "<div style='text-align: center;' data-dojo-attach-point='alertIcon'></div>";
              },
              setCellValue : function (listViewAlertIcon) {
                this.alertIcon.innerHTML = listViewAlertIcon.getHTML();
              },
              comparator : function(obj1, obj2) {
                return __alertComparator(obj1, obj2);
              }
            },
            {
                id : ID.getName(),
                field : 'name',
                name : i18n.GRID_HEADER_NAME,
                width : '20%',
                widgetsInCell : true,
                decorator : function() {
                  return "<div class='listViewTitleText'><span data-dojo-attach-point='name'></span></div>";
                },
                setCellValue : function(hostName) {
                  this.name.innerHTML = '<span dir="' + utils.getStringTextDirection(hostName) + '"><a href="#explore/hosts/' + hostName + '" title="' + hostName + '">' + hostName + '</a></span>';
                  if (this._onKeydownHandler) {
                    // Remove previously connected events to avoid memory leak.
                    this._onKeydownHandler.remove();
                  }
                  this._onKeydownHandler = on(this.name, "keydown", function(evt) {
                    if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
                      evt.srcElement.click();
                    }
                  });
                }
            },
            {
                id : ID.getServers(),
                field : 'servers',
                name : i18n.SERVERS,
                display : true,
                width : '15%',
                widgetsInCell : true,
                decorator : function() {
                  return "<span data-dojo-attach-point='chartLabel'></span><div class='listViewGraphicStackBar' data-dojo-attach-point='chartGraphic'></div>";
                },
                setCellValue: function(gridDataHostServers, storeDataHostServers, cellWidget, isInit){
                  storeDataHostServers.getServers().then(function(servers) {
                    if (cellWidget.chartLabel &&  cellWidget.chartGraphic) {
                      cellWidget.chartLabel.id = ID.dashDelimit(storeDataHostServers.id, ID.getChart(), ID.getLabel());
                      cellWidget.chartGraphic.id = ID.dashDelimit(storeDataHostServers.id, ID.getChart(), ID.getGraphic());
                      var chart = new ListViewChart({
                          resource : servers,
                          domElementLabel : cellWidget.chartLabel,
                          domElementGraphic :  cellWidget.chartGraphic,
                          height: 6,
                          animation: true
                      });
                      chart.updateLabel();
                      chart.updateChart();
                    }
                  });
                },
                comparator : function(obj1, obj2) {
                  return __serverInstanceComparator(obj1, obj2);
                }
            }, {
                id : ID.getRuntimes(),
                field : 'runtimes',
                name : i18n.RUNTIMES,
                display : true,
                width : '10%',
                widgetsInCell : true,
                decorator : function() {
                  return "<div><span data-dojo-attach-point='runtimes'></span></div>";
                },
                setCellValue: function(gridDataRuntimes, storeDataRuntimes, cellWidget){
                  storeDataRuntimes.then(function(runtimes) {
                    cellWidget.runtimes.innerHTML = runtimes.list.length;
                  });
                },
                comparator: function(obj1, obj2) {
                  return __runtimesComparator(obj1, obj2);
                }
            }, {
                id : ID.getTags(),
                field : 'tags',
                name : i18n.TAGS,
                display : true,
                width : '27%',
                widgetsInCell : true,
                decorator : function(s) {
                  return "<div class='listViewTags' data-dojo-attach-point='tagPane'></div>";
                },
                setCellValue : function(host) {
                  var cp = new TagPane([host, 'tag']);
                  domConstruct.empty(this.tagPane);
                  cp.placeAt(this.tagPane);
                }
            }, {
                id : ID.getActions(),
                field : 'actions',
                name : i18n.ACTIONS,
                display : true,
                width : '8%',
                widgetsInCell : true,
                decorator : function(s) {
                  return "<div class='listViewAction'><span data-dojo-attach-point='action'></span></div>";
                },
                setCellValue : function(data, resource, cellWidget) {
                  __setActionCell(resource, this.action, cellWidget);
                }
            } ];

        return structure;
      }
      ;

      function __getHostData(list) {
        var store = new Memory();

        if (list) {
          list.query().forEach(function(host) {

            var alertIcon = new ListViewAlertIcon({
              resource: host
            });

            var stateIcon = new StateIcon({
                parentId : ID.dashDelimit(ID.getSearchView(), host.id),
                resource : host,
                size : '20',
                cardState : true,
                showLabel : true,
                labelClass : 'listViewStateLabel'
            });

            var rowData = {
                id : host.id,
                alert : alertIcon,
                name : host.name,
                servers : host,
                runtimes : host.getRuntimes(),
                tags : host,
                actions : host
            };
            store.put(rowData);
          });
        }
        return store;
      }
      ;

      function __getHostGrid(rowList, persistedData) {
        var titleText = __formatTitleImageAndText('host', lang.replace(i18n.NUMBER_HOSTS, [ rowList.data.length ]));
        var hideableColumns = [2, 3, 4, 5];
        var nonSortableColumns = [4, 5];
        return(__getGrid(rowList, ID.getHostGrid(), "hosts", __getHostData, __getHostStructure, titleText, hideableColumns, nonSortableColumns, persistedData));
      };

      function __getRuntimeStructure() {

        var structure = [
            {
              id : ID.getAlert(),
              field : 'alert',
              name : i18n.ALERT,
              width : '5%',
              widgetsInCell : true,
              decorator : function() {
                return "<div style='text-align: center;' data-dojo-attach-point='alertIcon'></div>";
              },
              setCellValue : function (listViewAlertIcon) {
                this.alertIcon.innerHTML = listViewAlertIcon.getHTML();
              },
              comparator : function(obj1, obj2) {
                return __alertComparator(obj1, obj2);
              }
            },
            {
                id : ID.getName(),
                field : 'name',
                name : i18n.GRID_HEADER_NAME,
                width : '10%',
                widgetsInCell : true,
                decorator : function() {
                  return "<div class='listViewTitleText'><span data-dojo-attach-point='name'></span></div>";
                },
                setCellValue : function(runtime) {
                  var name = runtime.name;
                  var uri = runtime.id;
                  this.name.innerHTML = '<span dir="' + utils.getStringTextDirection(name) + '"><a href="#explore/runtimes/' + uri + '" title="' + name + '">' + name + '</a></span>';
                  if (this._onKeydownHandler) {
                    // Remove previously connected events to avoid memory leak.
                    this._onKeydownHandler.remove();
                  }
                  this._onKeydownHandler = on(this.name, "keydown", function(evt) {
                    if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
                      evt.srcElement.click();
                    }
                  });
                }
            },
            {
                id : ID.getServers(),
                field : 'servers',
                name : i18n.SERVERS,
                display : true,
                width : '15%',
                widgetsInCell : true,
                decorator : function() {
                  return "<span data-dojo-attach-point='chartLabel'></span><div class='listViewGraphicStackBar' data-dojo-attach-point='chartGraphic'></div>";
                },
                setCellValue: function(unused, runtime, cellWidget, isInit){
                  runtime.getServers().then(function(servers) {
                    if (cellWidget.chartLabel &&  cellWidget.chartGraphic) {
                      cellWidget.chartLabel.id = ID.dashDelimit(runtime.id, ID.getChart(), ID.getLabel());
                      cellWidget.chartGraphic.id = ID.dashDelimit(runtime.id, ID.getChart(), ID.getGraphic());
                      var chart = new ListViewChart({
                          resource : servers,
                          domElementLabel : cellWidget.chartLabel,
                          domElementGraphic :  cellWidget.chartGraphic,
                          height: 6,
                          animation: true
                      });
                      chart.updateLabel();
                      chart.updateChart();
                    }
                  });
                },
                comparator : function(obj1, obj2) {
                  return __serverInstanceComparator(obj1, obj2);
                }
            }, {
              id : 'host',
              field : 'host',
              name : i18n.HOST,
              display : true,
              width : '17%',
              widgetsInCell : true,
              decorator : function(s) {
                return "<div class='listViewTitleText' data-dojo-attach-point='host'></div>";
              },              
              setCellValue : function(hostName) {
                this.host.innerHTML = '<span dir="' + utils.getStringTextDirection(hostName) + '"><a href="#explore/hosts/' + hostName + '" title="' + hostName + '">' + hostName + '</a></span>';
                if (this._onKeydownHandler) {
                  // Remove previously connected events to avoid memory leak.
                  this._onKeydownHandler.remove();
                }
                this._onKeydownHandler = on(this.host, "keydown", function(evt) {
                  if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
                    evt.srcElement.click();
                  }
                });
              },
              comparator : function(obj1, obj2) {
                return obj1.host.localeCompare(obj2.host);
              }
            }, {
              id : 'path',
              field : 'path',
              name : i18n.PATH,
              display : true,
              width : '25%',
              widgetsInCell : true,
              decorator : function(s) {
                return "<div class='listViewTitleText' data-dojo-attach-point='path'></div>";
              },              
              setCellValue : function(path) {
                // need to handle path correctly for bidi
                if (utils.getBidiTextDirectionSetting() !== "ltr") {
                  this.path.innerHTML = BidiComplex.createDisplayString(path, "FILE_PATH");
                  this.path.title = BidiComplex.createDisplayString(path, "FILE_PATH");
                } else {
                  this.path.innerHTML = path;
                  this.path.title = path;
                }
              },
              comparator : function(obj1, obj2) {
                return obj1.host.localeCompare(obj2.host);
              }
            }, {
              id : ID.getTags(),
              field : 'tags',
              name : i18n.TAGS,
              display : true,
              width : '20%',
              widgetsInCell : true,
              decorator : function(s) {
                return "<div class='listViewTags' data-dojo-attach-point='tagPane'></div>";
              },
              setCellValue : function(runtime) {
                var cp = new TagPane([runtime, 'tag']);
                domConstruct.empty(this.tagPane);
                cp.placeAt(this.tagPane);
              }
            }, {
                id : ID.getActions(),
                field : 'actions',
                name : i18n.ACTIONS,
                display : true,
                width : '8%',
                widgetsInCell : true,
                decorator : function(s) {
                  return "<div class='listViewAction'><span data-dojo-attach-point='action'></span></div>";
                },
                setCellValue : function(resource) {
                  var button = ActionButtons.createStateButton(resource, ID.dashDelimit(ID.getSearchView(), ID.getRuntimes(), resource.id), 'listViewActionDropDown', 22);
                  domConstruct.empty(this.action);
                  button.placeAt(this.action);
                  button.startup();
                }
            } ];

        return structure;
      }
      ;

      function __getRuntimeData(list) {
        var store = new Memory();


        if (list) {
          list.query().forEach(function(runtime) {

            var alertIcon = new ListViewAlertIcon({
              resource: runtime
            });

            var stateIcon = new StateIcon({
                parentId : ID.dashDelimit(ID.getSearchView(), runtime.id),
                resource : runtime,
                size : '20',
                cardState : true,
                showLabel : true,
                labelClass : 'listViewStateLabel'
            });

            var rowData = {
                id : runtime.id,
                alert : alertIcon,
                name : runtime,
                path : runtime.path,
                host : runtime.host.name,
                servers : runtime,
                tags : runtime,
                actions : runtime
            };
            store.put(rowData);
          });
        }
        return store;
      }
      ;

      function __getRuntimeGrid(rowList, persistedData) {
        var titleText = __formatTitleImageAndText('runtime', lang.replace(i18n.NUMBER_RUNTIMES, [ rowList.data.length ]));
        var hideableColumns = [2, 3, 4, 5, 6];
        var nonSortableColumns = [5, 6];
        return(__getGrid(rowList, ID.getRuntimeGrid(), "runtimes", __getRuntimeData, __getRuntimeStructure, titleText, hideableColumns, nonSortableColumns, persistedData));
      };

      function __formatTitleImageAndText(icon, text) {
        return imgUtils.getSVGSmall(icon) + '<span style="margin-left: 6px;">' + text + '</span>';
        
      };
      
      function __getAppsDisplayString(server) {
        var appList = server.apps.list;
        var nameList = [];
        if (appList !== undefined) {
          appList.forEach(function (app) {
            nameList.push(app.name);
          });
        }
        var template = (nameList.length > 2 || nameList.length < 1) ? i18n.APPS_LIST : i18n.EMPTY_MESSAGE;
        var inserts = (nameList.length > 2 || nameList.length < 1) ? [ nameList.length ] : nameList;

        var appsDisplayString = lang.replace(template, [ inserts ]);
        return appsDisplayString;
      };
      
      function __compareAppsDisplayString(server1, server2) {
        var server1AppsDisplayString = __getAppsDisplayString(server1);
        var server2AppsDisplayString = __getAppsDisplayString(server2);
        return server1AppsDisplayString.localeCompare(server2AppsDisplayString);        
      };
      
      function __nameComparator(obj1, obj2) {    
        return obj1.name.localeCompare(obj2.name);
      };
    
      function __locationComparator(obj1, obj2) {
          var name1 = __getNameFromId(obj1);
          var name2 = __getNameFromId(obj2);
          return name1.localeCompare(name2);
      };
      
      function __stateComparator(obj1, obj2) {
          return obj1.state.localeCompare(obj2.state);
      };
      
      function __appInstanceComparator(obj1, obj2) {
        return __instanceComparator(obj1, obj2);
      };
      
      function __serverInstanceComparator(obj1, obj2) {
        var compareResult;
        obj1.getServers().then(function(obj1Servers) {
          obj2.getServers().then(function(obj2Servers) {
            compareResult =  __instanceComparator(obj1Servers, obj2Servers);
          });
        });      
        return compareResult;
      };      
      
      function __instanceComparator(obj1, obj2) {
        var chart1 = new ListViewChart({
          resource : obj1,
          domElementLabel : null,
          domElementGraphic :  null,
          height: 6,
          animation: true
        });
        var chart2 = new ListViewChart({
          resource : obj2,
          domElementLabel : null,
          domElementGraphic :  null,
          height: 6,
          animation: true
        });
        return chart1.getLabel().localeCompare(chart2.getLabel());
      };
      
      function __runtimesComparator(obj1, obj2) {
        var compareResult;
        obj1.then(function(obj1Runtimes) {
          obj2.then(function(obj2Runtimes) {
            compareResult = obj1Runtimes.list.length.toString().localeCompare(obj2Runtimes.list.length.toString());
          });
        });
        return compareResult;
      };
      
      function __getNameFromId(app) {
        var name = app.id;
        var startIndex = name.lastIndexOf("(");
        if (startIndex > 0) {
          name = name.substring(startIndex + 1, name.length - 1);
        }
        return name;
      };
      
      /**
       * Custom comparator for the Alert column.  The alerts are sorted according to 
       * the type of icon(s) they are displaying (alert, maintenance mode, etc) and then
       * alphabetically by resource ID within each alert-type grouping.
       * 
       * obj1 and obj are ListViewAlertIcon objects.
       * 
       */
      function __alertComparator(obj1, obj2) {      
        if (obj1.alertString === obj2.alertString) {          
          // sort alphabetically within type of icon(s) displayed
          return obj1.resource.id.localeCompare(obj2.resource.id);
        } else {
          // Reverse the locale Compare so that entries with alerts will migrate to the top and 
          // entries with no alerts will migrate to the bottom when sorting the column.
          return obj1.alertString < obj2.alertString ? 1 : obj1.alertString > obj2.alertString ? -1 : 0;
          
        }
      };
      
      function __serverNameComparator(obj1, obj2) {
        if (obj1.type === "server" && obj2.type === "server") {
          if (obj1.isCollectiveController === obj2.isCollectiveController) {          
            // sort alphabetically when both resource has icon displayed
            return obj1.name.localeCompare(obj2.name);
          } else {       
            // need to figure out the sort order
            var sortOrderElement = document.getElementById("serverGrid-name");
            var dir = sortOrderElement.getAttribute("aria-sort");
            // entries wih icon always show first 
            if (obj1.isCollectiveController) {
              return (dir === "ascending") ? -1 : 1;
            } else if (obj2.isCollectiveController) {
              //return (dir === 1) ? 1 : -1;
              return (dir === "ascending") ? 1 : -1;
            } else { // no collectiveController involved
              return obj1.name.localeCompare(obj2.name);
            }          
          }
        }
      }

      /*
       * Read the persisted data and determine which grid columns to show/hide
       */
      function __showAndHideColumns(grid, hideableColumns, persistedData) {
        // Check if this grid has persisted data
        var persistedGrid = persistedData && persistedData[grid.id] && 
                            persistedData[grid.id].columns && persistedData[grid.id];
        
        // Set initial hidden columns
        for (var i = 0; i < grid.structure.length; i++) {
            // Check if the user data persisted does not contain the column            
            if (hideableColumns.indexOf(i) !== -1){
              if (persistedGrid){
                    if(persistedData[grid.id].columns.indexOf(grid.structure[i].field) === -1){
                      grid.hiddenColumns.add(grid.structure[i].field);
                      grid.structure[i].display = false; // Necessary for this column to be unselected in the grid's action menu 
                    }
                    else{
                      grid.structure[i].display = true; // Necessary for this column to be selected in the grid's action menu 
                    }
              }
              // Hide the column if it should be hidden by default and there is no persistent data
              else if(!grid.structure[i].display){
                grid.hiddenColumns.add(grid.structure[i].field);
              }                
            }           
        }
      }
      
      
      function __setActionCell(resource, actionHolder, cellWidget) {
        var resourceTypeId = null;
        if (resource.type === "appOnServer" || resource.type === "appOnCluster") {
          resourceTypeId = ID.getApplications();        
        } else if (resource.type === "server") {
          resourceTypeId = ID.getServers();
        } else if (resource.type === "cluster") {
          resourceTypeId = ID.getClusters();
        } else if (resource.type === "host") {
          resourceTypeId = ID.getHosts();
        }
        // TODO: maybe not the way to get the button setting
        var multiSelectButton = registry.byId(ID.dashDelimit(ID.getSearchView(), resourceTypeId, "TitlePane",  ID.getMultiSelectListViewButton()));
        // if not in titlePane, try the buttonPane
        if (!multiSelectButton) {
          multiSelectButton = registry.byId(ID.dashDelimit(ID.getSearchView(), resourceTypeId, "SummaryPane",  ID.getMultiSelectListViewButton()));
        }

        // handle the case that multiSelectButton may not setup yet at the initial start
        var multiSelectButtonChecked = false;
        if (multiSelectButton) {
          multiSelectButtonChecked = multiSelectButton.get("checked");
        }
        var button;

        if (!multiSelectButtonChecked) {
          button = ActionButtons
          .createStateButton(resource, ID.dashDelimit(ID.getSearchView(), resourceTypeId, resource.id), 'listViewActionDropDown', 22);
        } else {
          var buttonId = ID.dashDelimit(ID.getSearchView(), resourceTypeId, resource.id, ID.getRowSelectButton());
          button = registry.byId(buttonId);
          
          if (button) {
            // reset the button
            var grid = cellWidget.cell.row.grid;
            var rowIndex = cellWidget.cell.row.index();
            //console.log(grid.select.row.isSelected(rowIndex));
            var isSelected = grid.select.row.isSelected(rowIndex);
            var isChecked = button.get("checked");
            if (isSelected && !isChecked) {
              button.set("checked", true);
            } else if (isChecked && !isSelected) {
              button.set("checked", false);
            }
          } else {
            button = new ToggleButton({
              id: buttonId,
              label: '<span style="display:none;">' + i18n.SELECT + '</span>',
              'class': 'listViewRowSelect',
              iconClass: 'listViewRowSelectUnchecked',
              showLabel: false,
              value: buttonId, // workaround for batchscan false positive label requirement on hidden input tag
              onClick : function() {
                  __setActionButtonIcon(this);
              },
              
              onBlur: function(evt) {
                  button.set("class", "listViewRowSelect"); // no outline by default
              }
            });
            // In order to get JAWS to read the information, we need to give it a role
            button.set("role", "button");
            // Also set the aria-label 
            button.set("aria-label", i18n.SELECT);
          }

          // Since cellWidget is reused, need to remove the handlers from previous row first. Otherwise, there will be memory leak.
          // Note: need to redo the handler everytime because of pagination that swaps out the rows.
          if (cellWidget._onClickHandler) {
            cellWidget._onClickHandler.remove();
          }
          if (cellWidget._onKeydownHandler) {
            cellWidget._onKeydownHandler.remove();
          }
          cellWidget._onClickHandler = on(button, 'click', function() {
            var grid = cellWidget.cell.row.grid;
            var rowIndex = cellWidget.cell.row.index();
            if (button.get("checked")) {
              grid.select.row.selectById(rowIndex);   
            } else { 
              grid.select.row.deselectById(rowIndex);
            }
          });        
          cellWidget._onKeydownHandler = on(button, 'keydown', function(evt) {
            if (evt.keyCode === keys.ENTER || evt.keyCode === keys.SPACE) {
              var grid = cellWidget.cell.row.grid;
              var rowIndex = cellWidget.cell.row.index();
              if (button.get("checked")) {
                grid.select.row.selectById(rowIndex);   
              } else {
                grid.select.row.deselectById(rowIndex);
              }
            }
          });   
          
          var grid = cellWidget.cell.row.grid;
          var rowIndex = cellWidget.cell.row.index();
          grid.on( 'cellKeyDown', function(evt) {
            if (evt.keyCode === keys.F2 && evt.columnId === ID.getActions() && evt.rowIndex == rowIndex) {
              button.set("class", "listViewRowSelect listViewRowSelectKeyDown"); // add outline for the focus
            }
          });   
          //__setOnKeyEvents(button, cellWidget.cell.row.grid);
        }
        
        domConstruct.empty(actionHolder);
        button.placeAt(actionHolder);
        button.startup();
      }
      
      function __setRowBackgroundColor(row, isSelected) {
        var color = "";
        if (isSelected) {
          color = "#ddf2f9";
        } 
        
        // Can't set the background color on the row level as it has been overriden by the cell
        // level. Has to go thru each cell of the row to set the background color.
        var cells = row.cells();
        for (var i = 0; i < cells.length; i++) {
          if(cells[i].node()){
            cells[i].node().style.backgroundColor = color;
          }
        }
      }
      
      /**
       * clear all selected row 
       */
      function __clearRowSelection(grid) {
        // Note: we're removing the selected row while iterating thru the array, hence not
        // incrementing i until getSelected returns 0.
        for (var i = 0; i < grid.select.row.getSelected().length;) {             
          var rowIndex = grid.select.row.getSelected()[i];
          grid.select.row.deselectById(rowIndex);
        }
      }
      
      /**
       * select/de-select all rows in the grid + check/uncheck the action buttons
       */
      function __setRowSelection(grid, isSelect) {
        for (var i = 0; i < grid.rowCount(); i++) {
          if (isSelect) {
            grid.select.row.selectById(i);
          } else {
            grid.select.row.deselectById(i);
          }
          var actionButton = registry.byId(ID.dashDelimit(ID.getSearchView(), grid.resourceType, grid.row(i).id, ID.getRowSelectButton()));
          if (actionButton) {
            actionButton.set('checked', isSelect);
            __setActionButtonIcon(actionButton);
          }
        }
      }
      
      /*
       * Set the action button icon and labels accordingly.
       */
      function __setActionButtonIcon(button) {
          if (button.get("checked")) {
              button.set("iconClass", "listViewRowSelectChecked");
              button.set("aria-label", i18n.DESELECT);
              button.set("label", '<span style="display:none;">' + i18n.DESELECT + '</span>');
          } else {
              button.set("iconClass", "listViewRowSelectUnchecked");
              button.set("aria-label", i18n.SELECT);
              button.set("label", '<span style="display:none;">' + i18n.SELECT + '</span>');
          }
      }
      
      function __getSelectedWithInMaintenanceModeCount(grid) {
        var inMM = 0;
        for (var i = 0; i < grid.select.row.getSelected().length; i++) {
          var resource = grid.store.data[grid.select.row.getSelected()[i]].actions;
          if (resource.maintenanceMode === constants.MAINTENANCE_MODE_IN_MAINTENANCE_MODE) {
            inMM++;
          }
        }
        return inMM;
      }
      
      function __getSelectedWithNotInMaintenanceModeCount(grid) {
        var notInMM = 0;
        for (var i = 0; i < grid.select.row.getSelected().length; i++) {
          var resource = grid.store.data[grid.select.row.getSelected()[i]].actions;
          if (resource.maintenanceMode === constants.MAINTENANCE_MODE_NOT_IN_MAINTENANCE_MODE) {
            notInMM++;
          }
        }
        return notInMM;
      }
      
      function __setGridFunctions(grid) {
        grid.clearRowSelection = function() {__clearRowSelection(grid);};
        grid.selectAllRows = function() {__setRowSelection(grid, true);};
        grid.deSelectAllRows = function() {__setRowSelection(grid, false);};
        grid.getSelectedWithInMaintenanceModeCount = function() {return __getSelectedWithInMaintenanceModeCount(grid);};
        grid.getSelectedWithNotInMaintenanceModeCount = function() {return __getSelectedWithNotInMaintenanceModeCount(grid);};
      }
      
      function __setColumnOrderFromPersistence(gridId, oldStructure, oldHideableColumns, persistedData){
        var newStructure = [];
        var hideableColumns = [];
        var columns = persistedData[gridId].columns;
        
        // Loop through the persisted column order
        for(var i = 0; i < columns.length; i++){
          var id = columns[i];                    
          var j = 0;
          
          // Look for the column in the old structure and append it to the new structure
          while(j < oldStructure.length){
            if(oldStructure[j].id === id){
              newStructure.push(oldStructure[j]);     
              break;
            }
            j++;            
          }
        }        
        
        // Add any columns to the structure that weren't in the persisted data
        for(var i = 0; i < oldStructure.length; i++){
          if(oldHideableColumns.indexOf(i) !== -1){
            // Find the index of the column in the new structure and add its index to hideable columns
            var index = newStructure.indexOf(oldStructure[i]);
            if(index === -1){
              newStructure.push(oldStructure[i]);              
              index = newStructure.length-1;
            }            
            hideableColumns.push(index);
          }
        }

        var returnObj = {};
        returnObj.structure = newStructure;
        returnObj.hideableColumns = hideableColumns;
        return returnObj;
      }
    });