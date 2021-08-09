import StandaloneLayout from "./layout"
import TopbarPlugin from "plugins/topbar"
import ConfigsPlugin from "corePlugins/configs"
import HeaderbarPlugin from "plugins/headerbar"

// the Standalone preset

export default [
  TopbarPlugin,
  ConfigsPlugin,
  HeaderbarPlugin,
  () => {
    return {
      components: { StandaloneLayout }
    }
  }
]
