var path = require('path');

var config = {
  devtool: 'sourcemap',
  entry: {
    index: './src/ObjectInspector.js',
  },
  output: {
    path: path.join(__dirname, 'build'),
    publicPath: 'build/',
    filename: 'react-object-inspector.js',
    sourceMapFilename: 'react-object-inspector.map',
    library: 'ObjectInspector',
    libraryTarget: 'umd',
  },
  module: {
    loaders: [{
      test: /\.(js|jsx)/,
      loader: 'babel',
    },    
    ],
  },
  plugins: [],
  resolve: {
    extensions: ['', '.js', '.jsx'],
  },
  externals: {
    'react': {
      root: 'React',
      commonjs2: 'react',
      commonjs: 'react',
      amd: 'react',
    },
  },
};

module.exports = config;
