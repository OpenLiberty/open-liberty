## Liberty modifications to Swagger UI

This is the [Swagger UI][swagger-ui] used by the `openapi-3.x` and `mpOpenApi-x.x` features.

### Changes from the base Swagger UI

- Add a header bar with the OpenLiberty logo and an optional filter field
  - the filter field relies on the backend to serve up a filtered openapi document, which is only implemented in the `openapi-x.x` features
- Move some information from the info section to the footer
- Change the colors for better accessibility (I assume this is for better contrast)

### Overview

For the most part, Swagger UI is pulled in as an npm dependency and our modifications build on top of it.

To make the color changes, we need to modify the original sass style files from swagger UI, but these aren't available from npm (only the compiled CSS is included in the package). Instead, we keep the original sass files checked in under src/style/original. We can then recompile them with our modifications added.

There are slight differences between the UI presented for the `openapi-3.x` and `mpOpenApi-x.x` features. The main code is capable of displaying either version and the config to choose which to use is in the html file where the initialization is done.

* `microprofile.html` is used for `mpOpenApi-x.x`
* `openapi.html` is used for `openapi-3.x`
* `dev.html` is used during development on our extension code when running `npm start`

### Building

The code is not built as part of the liberty build. Instead, it's built locally and the built output is checked in (in the dist folder). During the liberty build, the built files are included into the bundles where they're needed by bnd.

To build

```
npm run build -- --mode=production
```

The build creates the output files in the `dist` folder. These files are included into bundles by the `bnd.bnd` files in projects:
* `com.ibm.ws.openapi.ui` (this project)
* `com.ibm.ws.openapi.ui.private`
* `com.ibm.ws.microprofile.openapi.ui`

### Development

If changes need to be made to our extensions to swagger UI, `npm start` can be run from this directory to start a webpack development server. This allows you to make and test changes without rebuilding and restarting liberty.

When started like this, it expects to be able to load an openapi document from `http://localhost:9080/openapi`. The URL can be changed by editing `dev.html`.

### Updating dependencies

To update all our dependencies to the latest compatible version
```
npm update
npm install
```

If you've updated Swagger UI, then you also need to replace the sass files under `src/style/original` with the versions from the Swagger UI source code.

Here's some git magic to do that (replace `vX.X.X` with the actual tag for the release):

```
git clone --filter=blob:none --depth 1 --no-checkout -b vX.X.X git@github.com:swagger-api/swagger-ui.git
git -C swagger-ui sparse-checkout set src/style
git -C swagger-ui checkout
rm src/style/original/*
cp swagger-ui/src/style/* src/style/original
rm -rf swagger-ui
```

Finally, rebuild

```
npm run build -- --mode=production
```

### Other references

This was originally based on the [`webpack-getting-started`][webpack-sample] sample.

[swagger-ui]: https://github.com/swagger-api/swagger-ui
[webpack-sample]: https://github.com/swagger-api/swagger-ui/tree/df7749b2fe88c3235a2a7a2c965e8edaaa646356/docs/samples/webpack-getting-started