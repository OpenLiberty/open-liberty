'use strict';


const {Collapse} = require('./Collapse');
const {UnmountClosed} = require('./UnmountClosed');


UnmountClosed.Collapse = Collapse;
UnmountClosed.UnmountClosed = UnmountClosed;


module.exports = UnmountClosed;
