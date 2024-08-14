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

* `mpOpenApi.html` is used for `mpOpenApi-x.x`
* `openapi.html` is used for `openapi-3.x`
* `dev.html` is used during development on our extension code when running `npm start`

### Building

The code is not built as part of the liberty build. Instead, it's built locally and the built output is checked in (in the dist folder). During the liberty build, the built files are included into the bundles where they're needed by bnd.

To build, first make sure you have npm 10.x installed and available on your path.

```
$ npm --version
10.5.0
```

Then run

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

1. Make sure you have npm 10.x installed and available on your path.

   ```
   $ npm --version
   10.5.0
   ```

1. Run `update-openapi-ui.sh`

1. Rebuild the liberty bundles which include the built Swagger UI files. From the open-liberty `dev` directory:
   ```
   ./gradlew :com.ibm.ws.openapi.ui:assemble :com.ibm.ws.openapi.ui.private:assemble :com.ibm.ws.microprofile.openapi.ui:assemble
   ```

1. Make a commit with your changes. This should include:
   * Updating `package.json` and `package-lock.json` with the new dependency versions
   * Deleting the existing `.js` file in `dist` and creating a new one with a different filename
   * If there were styling changes in the new version of swagger-ui:
     * Changes to files in `src/style/original`
     * Deleting the existing `.css` file in `dist` and creating a new one with a different filename
   * Updating the `.html` files in `dist` with the new `.js` and `.css` file names

1. Make a PR and run a personal build
   * Add `#spawn.fullfat.buckets=io.openliberty.microprofile.openapi.ui.internal_fat,com.ibm.ws.openapi.ui_fat` to the request to run the Selenium UI tests

1. Using the result of your personal build, follow the manual test plan available at https://github.com/OpenLiberty/openapi-ui-test-app

### Testing

Before delivering changes, create a PR and run a personal build. Using the personal build result, follow the test plan available at https://github.com/OpenLiberty/openapi-ui-test-app

### Other references

This was originally based on the [`webpack-getting-started`][webpack-sample] sample.

[swagger-ui]: https://github.com/swagger-api/swagger-ui
[webpack-sample]: https://github.com/swagger-api/swagger-ui/tree/df7749b2fe88c3235a2a7a2c965e8edaaa646356/docs/samples/webpack-getting-started
[swagger-ui-repo]: https://github.com/swagger-api/swagger-ui
[swagger-ui-releases]: https://github.com/swagger-api/swagger-ui/releases
