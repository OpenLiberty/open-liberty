// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: ConsumerStore.proto

package com.ibm.test.g3store.grpc;

public interface GenreCountsOrBuilder extends
    // @@protoc_insertion_point(interface_extends:test.g3store.grpc.GenreCounts)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.GenreType genreType = 1;</code>
   * @return The enum numeric value on the wire for genreType.
   */
  int getGenreTypeValue();
  /**
   * <code>.GenreType genreType = 1;</code>
   * @return The genreType.
   */
  com.ibm.test.g3store.grpc.GenreType getGenreType();

  /**
   * <code>int32 totalCount = 2;</code>
   * @return The totalCount.
   */
  int getTotalCount();
}
