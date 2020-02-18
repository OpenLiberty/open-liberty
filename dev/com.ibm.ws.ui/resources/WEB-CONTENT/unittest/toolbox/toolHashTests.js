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
define(['intern!tdd', 'intern/chai!assert', 'dojo/hash', 'js/toolbox/toolHash'],
    function(tdd, assert, hash, toolHash) {

    with(assert) {
      
      /**
       * Defines the 'toolbox' module test suite.
       */
      tdd.suite('Tool Hash Tests', function() { 

           tdd.test('ToolHash.isSet() - no hash set', function() {
             hash('');
             assert.isFalse(toolHash.isSet(), 'When there is no hash, isSet should be false');
           });
    
           tdd.test('ToolHash.isSet() - with hash', function() {
             hash('something');
             assert.equal(toolHash.isSet(), true, 'When there is a hash set, isSet should be true');
           });
    
           tdd.test('ToolHash.get() - no hash set', function() {
             hash('');
             assert.equal(toolHash.get(), '', 'When there is no hash set, get should return empty string');
           });
    
           tdd.test('ToolHash.get() - non-slashed hash', function() {
             hash('noslashes');
             assert.equal(toolHash.get(), 'noslashes', 'When there is no slashes in the hash, get should return that value');
           });
    
           tdd.test('ToolHash.get() - slashed hash', function() {
             hash('slashed/value/s');
             assert.equal(toolHash.get(), 'slashed', 'When there is a hash with slashes, get should return the first segment');
           });
    
           tdd.test('ToolHash.set()', function() {
             var mockTool = { hashId: 'mockId' };
             toolHash.set(mockTool);
             assert.equal(hash(), 'mockId', 'The initial hash should be set to the tool ID');
           });
    
           tdd.test('ToolHash.wasSet() - changes outside of set()', function() {
             hash('');
             assert.isFalse(toolHash.wasSet(), 'wasSet should be false when the hash changes outside of set()');
             
             hash('abc-1.0');
             assert.isFalse(toolHash.wasSet(), 'wasSet should be false when the hash changes outside of set()');
           });
           
           tdd.test('ToolHash.wasSet() - true when changed using set()', function() {
             var mockTool = { hashId: 'mockId' };
             toolHash.set(mockTool);
             assert.isTrue(toolHash.wasSet(), 'wasSet should be true when the hash changes using set()');
           });
           
           tdd.test('ToolHash.wasSet() - false when changed after set()', function() {
             var mockTool = { hashId: 'mockId' };
             toolHash.set(mockTool);
             hash('differentID');
             assert.isFalse(toolHash.wasSet(), 'wasSet should be false when the hash changes after set()');
           });
           
           tdd.test('ToolHash.wasSet() - true after multiple set()', function() {
             var mockTool = { hashId: 'mockId' };
             toolHash.set(mockTool);
             
             mockTool.hashId = 'mockId2';
             toolHash.set(mockTool);
    
             assert.isTrue(toolHash.wasSet(), 'wasSet should be true when the hash changes using set()');
           });
    
           /**
            * This is a super unlikely case, but its tested for documentation.
            * Note that this is a corner case that is super unlikely, and we should never
            * be in a situation like this. If the user presses enter on a browser where they
            * do not change the hash value, that causes a refresh. If the value is changed,
            * then wasSet will be false. There is also the case where the value could be changed
            * to be something other than the tool ID
            */
           tdd.test('ToolHash.wasSet() - true when changed outside of set() to the same value', function() {
             var mockTool = { hashId: 'mockId' };
             toolHash.set(mockTool);
             hash('mockId');
             assert.isTrue(toolHash.wasSet(), 'wasSet should be true when the hash manually changes to the same value as set() - NOTE: this is a corner case that is super unlikely');
           });
           
           /**
            * This is a super unlikely case, but its tested for documentation.
            * Note that this is a corner case that is super unlikely, and we should never
            * be in a situation like this. If the user presses enter on a browser where they
            * do not change the hash value, that causes a refresh. If the value is changed,
            * then wasSet will be false. There is also the case where the value could be changed
            * to be something other than the tool ID
            */
           tdd.test('ToolHash.wasSet() - false when changed back to previous set() value', function() {
             var mockTool = { hashId: 'mockId' };
             toolHash.set(mockTool);
             assert.isTrue(toolHash.wasSet(), 'wasSet should be true when the hash changes using set()');
             
             hash('differentId');
             assert.isFalse(toolHash.wasSet(), 'wasSet should be false when the hash manually changes to another value');
             
             hash('mockId');
             assert.isFalse(toolHash.wasSet(), 'wasSet should be false when the hash manually changes to previous set() value');
           });
    
           tdd.test('ToolHash.clear()', function() {
             hash('before');
             toolHash.clear();
             assert.equal(hash(), '', 'When cleared, there should no longer be a hash value');
           });
    
           tdd.test('ToolHash.erase()', function() {
             hash('before');
             toolHash.erase();
             assert.equal(hash(), '', 'When erased, there should no longer be a hash value');
             // This check does NOT work on IE, but the logic seems to
             //assert.equal(-1, document.URL.indexOf('#'), 'When erased, there should be no # anywhere in the URL');
           });
    
           tdd.test('ToolHash.hasChanged() - empty to tool1', function() {
             assert.isTrue(toolHash.hasChanged('', 'tool1'));
           });
           
           tdd.test('ToolHash.hasChanged() - tool1 to empty', function() {
             assert.isTrue(toolHash.hasChanged('tool1', ''));
           });
           
           tdd.test('ToolHash.hasChanged() - tool1 to tool2', function() {
             assert.isTrue(toolHash.hasChanged('tool1', 'tool2'));
           });
           
           tdd.test('ToolHash.hasChanged() - tool2 to tool1', function() {
             assert.isTrue(toolHash.hasChanged('tool2', 'tool1'));
           });
           
           tdd.test('ToolHash.hasChanged() - tool1/a to tool2/a', function() {
             assert.isTrue(toolHash.hasChanged('tool1/a', 'tool2/a'));
           });
           
           tdd.test('ToolHash.hasChanged() - tool2/a to tool1/a', function() {
             assert.isTrue(toolHash.hasChanged('tool2/a', 'tool1/a'));
           });
           
           tdd.test('ToolHash.hasChanged() - empty to tool1/a', function() {
             assert.isTrue(toolHash.hasChanged('', 'tool1/a'));
           });
           
           tdd.test('ToolHash.hasChanged() - tool1/a to empty', function() {
             assert.isTrue(toolHash.hasChanged('tool1/a', ''));
           });
           
           tdd.test('ToolHash.hasChanged() - tool1/a to tool1/b', function() {
             assert.isFalse(toolHash.hasChanged('tool1/a', 'tool1/b'));
           });
           
           tdd.test('ToolHash.hasChanged() - tool1/b to tool1/a', function() {
             assert.isFalse(toolHash.hasChanged('tool1/b', 'tool1/a'));
           });
           
           /** These case should never happen, but check just in case! */
           tdd.test('ToolHash.hasChanged() - sanity checks', function() {
             assert.isFalse(toolHash.hasChanged('', ''));
             assert.isFalse(toolHash.hasChanged('tool1', 'tool1'));
             assert.isFalse(toolHash.hasChanged('tool1/a', 'tool1/a'));
           });
           
           /** A few sanity checks to quickly check the getName function */
           tdd.test('ToolHash.getName() - sanity checks', function() {
             assert.equal(toolHash.getName(), '');
             assert.equal(toolHash.getName(''), '');
             assert.equal(toolHash.getName('name-version'), 'name');
             assert.equal(toolHash.getName('nameOnly'), 'nameOnly');
           });
    
      });
    }
});
