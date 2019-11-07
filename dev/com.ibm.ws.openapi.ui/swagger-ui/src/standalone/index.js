import StandaloneLayout from "./layout"
import HeaderbarPlugin from "plugins/headerbar"

// the Standalone preset

let preset = [
  HeaderbarPlugin,
  () => {
    return {
      components: { StandaloneLayout }
    }
  }
]

module.exports = preset
