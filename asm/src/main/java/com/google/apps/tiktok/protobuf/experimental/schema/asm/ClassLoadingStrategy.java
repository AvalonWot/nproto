package com.google.apps.tiktok.protobuf.experimental.schema.asm;

import com.google.apps.tiktok.protobuf.experimental.InternalApi;

/**
 * A strategy for loading the generated bytecode into the application.
 */
@InternalApi
public interface ClassLoadingStrategy {
  /**
   * Loads the generated schema class.
   *
   * @param messageClass the message class that the schema was generated for.
   * @param name the fully-qualified name of the class being loaded.
   * @param binaryRepresentation the bytecode for the schema.
   * @return the schema class.
   */
  Class<?> loadSchemaClass(Class<?> messageClass, String name, byte[] binaryRepresentation);

  /**
   * Indicates whether the classes generated by this strategy will have package-private access to
   * the message class.
   */
  boolean isPackagePrivateAccessSupported();
}