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
/* jshint strict: false */
define([ 'dojo/_base/declare',
         'dojo/_base/lang',
         'dojo/_base/array',
         'dojo/date/locale',
         'dojo/store/Memory',
         'dojo/dom',
         'dojo/has',
         'dojo/on',
         'dojo/text!./templates/MessagesTableGraph.html', 
         'dojo/i18n!jsExplore/nls/explorerMessages',
         'dijit/focus',
         'dijit/layout/ContentPane',
         'dijit/layout/StackContainer', 
         'dijit/form/Button',
         'dijit/registry',
         'gridx/core/model/cache/Sync',
         'gridx/core/model/extensions/Sort',
         'gridx/Grid',
         'gridx/modules/CellWidget',
         'gridx/modules/VirtualVScroller',
         'js/widgets/TextBox',
         'jsExplore/utils/ID',
         'jsExplore/widgets/graphs/logAnalytics/LogAnalyticsGraph' ],
    function(declare, lang, array, dateLocale, Memory, dom, has, on, template, i18n, focusUtil, ContentPane, StackContainer,
        Button, registry, Cache, Sort, Grid, CellWidget, VirtualVScroller, TextBox, ID, LogAnalyticsGraph) {

      return declare("MessagesTableGraph", [ LogAnalyticsGraph ], {

          searchStackContainer: null,
          displayActionButton : false,
          store : null,
          grid : null,
          gridStructure : null,
          // A boolean that will prevent multiple requests being issued when the user scrolls continually past the 
          // point where records are requested. It will get reset when the current request has been completed.
          requestingRecords: false,
          // This value is the timestamp of the latest record received. We use this for the subsequent requests, so we only
          // get the new records.
          lastRecordTimeStamp: 0,
          // This variable determines how many pixels from the bottom of scroll we should trigger the 
          // getting of the next set of records.
          scrollPixelsFromBottomToTriggerUpdate: 300,
          // This variable determines how many records are pulled back in each request.
          maxRecords: 500,
          // The sort field
          sortField: null,
          // This variable is used to identify when all the records have been read. We reset this when the time selector changes.
          allRecordsRead: false,
          // This variable is used to destinguish between the table and graph.
          initTableFlag:false,
          //Unique identifier for each record
          recordId:0,
          //Timeout handle
          timeoutHandle : null,
          
          constructor : function(params) {
            this.inherited(arguments);
            this.templateString = template;
            this.initTableFlag=true;
            this.gridStructure = new Array();
          },

          postCreate : function() {
            this.inherited(arguments);
            // Create the stack container and immediately start it, otherwise any children added before you start don't appear to display correctly.
            this.searchStackContainer = new StackContainer({id: ID.underscoreDelimit(this.id, ID.getSearchStackContainer()), style: "messagesSearchStackContainer"}, this.searchNode);
            this.searchStackContainer.startup();

            /**
             * create the memory store for the grid
             */
            this.store = new Memory({
              idProperty: ID.getId()
            });
            
            /**
             * Code to create the Search Icon Button
             */
            // The content Pane for the search Icon button.
            var searchIconContentPane = new ContentPane({id: this.id + ID.getSearchIconContentPane()});
            
            // The search icon button.
            var searchButton = new Button({
                id: this.id + ID.getSearchButton(),
                iconClass : 'messagesSearchIcon',
                baseClass : "messagesSearchButton",
                showLabel : false
            });
            
            searchIconContentPane.addChild(searchButton);

            // The search icon button on click event. This switches the display to the text field and focuses on that text field.
            on(searchButton, "click", lang.hitch(this, function() {
              // If the initial button is clicked, then we need to display the search field.
              this.searchStackContainer.selectChild(this.id + ID.getSearchFieldContentPanel());
              focusUtil.focus(dom.byId(this.id + ID.getMessagesFilter()));
            }));
            
            /**
             * Code to create the Search Text Field
             */
            // This is the content pane that holds the text field and the search icon.
            var searchFieldContentPane = new ContentPane({
                id : this.id + ID.getSearchFieldContentPanel(),
                "class" : "messagesSearchContentPane"
            });
            
            var searchIconPositionClass = "messagesIconFloatLeft";
// TODO: the id has change so throws an exception. Also, is this mirroring or associated with text direction?
// Make this behave the same as search in the toolbox.
            if (has("adminCenter-bidi")) {
//              console.error("looking for " + this.id + "messagesFilter", this.id + "messagesFilter");
              if (dom.byId(this.id + ID.getMessagesFilter()) && dom.byId(this.id + ID.getMessagesFilter()).dir === "rtl") 
                searchIconPositionClass = "messagesIconFloatRight";
            }
            
            // The search icon in the search pill
            var searchFieldIconContentPane = new ContentPane({
              id : this.id + ID.getSearchFieldIconContentPanel(),
              "class" : "messagesSearchIcon messagesTextBoxIcon " + searchIconPositionClass
            });

            // Add in the search textbox.
            var messagesFilterTextBox = new TextBox({
                id : this.id + ID.getMessagesFilter(),
                "class": "messagesTextBox",
                // TODO
                "aria-labelledby" : "TODO"
            });
            
            searchFieldContentPane.addChild(searchFieldIconContentPane);
            searchFieldContentPane.addChild(messagesFilterTextBox);

            // If the user clicks in the search pill, then focus on the text box.
            on(searchFieldContentPane, "click", lang.hitch(this, function() {
              focusUtil.focus(dom.byId(this.id + ID.getMessagesFilter()));
            }));

            on(searchFieldContentPane, "keyDown", lang.hitch(this, function(event) {
              if (this.timeoutHandle) {
                clearTimeout(this.timeoutHandle);
              };
            }));
            
            
            // When the user leaves the text field area, we should check whether there is any search data. 
            // If there is, leave the search text field there, otherwise switch back to the search icon pane.
            on(searchFieldContentPane, "keyUp", lang.hitch(this, function(event) {
              this.timeoutHandle = setTimeout(lang.hitch(this, function() {
                var newSearchText = registry.byId(this.id + ID.getMessagesFilter()).get("value").trim();
                if (newSearchText === "") {
                  // If the initial button is clicked, then we need to display the search field.
                  this.searchStackContainer.selectChild(this.id + ID.getSearchIconContentPane());
                }
                // If the newSearchText is different to the previous searchText then we need to regen the graph. So if the user
                // blanks out the text we'll need to redisplay all records.
                if (newSearchText !== this.searchText) {
                  // Update the store searchText with the new value.
                  this.searchString = newSearchText;
                  this.initTable();
                  this.processPipe(this.id, this.chartNode, this.startTime, this.endTime, true);
                }
              }), 500);
            }));

            // This method watches the timeSelector range for changes and resets the allRecordsRead variable, and positions the 
            // startRecord to the 0;
            if (this.timeSelector) {
              this.timeSelector.watch("selectedRange", lang.hitch(this, function(name, oldValue, value) {
                this.allRecordsRead = false;
                this.startRecord = 0;
              }));
            }

            // Add both the search icon and the serach text field into the stackContainer.
            this.searchStackContainer.addChild(searchIconContentPane);
            this.searchStackContainer.addChild(searchFieldContentPane);
            
            
            // TODO  There is already a get of the keys in the LogAnalyticsGraph.postCreate. We may need to create an abstract
            // method in the LogAnalyticsGraph class, that we can override. 
            this.processPipe(this.id, this.chartNode, this.startTime, this.endTime, true);
          },
          /**
           * The grid should be destroyed in case there is no data
           * TODO Destroy function not working, need to find other solution.
           */
          
          initTable: function(){
            if (registry.byId(this.id + ID.getMessageGrid())) {
              this.store = new Memory({
                idProperty: ID.getId()
              });
              this.lastRecordTimeStamp=0;
              this.grid.model.clearCache();
              this.grid.model.setStore(this.store);
              this.grid.body.refresh();
              registry.byId(this.id + ID.getMessageGrid()).set("style", "display:none");
            }
          },
          
          /**
           * @param pipeName
           * @param from
           * @param searchString
           * @param init
           */
          processResponse: function(response) {

            // If we have the init data, process the columns, formats and labels
            if(registry.byId(this.id + ID.getMessageGrid())){ // showing the grid
              registry.byId(this.id + ID.getMessageGrid()).set("style", "display:block");
            }
            if (response.cols) {
              this.__processMessageLabels(response.cols, response.formats, response.labels);
              this.__createGrid();
              if (this.configNoDataPane) {// No data pane should be hidden, if the data exists for table.
                this.configNoDataPane.set("style", "display:none");
              }
            }
            
            // If we find that we have got fewer records than the maxRecords, then set the flag to not read any more. 
            if (response.data.length < this.maxRecords) {
              this.allRecordsRead = true;
            } 
            //The maximum table size should be 200 rows. If we have received this max number of rows from runtime, 
            // we have to replace the entire row with this new data. 
            if(response.data.length > 199){
              this.store.setData(new Array());
              this.lastRecordTimeStamp = 0;  // set to 0 since we have no records now
              this.grid.model.clearCache();
              this.grid.model.sort([{colId: this.sortField, descending: true}]);
              this.grid.body.refresh();
            }
            var tempLastRecordTimeStamp=this.lastRecordTimeStamp;
            // Now process the records we have
            for (var i = 0; i < response.data.length; i++) {
              
              //TODO Need to come up with an id that can be used to see if we have already created this record. 
              // May need to use the sequence id that Analytics supply.
              var recordToAdd = response.data[i];
              if (new Date(recordToAdd.datetime).valueOf() > this.lastRecordTimeStamp){
                  this.recordId+=1;//Unique identification number for each row
                  recordToAdd.id=this.recordId;
                this.store.add(recordToAdd);
              
                if (new Date(recordToAdd.datetime).valueOf() > tempLastRecordTimeStamp) {
                  tempLastRecordTimeStamp = new Date(recordToAdd.datetime).valueOf();
                }

             }
            };
            this.lastRecordTimeStamp=tempLastRecordTimeStamp;
            //If we have received the max rows from runtime, we can avoid the further processing.
            var varStoreSize=0;
            if(response.data.length < 200){ 
            //Deletion of the rows which are not within the time boundary.
            var varStartTime=this.startTime;//creating a temp variable to pass the inside method
            tempStore=this.store;//creating a temp variable to pass the inside method
            itemsMarkedForDelete=this.store.query(function(object){
              varStoreSize++;// counting the number of records on the table
              return new Date(object.datetime).valueOf() < varStartTime;
             });//Marked all the deleted items
            if(itemsMarkedForDelete.length){
              array.forEach(itemsMarkedForDelete, function(item){
                if(item !== null){
                  tempStore.remove(item.id);//deleted the marked item from grid
                  varStoreSize--; // Decreasing the count whenever a record got deleted.
                }//endif
              });//end foreach

            }//end if

            this.store=tempStore;
            }
            this.grid.model.clearCache();
            this.grid.setStore(this.store);
            this.grid.model.sort([{colId: this.sortField, descending: true}]);
            this.grid.body.refresh();
            //If we have received the max rows from runtime, we can avoid the further processing.
            // Otherwise, we have to verify if the number of rows exceeds the max count.
            // If the data grid has more rows then max limit, we should remove the excess rows from the data table.
            if(response.data.length < 200){
                var varIndex=0;
                if(varStoreSize>200){
                  tempStore=this.store;//creating a temp variable to pass the inside method
                  itemsMarkedForDelete=this.store.query(function(object){
                    varIndex++;
                    return varIndex>200; // Marking the excess records for deletion.
                   });//Marked all the deleted items
                  if(itemsMarkedForDelete.length){
                    array.forEach(itemsMarkedForDelete, function(item){
                      
                      if(item !== null){
                        tempStore.remove(item.id);//deleted the marked item from grid
                        varStoreSize--; 
                      }//endif
                    });//end foreach
                  }//end if
                  this.store=tempStore;
                  this.grid.model.clearCache();
                  this.grid.setStore(this.store);
                  this.grid.model.sort([{colId: this.sortField, descending: true}]);
                  this.grid.body.refresh();
                }   
            }            
            this.requestingRecords = false;
          },
          
          /**
           * This method is called when the XHR call to the pipe fails. It allows this to be overridden if required.
           */
          processFailure: function(err) {
            console.log(err);
            this.requestingRecords = false;
          },
          
          __processMessageLabels: function(columns, formats, labels) {
            // cols:["col1","col2",...
            // labels:["label1","label2",..
            // formats:["icon","date_timestamp","text"]
            columns.forEach(lang.hitch(this, function(col,i) {
              var format = formats[i];
              var dFunc = function(value) {
                return value;
              };
              var msgWidthPct = "75%";
              var timeWidthPct="25%";
              var numWidthPct = "15%";
              if (formats.indexOf('icon') !== -1) {
                msgWidthPct = "70%";
              }else{
                if(col === 'uriPath'){
                  msgWidthPct = '45%';
                }
              }
              if (format === "icon") {
                dFunc = function(value) {
                  // figure out class based on value
                  if (value === "Error" || value === "Fatal") {
                    return "<div class='messagesSevError'></div>";
                  } else if (value === "Warn") {
                    return "<div class='messagesSevWarning'></div>";
                  } else if (value === "Info" || value === "Detail" || value === "Config") {
                    return "<div class='messagesSevInfo'></div>";
                  } else if (value === "Audit") {
                    return "<div class='messagesSevAudit'></div>";
                  } else if (value === "Fine") {
                    return "<div class='messagesSevInfo'></div>";
                  } else if (value === "Finer") {
                    return "<div class='messagesSevAudit'></div>";
                  } else if (value === "Entry") {
                    return "<div class='messagesSevAudit'></div>";
                  } else if (value === "Exit") {
                    return "<div class='messagesSevAudit'></div>";
                  } else if (value === "Finest") {
                    return "<div class='messagesSevInfo'></div>";
                  } else {
                    return "<div></div>";
                  }
                };
              } else if (format === "date_timestamp") {
//                console.error("format is date");
                dFunc = function(value) {
                  return dateLocale.format(new Date(value), { formatLength: "medium" });
                  
                };
              }
              if (format === "text") {
                this.gridStructure.push({
                  id: col, 
                  field: col, 
                  name: labels[i], 
                  width: msgWidthPct,
                  widgetsInCell : true,
                  decorator : function() {
                    return "<div data-dojo-attach-point='messagetext' style='word-wrap: break-word; white-space: normal; overflow-wrap: break-word;'></div>";
                  },
                  setCellValue : function(value) {
                    this.messagetext.innerHTML = value;
                  }
                });
              } else if (format === "icon") {
                this.gridStructure.push({id: col, field: col, name: labels[i], width: '5%',
                  decorator : dFunc
                });
              } else if (format === "date_timestamp") {
                this.gridStructure.push({id: col, field: col, name: labels[i], width: timeWidthPct,
                  decorator : dFunc
                });
                this.sortField = col;
              } else if (format === "number"){
                this.gridStructure.push({
                id: col, 
                field: col, 
                name: labels[i], 
                width: numWidthPct,
                widgetsInCell : true,
                decorator : function() {
                  return "<div data-dojo-attach-point='messagetext'></div>";
                },
                setCellValue : function(value) {
                  this.messagetext.innerHTML = value;
                }
              });
              }else {
                this.gridStructure.push({id: col, field: col, name: labels[i], width:timeWidthPct,
                  decorator : dFunc
                });
              }
            }));
          },
          
          /**
           * This method is used to create the grid when we have got records to load into it. 
           */
          __createGrid: function() {

            if (! this.grid) {
              this.grid = new Grid({
                id : this.id + ID.getMessageGrid(),
                columnWidthAutoResize: true,
                autoHeight: true,
                vScrollBuffSize: 1,
                cacheClass : Cache,
                store : this.store,
                structure : this.gridStructure,
                modules : [ CellWidget, VirtualVScroller ],
                modelExtensions: [ Sort ]
              });
              this.grid.placeAt(this.id + ID.getChartNode());
              
              // This adds an eventListener to the vertical scroll domNode. This means we'll get an event when the
              // user scrolls the table. We then can decide when we want to trigger the refreah.
              this.grid.vScroller.domNode.addEventListener("scroll", lang.hitch(this, function(e) {
               if(e && e.srcElement){// Sometimes scroll bars may not exists.
                var displayHeight = e.srcElement.clientHeight;
                var scrollHeight = e.srcElement.scrollHeight;
                var scrollTop = e.srcElement.scrollTop;
                
                // If we are the right number of pixels from the end of the table, we need to go and request more
                // records to add to the table. We check to see whether there are any more records to read, whether we are already 
                // updating the table, and finally whether we are in the range to request more records.
                if (!this.allRecordsRead && ! this.requestingRecords && ((scrollHeight - displayHeight) - scrollTop < this.scrollPixelsFromBottomToTriggerUpdate)) {
                  this.requestingRecords = true;
                  console.log("Requesting new records");
                  this.processPipe(this.id, this.chartNode, this.startTime, this.endTime, false);
                }
               }
              }));
            }
          }
    });
});
