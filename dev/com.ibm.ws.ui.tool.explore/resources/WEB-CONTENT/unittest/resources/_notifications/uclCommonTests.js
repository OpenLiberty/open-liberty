/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * General REST philosophy
 * 1) App Centric
 * 2) Minimal load, reference by name
 * 3) Consistency is good, but necessity is better
 * 4) Population by ResourceManager
 * 5) Replication layer is good for searches
 * 
 * General event philosophy:
 * 1) Only send an event when something is different (so processing is minimized)
 * 2) Only send an event with meaningful payload (so processing is minimized)
 * 3) Omit things which have not changed (so network payload is as small as possible)
 * 4) When a resource is removed, the event should only send a 'removed' state change. No other values should be specified.
 * 
 * A Collection Event is information about a collection and has the following properties:
 * - type: the event type, such as 'hosts' or 'servers'
 * - tallies: this is always 'up', 'down', 'unknown', and somtimes 'partial' and 'empty'
 * - added: Array of the name added resources
 * - removed: Array of the name removed resources
 * 
 * The Collection Event can be a top level event, or be a sub-collection for a resource:
 * 1. The 'top level' collections. These are Applications, Servers, Clusters, Hosts,and Runtimes.
 * 2. Sub-collection elements, such as the servers on a Host or the servers that make up a Cluster.
 * 
 * When resources are added to a collection, only the name is stored. The actual instance of the Object is not created until it is needed.
 * 
 * A Resource Event has the properties for the given resource, and generally includes at least one sub-collection.
 * 
 * The validateTypeEvent methods test for the supported event fields for all expected types.
 * 
 */
define([ 
        "intern!tdd",
        "intern/chai!assert",
        "resources/_notifications/uclCommon"
        ],

        function(tdd, assert, uclCommon) {

  /**
   * Defines the 'Unified Change Listener - Common Module Tests' module test suite.
   */
  tdd.suite("Unified Change Listener - Common Module Tests", function() {

    /**
     * No metadata, so no changes
     */
    tdd.test('Detailed metadata compare - no metadata in prev or now', function() {
      var prev = {};
      var now = {};
      var changes = {};

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isFalse(hasChanges, 'When there is no metadata in the objects, there should be no changes detected');
    });

    /**
     * Tags only, compare - no change
     */
    tdd.test('Detailed metadata compare test - tags only, no change', function() {
      var prev = { tags: ['tag1'] };
      var now =  { tags: ['tag1'] };
      var changes = {};

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isFalse(hasChanges, 'When there is no changes to the tags, there should be no changes detected');
    });

    /**
     * Tags only, compare - new tag
     */
    tdd.test('Detailed metadata compare test - tags only, new tag', function() {
      var prev = { tags: ['tag1'] };
      var now =  { tags: ['tag1','tag2'] };
      var changes = { tags: ['tag1','tag2'] };

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.tags, changes.tags, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Tags only, compare - removed tag
     */
    tdd.test('Detailed metadata compare test - tags only, removed tag', function() {
      var prev = { tags: ['tag1','tag2'] };
      var now =  { tags: ['tag1'] };
      var changes = { tags: ['tag1'] };

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.tags, changes.tags, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Tags only, compare - different tags
     */
    tdd.test('Detailed metadata compare test - tags only, different tags', function() {
      var prev = { tags: ['tag1'] };
      var now =  { tags: ['tag2'] };
      var changes = { tags: ['tag2'] };

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.tags, changes.tags, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Tags only, compare - added tags
     */
    tdd.test('Detailed metadata compare test - tags only, tags added', function() {
      var prev = {};
      var now =  { tags: ['tag1'] };
      var changes = { tags: ['tag1'] };

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.tags, changes.tags, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Tags only, compare - removed tags
     */
    tdd.test('Detailed metadata compare test - tags only, tags removed', function() {
      var prev = { tags: ['tag1'] };
      var now =  {};
      var changes = {};

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.tags, changes.tags, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Note only, compare - no change
     */
    tdd.test('Detailed metadata compare test - note only, no change', function() {
      var prev = { note: 'note' };
      var now =  { note: 'note' };
      var changes = {};

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isFalse(hasChanges, 'When there is no changes to the note, there should be no changes detected');
    });

    /**
     * Note only, compare - changed note
     */
    tdd.test('Detailed metadata compare test - note only, changed note', function() {
      var prev = { note: 'note1' };
      var now =  { note: 'note2' };
      var changes = { note: 'note2' };

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.note, changes.note, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Note only, compare - added note
     */
    tdd.test('Detailed metadata compare test - note only, note added', function() {
      var prev = {};
      var now =  { note: 'note' };
      var changes = { note: 'note' };

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.note, changes.note, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Note only, compare - removed note
     */
    tdd.test('Detailed metadata compare test - note only, note removed', function() {
      var prev = { note: 'note' };
      var now =  {};
      var changes = {};

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.note, changes.note, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Owner only, compare - no change
     */
    tdd.test('Detailed metadata compare test - owner only, no change', function() {
      var prev = { owner: 'owner' };
      var now =  { owner: 'owner' };
      var changes = {};

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isFalse(hasChanges, 'When there is no changes to the owner, there should be no changes detected');
    });

    /**
     * Owner only, compare - changed owner
     */
    tdd.test('Detailed metadata compare test - owner only, changed owner', function() {
      var prev = { owner: 'owner1' };
      var now =  { owner: 'owner2' };
      var changes = { owner: 'owner2' };

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.owner, changes.owner, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Owner only, compare - added note
     */
    tdd.test('Detailed metadata compare test - owner only, owner added', function() {
      var prev = {};
      var now =  { owner: 'owner' };
      var changes = { owner: 'owner' };

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.owner, changes.owner, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Owner only, compare - removed owner
     */
    tdd.test('Detailed metadata compare test - owner only, owner removed', function() {
      var prev = { owner: 'owner' };
      var now =  {};
      var changes = {};

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.owner, changes.owner, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Contacts only, compare - no change
     */
    tdd.test('Detailed metadata compare test - contacts only, no change', function() {
      var prev = { contacts: ['contact1'] };
      var now =  { contacts: ['contact1'] };
      var changes = {};

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isFalse(hasChanges, 'When there is no changes to the contacts, there should be no changes detected');
    });

    /**
     * Contacts only, compare - new contact
     */
    tdd.test('Detailed metadata compare test - contacts only, new contact', function() {
      var prev = { contacts: ['contact1'] };
      var now =  { contacts: ['contact1','contact2'] };
      var changes = { contacts: ['contact1','contact2'] };

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.contacts, changes.contacts, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Contacts only, compare - removed contact
     */
    tdd.test('Detailed metadata compare test - contacts only, removed contact', function() {
      var prev = { contacts: ['contact1','contact2'] };
      var now =  { contacts: ['contact1'] };
      var changes = { contacts: ['contact1'] };

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.contacts, changes.contacts, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Contacts only, compare - different contacts
     */
    tdd.test('Detailed metadata compare test - contacts only, different contacts', function() {
      var prev = { contacts: ['contact1'] };
      var now =  { contacts: ['contact2'] };
      var changes = { contacts: ['contact2'] };

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.contacts, changes.contacts, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Contacts only, compare - added contacts
     */
    tdd.test('Detailed metadata compare test - contacts only, contacts added', function() {
      var prev = {};
      var now =  { contacts: ['contact1'] };
      var changes = { contacts: ['contact1'] };

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.contacts, changes.contacts, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Contacts only, compare - removed contacts
     */
    tdd.test('Detailed metadata compare test - contacts only, contacts removed', function() {
      var prev = { contacts: ['contact1'] };
      var now =  {};
      var changes = {};

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.contacts, changes.contacts, 'When there are changes, the new value should always be what is set into changes');
    });

    /**
     * Some metadata changes
     */
    tdd.test('Detailed metadata compare test - tags only, tags removed', function() {
      var prev = { tags: ['tag1'], note: 'note1', owner: 'owner1', contacts: [ 'contact1' ] };
      var now =  { tags: ['tag2'], note: 'note1', owner: 'owner2', contacts: [ 'contact1' ] };
      var changes = { tags: ['tag2'], owner: 'owner2' };

      var hasChanges = uclCommon.compareMetadata(prev, now, changes);

      assert.isTrue(hasChanges, 'When there are changes, the return value should be true');
      assert.equal(now.tags, changes.tags, 'When there are changes, the new value should always be what is set into changes');
      assert.notOk(changes.note, 'There was no change to note, so there should be nothing set');
      assert.equal(now.owner, changes.owner, 'When there are changes, the new value should always be what is set into changes');
      assert.notOk(changes.contacts, 'There was no change to contacts, so there should be nothing set');
    });

  });

});
