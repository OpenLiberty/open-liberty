# lodash-id [![Build Status](https://travis-ci.org/typicode/lodash-id.svg)](https://travis-ci.org/typicode/lodash-id) [![NPM version](https://badge.fury.io/js/lodash-id.svg)](http://badge.fury.io/js/lodash-id)

> `lodash-id` makes it easy to manipulate id-based resources with [lodash](https://lodash.com/) or [lowdb](https://github.com/typicode/lowdb)

* `getById`
* `insert`
* `upsert`
* `updateById`
* `updateWhere`
* `replaceById`
* `removeById`
* `removeWhere`
* `save`
* `load`
* `createId`


## Install

__Node__

```bash
$ npm install lodash lodash-id
```

__Note__ lodash-id is also compatible with [underscore](http://underscorejs.org/)


## Usage example

```js
const _  = require('lodash')

_.mixin(require('lodash-id'))
```

Create an empty database object

```js
const db = {
  posts: []
}
```

Create a post

```js
const newPost = _.insert(db.posts, {title: 'foo'})
```

Display database `console.log(db)`

```js
{
  posts: [
    {title: "foo", id: "5ca959c4-b5ab-4336-aa65-8a197b6dd9cb"}
  ]
}
```

Retrieve post using lodash-id `get` or underscore `find` method

```js
const post = _.getById(db.posts, newPost.id)

const post = _.find(db.posts, function(post) {
  return post.title === 'foo'
})
```

Persist

```js
_.save(db)
```

## API

The following database object is used in API examples.

```js
const db = {
  posts: [
    {id: 1, body: 'one', published: false},
    {id: 2, body: 'two', published: true}
  ],
  comments: [
    {id: 1, body: 'foo', postId: 1},
    {id: 2, body: 'bar', postId: 2}
  ]
}
```

__getById(collection, id)__

Finds and returns document by id or undefined.

```js
const post = _.getById(db.posts, 1)
```

__insert(collection, document)__

Adds document to collection, sets an id and returns created document.

```js
const post = _.insert(db.posts, {body: 'New post'})
```

If the document already has an id, and it is the same as an existing document in the collection, an error is thrown.

```js
_.insert(db.posts, {id: 1, body: 'New post'})
_.insert(db.posts, {id: 1, title: 'New title'}) // Throws an error
```

__upsert(collection, document)__

Adds document to collection, sets an id and returns created document.

```js
const post = _.upsert(db.posts, {body: 'New post'})
```

If the document already has an id, it will be used to insert or replace.

```js
_.upsert(db.posts, {id: 1, body: 'New post'})
_.upsert(db.posts, {id: 1, title: 'New title'})
_.getById(db.posts, 1) // {id: 1, title: 'New title'}
```

__updateById(collection, id, attrs)__

Finds document by id, copies properties to it and returns updated document or undefined.

```js
const post = _.updateById(db.posts, 1, {body: 'Updated body'})
```

__updateWhere(collection, whereAttrs, attrs)__

Finds documents using `_.where`, updates documents and returns updated documents or an empty array.

```js
// Publish all unpublished posts
const posts = _.updateWhere(db.posts, {published: false}, {published: true})
```

__replaceById(collection, id, attrs)__

Finds document by id, replaces properties and returns document or undefined.

```js
const post = _.replaceById(db.posts, 1, {foo: 'bar'})
```

__removeById(collection, id)__

Removes document from collection and returns it or undefined.

```js
const comment = _.removeById(db.comments, 1)
```

__removeWhere(collection, whereAttrs)__

Removes documents from collection using `_.where` and returns removed documents or an empty array.

```js
const comments = _.removeWhere(db.comments, {postId: 1})
```

__save(db, [destination])__

Persists database using localStorage or filesystem. If no destination is specified it will save to `db` or `./db.json`.

```js
_.save(db)
_.save(db, '/some/path/db.json')
```

__load([source])__

Loads database from localStorage or filesystem. If no source is specified it will load from `db` or `./db.json`.

```js
const db = _.load()
const db = _.load('/some/path/db.json')
```

__id__

Overwrite it if you want to use another id property.

```js
_.id = '_id'
```

__createId(collectionName, doc)__

Called by lodash-id when a document is inserted. Overwrite it if you want to change id generation algorithm.

```js
_.createId = function(collectionName, doc) {
  return collectionName + '-' + doc.name + '-' + _.random(1, 9999)
}
```

## FAQ

### How to reduce file size?

With Lodash, you can create custom builds and include just what you need.


```bash
$ npm install -g lodash-cli
$ lodash include=find,forEach,indexOf,filter,has
```

For more build options, see http://lodash.com/custom-builds.

## Changelog

See details changes for each version in the [release notes](https://github.com/typicode/lodash-id/releases).

## License

lodash-id is released under the MIT License.
