/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define("jsShared/grid/modules/HiddenColumns", [
  "dojo/_base/declare",
  "dojo/_base/array",
  "dojo/query",
  "gridx/modules/HiddenColumns"
], function(declare, array, query, gridxHiddenColumns){

/*=====
 * An extension of gridx/modules/HiddenColumns that will work correctly with the gridx/modules/Dod 
 * (Details on demand) module when the content of the details also contains a grid. 
===== */  
/*=====
    add: function(colId){
      // summary:
      //    Hide all the given columns in arguments.
      // colId: String|gridx.core.Column...
      //    Column IDs can be provided directly as arguments.
      //    gridx.core.Column object can also be provided.
      // example:
      //  | //Hide columnA
      //  | grid.hiddenColumns.add("columnA");
      //  | //Hide columnB, columnC and columnD
      //  | grid.hiddenColumns.add("columnB", "columnC", "columnD");
      //  | //Column object is also acceptable.
      //  | var col = grid.column("columnA");
      //  | grid.hiddenColumns.add(col);
    }
=====*/

  return declare(gridxHiddenColumns, {
    name: 'hiddenColumns',

    add: function(){
      var t = this,
          g = t.grid,
          columnsById = g._columnsById,
          columns = g._columns,
          columnLock = g.columnLock,
          lockCount = 0,
          hash = {},
          cols = array.filter(array.map(arguments, function(id){
                     id = id && typeof id === "object" ? id.id : id;
                     return columnsById[id];
                 }), function(col){
                     return col && !col.ignore && (col.hidable === undefined || col.hidable);
                 });
      //remove duplicated arguments.
      for(var i = 0, len = cols.length; i < len; ++i){
        hash[cols[i].id] = cols[i];
      }
      cols = [];
      for(var arg in hash){
        cols.push(hash[arg]);
      }
      if(columnLock){
        lockCount = columnLock.count;
        columnLock.unlock();
      }
      array.forEach(cols, function(col){
        if(col.index < lockCount){
          //If a locked column is hidden, should unlock it.
          --lockCount;
        }
        col.hidden = true;
        delete columnsById[col.id];
        columns.splice(array.indexOf(columns, col), 1);
        //Directly remove dom nodes instead of refreshing the whole body to make it faster.
        query('[colid="' + g._escapeId(col.id) + '"].gridxCell', g.domNode).forEach(function(node){
          node.parentNode.removeChild(node);
        });
      });
      if(cols.length){
        array.forEach(columns, function(col, i){
          col.index = i;
        });
      }
      g.columnWidth._adaptWidth();
      query('.gridxCell', g.bodyNode).forEach(function(node){
        // FIX: check that we are setting the width of a column in the grid we are associated with,
        //      and not one that may be part of the details on demand (gridx/modules/Dod) content
        //      that appears within our grid.
        if (columnsById[node.getAttribute('colid')]) {          
          var s = node.style,
          w = s.width = s.minWidth = s.maxWidth = columnsById[node.getAttribute('colid')].width;
       }
      });
      //FIXME: this seems ugly....
      if(g.vScroller._doVirtualScroll){
        g.body.onForcedScroll();
      }
      return t._refresh(0).then(function(){
        t.onHide(array.map(cols, function(col){
          return col.id;
        }));
        if(columnLock && lockCount > 0){
          columnLock.lock(lockCount);
        }
      });
    }
  });
});
