/**
 * Entry point for actually loading the toolbox.
 * 
 * The first thing we need to do is restore the hash from the cookie which was created during the login. Once the hash is restored, we're
 * ready to initialize the toolbox.
 */
require([ 'login/hashCookie', 'js/toolbox/views/toolbox', 'dojo/dom', 'dojo/hash', 'dojo/topic' ], function(hashCookie, toolbox, dom, hash,
    topic) {
  'use strict';

  /*
   * The Below code is used to get around the fact that our tools are currently being opened as iframes 
   * (we will likely want to change this since it causes complications) and an iFrame has its own hash 
   * (each window has its own hash, and an iframe is its own window).  dojo/hash reads the current window's
   * hash, so it doesn't work for us in the tool.  So instead, we subscribe to hashchange notifications here,
   * and if a tool iframe is present, we update its hash.  This allows us to rely on dojo/hash for reading 
   * the hash from within the tool, but setting it still has to be done 'manually' using top.location.hash
   */
  topic.subscribe("/dojo/hashchange", function(changedHash) {
    //    console.error('hashchanged in toolbox');
    var iframe = dom.byId("toolIFrame");
    if (iframe) {
      var win = (iframe.contentWindow || iframe.contentDocument);
      if (win.document) {
        win = win.document;
      }
      // TODO: What should we check to see if the iframe has loaded?  Probably not host & hostname!
      if ((win.location.host || win.location.hostname) && (win.location.hash !== '#' + changedHash)) {
        // We have to use replace in order to not create a duplicate history
        // Origin is not supported on all browsers (like FF on certain linux distros), so instead either build the URl or grab it from href
        // win.location.replace(win.location.origin + win.location.pathname + '#' + changedHash); <- Not safe due to FF bug
        // win.location.replace(win.location.href.substring(0, win.location.href.indexOf('#')) + '#' + changedHash); <- Alternative incase we find issues with the below
        win.location.replace(win.location.protocol + "//" + win.location.host + win.location.pathname + '#' + changedHash);
      }
    }

  });

  try {
    hashCookie.restoreHashFromCookie();

    toolbox.initialize();
  } catch (err) {
    console.error('Critical error occured while trying to load the toolbox. Error: ', err);
  }

});
