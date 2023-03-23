const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const postcssPresetEnv = require('postcss-preset-env');
const cssnano = require('cssnano');
const { ProvidePlugin } = require('webpack');

const outputPath = path.resolve(__dirname, 'dist');

module.exports = {
  mode: 'development',
  entry: {
    'liberty-swagger-ui': require.resolve('./src/index'),
  },
  resolve: {
    extensions: ['.ts', '.js'],
    fallback: {
      "stream": require.resolve("stream-browserify"),
    }
  },
  devtool: "hidden-source-map",
  performance : {
    // SwaggerUI itself is already big enough to prompt warnings from webpack
    // so webpack's hints aren't helpful to us.
    hints: false,
  },
  module: {
    rules: [
      {
        test: /\.css$/,
        use: [
          MiniCssExtractPlugin.loader,
          "css-loader",
          {
            loader: "postcss-loader",
            options: {
              postcssOptions: {
                plugins: [
                  postcssPresetEnv(),
                  cssnano(),
                ],
              }
            },
          },
        ]
      },
      {
        test: /\.s[ac]ss$/i,
        use: [
          MiniCssExtractPlugin.loader,
          "css-loader",
          {
            loader: "postcss-loader",
            options: {
              postcssOptions: {
                plugins: [
                  postcssPresetEnv(),
                  cssnano(),
                ],
              }
            },
          },
          {
            loader: "sass-loader",
            options: {
              sassOptions: {
                outputStyle: "expanded",
              },
            },
          },
        ]
      },
      {
        test: /\.js$/,
        enforce: "pre",
        use: ["source-map-loader"],
      },
      {
        test: /\.m?jsx?$/,
        include: [
          path.resolve(__dirname, 'src/'),
          path.resolve(__dirname, 'node_modules/swagger-ui/dist/'),
          // Additional libraries which need transformed for IE11 compatibility
          path.resolve(__dirname, 'node_modules/buffer/'),
          path.resolve(__dirname, 'node_modules/fraction.js/'),
          path.resolve(__dirname, 'node_modules/patch-package/'),
          path.resolve(__dirname, 'node_modules/drange/'),
          path.resolve(__dirname, 'node_modules/serialize-error/'),
          path.resolve(__dirname, 'node_modules/highlight.js/'),
          path.resolve(__dirname, 'node_modules/randexp/'),
          path.resolve(__dirname, 'node_modules/ret/'),
          path.resolve(__dirname, 'node_modules/formdata-node/'),
          path.resolve(__dirname, 'node_modules/fast-json-patch/'),
          path.resolve(__dirname, 'node_modules/react-redux/'),
          path.resolve(__dirname, 'node_modules/swagger-client/'),
        ],
        loader: "babel-loader",
        options: {
          sourceType: "unambiguous", // babel-preset-env breaks on swagger UI without this
          presets: [
            [
              '@babel/preset-env',
              {
                useBuiltIns: 'usage',
                corejs: '3.19'
              }
            ],
            '@babel/preset-react'
          ],
          retainLines: true,
          cacheDirectory: true,
        },
      },
    ]
  },
  plugins: [
    new CleanWebpackPlugin(),
    new CopyWebpackPlugin({
      patterns: [
        {
          // Copy the Swagger OAuth2 redirect file to the project root;
          // that file handles the OAuth2 redirect after authenticating the end-user.
          from: 'node_modules/swagger-ui/dist/oauth2-redirect.html',
          to: './'
        },
        {
          from: '**',
          to: './',
          context: 'static'
        }
      ],
    }),
    new HtmlWebpackPlugin({
      template: 'openapi.html',
      filename: 'openapi.html',
      inject: false,
      minify: false
    }),
    new HtmlWebpackPlugin({
      template: 'mpOpenApi.html',
      filename: 'mpOpenApi.html',
      inject: false,
      minify: false
    }),
    new HtmlWebpackPlugin({
      template: 'dev.html',
      filename: 'index.html',
      inject: false,
      minify: false
    }),
    new MiniCssExtractPlugin({
      filename: '[name]-[contenthash].css'
    }),
    new ProvidePlugin({
      process: "process/browser",
      Buffer: ["buffer", "Buffer"],
    }),
  ],
  output: {
    filename: '[name]-bundle-[contenthash].js',
    path: outputPath,
    library: 'SwaggerUI',
    publicPath: '' // Needed for IE
  },
  ignoreWarnings: [/Failed to parse source map/],
};
