/**
 * @prettier
 */

// NOTE: this config *does not* inherit from `_config-builder`.
// It is also used in the dev config.

import path from "path"
import MiniCssExtractPlugin from "mini-css-extract-plugin"
import IgnoreAssetsPlugin from "ignore-assets-webpack-plugin"

export default {
  mode: "production",

  entry: {
    "swagger-ui": "./src/style/main.scss",
  },

  module: {
    rules: [
      {
        test: [/\.(scss)(\?.*)?$/],
        use: [
          {
            loader: MiniCssExtractPlugin.loader,
          },
          {
            loader: "css-loader",
            options: { sourceMap: true },
          },
          {
            loader: "postcss-loader",
            options: {
              postcssOptions: {
                sourceMap: true,
                plugins: [
                  require("cssnano")(),
                  "postcss-preset-env" // applies autoprefixer
                ],

              }
            },
          },
          {
            loader: "sass-loader",
            options: {
              sourceMap: true,
              sassOptions: {
                outputStyle: "expanded",
                // sourceMapContents: "true", // if sourceMap: true, sassOptions.sourceMapContents is ignored
              },
            },
          },
        ],
      },
    ],
  },

  plugins: [
    new MiniCssExtractPlugin({
      filename: "[name].css",
    }),
    new IgnoreAssetsPlugin({
      // This is a hack to avoid a Webpack/MiniCssExtractPlugin bug, for more
      // info see https://github.com/webpack-contrib/mini-css-extract-plugin/issues/151
      ignore: ["swagger-ui.js", "swagger-ui.js.map"],
    }),
  ],

  devtool: "source-map",

  output: {
    path: path.join(__dirname, "../", "dist"),
    publicPath: "/dist",
  },

  optimization: {
    splitChunks: {
      cacheGroups: {
        styles: {
          name: "styles",
          test: /\.css$/,
          chunks: "all",
          enforce: true,
        },
      },
    },
  },
}
