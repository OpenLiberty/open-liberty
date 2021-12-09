// ***********************************************************
// This example support/index.js is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

// Import commands.js using ES2015 syntax:
import "./commands"

// Alternatively you can use CommonJS syntax:
// require('./commands')


// Remove fetch, so Cypress can intercept XHRs
// see https://github.com/cypress-io/cypress/issues/95
Cypress.on("window:before:load", win => {
  win.fetch = null
})

Cypress.on("uncaught:exception", (err, runnable) => {
  console.log(JSON.stringify(err, null, 2))
  return true
})
