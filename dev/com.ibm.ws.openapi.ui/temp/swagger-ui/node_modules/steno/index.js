var fs = require('graceful-fs')
var path = require('path')

var writers = {}

// Returns a temporary file
// Example: for /some/file will return /some/.~file
function getTempFile (file) {
  return path.join(path.dirname(file), '.~' + path.basename(file))
}

function Writer (file) {
  this.file = file
  this.callbacks = []
  this.nextData = null
  this.nextCallbacks = []
}

Writer.prototype.write = function (data, cb) {
  if (this.lock) {
    // File is locked
    // Save callback for later
    this.nextCallbacks.push(cb)
    // Set next data to be written
    this.nextData = data
  } else {
    // File is not locked
    // Lock it
    this.lock = true

    // Write data to a temporary file
    var tmpFile = getTempFile(this.file)
    fs.writeFile(tmpFile, data, function (err) {
      if (err) {
        // On error, call all the stored callbacks and the current one
        // Then return
        while (this.callbacks.length) this.callbacks.shift()(err)
        cb(err)
        return
      }

      // On success rename the temporary file to the real file
      fs.rename(tmpFile, this.file, function (err) {
        // call all the stored callbacks and the current one
        while (this.callbacks.length) this.callbacks.shift()(err)
        cb()

        // Unlock file
        this.lock = false

        // Write next data if any
        if (this.nextData) {
          var data = this.nextData
          this.callbacks = this.nextCallbacks

          this.nextData = null
          this.nextCallbacks = []

          this.write(data, this.callbacks.pop())
        }
      }.bind(this))
    }.bind(this))
  }
}

module.exports.writeFile = function (file, data, cb) {
  // Convert to absolute path
  file = path.resolve(file)

  // Create or get writer
  writers[file] = writers[file] || new Writer(file)

  // Write
  writers[file].write(data, cb)
}

module.exports.writeFileSync = function (file, data) {
  fs.writeFileSync(getTempFile(file), data)
  fs.renameSync(getTempFile(file), file)
}
