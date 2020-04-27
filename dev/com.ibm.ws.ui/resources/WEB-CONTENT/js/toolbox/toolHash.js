define(['dojo/hash', 'dojo/i18n!login/nls/loginMessages'],
    function(hash, i18n) {
  'use strict';

  /**
   * Computes from the hash the tool ID segment.
   */
  function getToolSegment(hashValue) {
    var i = hashValue.indexOf('/');
    if (i > 0) {
      return hashValue.substring(0, i);
    } else {
      return hashValue;
    }
  }

  return {
    /** Tracks the last value of the set tool ID */
    __lastId: null,

    /**
     * Tracks whether or not anything has changed since __lastId was changed.
     * Note that this tracking flag is updated via wasSet().
     */
    __wasSet: false,

    /**
     * Determines if the tool hash is set.
     * 
     * @return true if the hash is set with a tool hash, false otherwise
     */
    isSet: function() {
      return this.get() ? true : false;
    },

    /**
     * Gets the tool ID from the hash.
     * 
     * @return The tool ID segment of the hash
     */
    get: function() {
      return getToolSegment(hash());
    },

    /**
     * Sets the tool hash.
     */
    set: function(tool) {
      this.__wasSet = true;
      this.__lastId = tool.hashId;
      hash(this.__lastId);
    },

    /**
     * Determines if the current value in the hash was the value just set by the set()
     * method.
     * 
     * Note, this works because we have one listener on /dojo/hashchange in LibertyToolbox.
     * This listener calls wasSet each time a change occurs, so we're always invalidating __wasSet.
     * 
     * @return true if the current value of the hash is the same as the last set value,
     *              and that there was no previous change to the hash value.
     */
    wasSet: function() {
      var wasSet = (this.__wasSet && hash() === this.__lastId);
      this.__wasSet = false;
      return wasSet;
    },

    /**
     * Clears the current tool hash. This pushes a new value onto the browser's history stack.
     */
    clear: function() {
      //hash('');
      // For now, map this to erase. Eventually, we will probably want to use # and not cause a page reload
      this.erase(); 
    },

    /**
     * Erases the current tool hash. This replaces the current history entry with one with no tool hash.
     */
    erase: function() {
      hash(''); // calling hash will leave a # in it causing problem with back arrow
      var index =  window.location.href.indexOf("#");
      if (index !== -1) {
        var newUrl = window.location.href.substring(0, index);
        // this will not reload the page instead just remove the hash
        if ("replaceState" in history) {
          try {
            // replaceState still keeps the # for IE
            window.history.replaceState({}, i18n.LOGIN_TITLE, newUrl);
            console.log("using replaceState");
          } catch (err) {
            //window.location.hash = "";
            console.error("Caught error", err, ". Setting window.location");
            window.location = newUrl;
          }
        }
      }
    },

    /**
     * Given two hash values, determines if the tool changed.
     * 
     * @return true if the tool in the hash changed, false otherwise
     */
    hasChanged: function(previousHash, currentHash) {
      return getToolSegment(previousHash) !== getToolSegment(currentHash);
    },

    /**
     * Function to get only the name of the feature's short name. The version (if present) is dropped.
     */
    getName: function(featureShortName) {
      if (!featureShortName) {
        return '';
      }

      var i = featureShortName.indexOf('-');
      if (i > 0) {
        return featureShortName.substring(0, i);
      } else {
        return featureShortName;
      }
    }
  };
});  
