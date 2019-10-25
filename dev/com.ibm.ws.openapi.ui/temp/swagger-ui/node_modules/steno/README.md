# steno [![](http://img.shields.io/npm/dm/steno.svg?style=flat)](https://www.npmjs.org/package/steno)  [![](https://travis-ci.org/typicode/steno.svg?branch=master)](https://travis-ci.org/typicode/steno)

> Simple file writer with __atomic writing__ and __race condition prevention__.

Can be used as a drop-in replacement to `fs.writeFile()`.

Built on [graceful-fs](https://github.com/isaacs/node-graceful-fs) and used in [lowdb](https://github.com/typicode/lowdb).

## Install

```
npm install steno --save
```

## Usage

```javascript
const steno = require('steno')

steno.writeFile('file.json', data, err => {
  if (err) throw err
})
```

## The problem it solves

### Without steno

Let's say you have a server and want to save data to disk:

```javascript
var data = { counter: 0 }

server.post('/', (req, res) => {
  // Increment counter
  ++data.counter

  // Save data asynchronously
  fs.writeFile('data.json', JSON.stringify(data), err => {
    if (err) throw err
    res.end()
  })
})
```

Now if you have many requests, for example `1000`, there's a risk that you end up with:

```javascript
// In your server
data.counter === 1000

// In data.json
data.counter === 865 // ... or any other value
```

Why? Because, `fs.write` doesn't guarantee that the call order will be kept. Also, if the server is killed while `data.json` is being written, the file can get corrupted.

### With steno

```javascript
server.post('/increment', (req, res) => {
  ++data.counter

  steno.writeFile('data.json', JSON.stringify(data), err => {
    if (err) throw err
    res.end()
  })
})
```

With steno you'll always have the same data in your server and file. And in case of a crash, file integrity will be preserved.

if needed, you can also use `steno.writeFileSync()` which offers atomic writing too.

__Important: works only in a single instance of Node.__

## License

MIT - [Typicode](https://github.com/typicode)
