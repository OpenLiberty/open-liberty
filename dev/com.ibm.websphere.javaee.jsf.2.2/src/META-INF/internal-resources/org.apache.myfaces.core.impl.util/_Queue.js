/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @class
 * @name _Queue
 * @memberOf myfaces._impl._util
 * @description Queue implementation used by our runtime system
 * improved version of
 * @see <a href="http://safalra.com/web-design/javascript/queues/Queue.js">http://safalra.com/web-design/javascript/queues/Queue.js</a>
 */
_MF_CLS(_PFX_UTIL+"_Queue", _MF_OBJECT,
  /**
   * @lends myfaces._impl._util._Queue.prototype
   */
{
    //faster queue by http://safalra.com/web-design/javascript/queues/Queue.js
    //license public domain
    //The trick is to simply reduce the number of slice and slice ops to a bare minimum.

    _q : null,
    _space : 0,
    _size: -1,

    /**
     * Standard constructor
     */
    constructor_: function() {
        this._callSuper("constructor_");
        this._q = [];
    },

    /**
     * @return the length of the queue as integer
     */
    length: function() {
        // return the number of elements in the queue
        return this._q.length - this._space;

    },

    /**
     * @return true if the current queue is empty false otherwise
     */
    isEmpty: function() {
        // return true if the queue is empty, and false otherwise
        return (this._q.length == 0);
    },

    /**
     * Sets the current queue to a new size, all overflow elements at the end are stripped
     * automatically
     *
     * @param {int} newSize as numeric value
     */
    setQueueSize: function(newSize) {
        this._size = newSize;
        this._readjust();
    },

    /**
     * adds a listener to the queue
     *
     * @param element the listener to be added
     */
    enqueue : function(/*function*/element) {
        this._q.push(element);
        //qeuesize is bigger than the limit we drop one element so that we are
        //back in line

        this._readjust();
    },

    _readjust: function() {
        var size = this._size;
        while (size && size > -1 && this.length() > size) {
            this.dequeue();
        }
    },

    /**
     * removes a listener form the queue
     *
     * @param element the listener to be removed
     */
    remove : function(/*function*/element) {
        /*find element in queue*/
        var index = this.indexOf(element);
        /*found*/
        if (index != -1) {
            this._q.splice(index, 1);
        }
    },

    /**
     * dequeues the last element in the queue
     * @return {Object} element which is dequeued
     */
    dequeue: function() {
        // initialise the element to return to be undefined
        var element = null;

        // check whether the queue is empty
        var qLen = this._q.length;
        var queue = this._q;
        
        if (qLen) {

            // fetch the oldest element in the queue
            element = queue[this._space];

            // update the amount of space and check whether a shift should occur
            //added here a max limit of 30
            //now bit shift left is a tad faster than multiplication on most vms and does the same
            //unless we run into a bit skipping which is impossible in our usecases here
            if ((++this._space) << 1 >= qLen) {

                // set the queue equal to the non-empty portion of the queue
                this._q = queue.slice(this._space);

                // reset the amount of space at the front of the queue
                this._space = 0;

            }

        }

        // return the removed element
        return element;
    },

    /**
     * simple foreach
     *
     * @param closure a closure which processes the element
     * @code
     *   queue.each(function(element) {
     *      //do something with the element
     *   });
     */
    each: function(closure) {
        this._Lang.arrForEach(this._q, closure, this._space);
    },

    /**
     * Simple filter
     *
     * @param closure a closure which returns true or false depending
     * whether the filter has triggered
     *
     * @return an array of filtered queue entries
     */
    arrFilter: function(closure) {
        return this._Lang.arrFilter(this._q, closure, this._space);
    },

    /**
     * @param element
     * @return the current index of the element in the queue or -1 if it is not found
     */
    indexOf: function(element) {
        return this._Lang.arrIndexOf(this._q, element);
    },

    /**
     * resets the queue to initial empty state
     */
    cleanup: function() {
        this._q = [];
        this._space = 0;
    }
});

