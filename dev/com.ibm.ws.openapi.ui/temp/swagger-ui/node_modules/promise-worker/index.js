'use strict';

/* istanbul ignore next */
var MyPromise = typeof Promise !== 'undefined' ? Promise : require('lie');

var messageIds = 0;

function parseJsonSafely(str) {
  try {
    return JSON.parse(str);
  } catch (e) {
    return false;
  }
}

function onMessage(self, e) {
  var message = parseJsonSafely(e.data);
  if (!message) {
    // Ignore - this message is not for us.
    return;
  }
  var messageId = message[0];
  var error = message[1];
  var result = message[2];

  var callback = self._callbacks[messageId];

  if (!callback) {
    // Ignore - user might have created multiple PromiseWorkers.
    // This message is not for us.
    return;
  }

  delete self._callbacks[messageId];
  callback(error, result);
}

function PromiseWorker(worker) {
  var self = this;
  self._worker = worker;
  self._callbacks = {};

  worker.addEventListener('message', function (e) {
    onMessage(self, e);
  });
}

PromiseWorker.prototype.postMessage = function (userMessage) {
  var self = this;
  var messageId = messageIds++;

  var messageToSend = [messageId, userMessage];

  return new MyPromise(function (resolve, reject) {
    self._callbacks[messageId] = function (error, result) {
      if (error) {
        return reject(new Error(error.message));
      }
      resolve(result);
    };
    var jsonMessage = JSON.stringify(messageToSend);
    /* istanbul ignore if */
    if (typeof self._worker.controller !== 'undefined') {
      // service worker, use MessageChannels because e.source is broken in Chrome < 51:
      // https://bugs.chromium.org/p/chromium/issues/detail?id=543198
      var channel = new MessageChannel();
      channel.port1.onmessage = function (e) {
        onMessage(self, e);
      };
      self._worker.controller.postMessage(jsonMessage, [channel.port2]);
    } else {
      // web worker
      self._worker.postMessage(jsonMessage);
    }
  });
};

module.exports = PromiseWorker;