## Summary
A functional automated test (FAT) bucket for the `grpc-1.0` and `grpcClient-1.0` features. 

## Building

The default build targets will not cause the gRPC stubs to be recompiled (via the [protobuf plugin](https://github.com/google/protobuf-gradle-plugin)). Any time changes are made to protobuf files, for example `./test-applications/HelloWorldService.war/resources/proto/helloworld.proto`, be sure enable the protobuf plugin via `-PcompileProtobufs`: 

    ./gradlew com.ibm.ws.grpc_fat:build -PcompileProtobufs

If changes have been made to the protobufs, this execution should cause changes in `./generated-src`. Check those changes in along with any protobuf changes.

## Running the tests
### Run in default LITE mode
    ./gradlew com.ibm.ws.grpc_fat:buildandrun

### Run in FULL mode
    ./gradlew com.ibm.ws.grpc_fat:buildandrun -Dfat.test.mode=full

