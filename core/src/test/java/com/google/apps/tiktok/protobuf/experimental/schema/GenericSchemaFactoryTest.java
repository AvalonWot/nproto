package com.google.apps.tiktok.protobuf.experimental.schema;

import com.google.apps.tiktok.protobuf.experimental.testing.TestMessage;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GenericSchemaFactoryTest extends AbstractSchemaFactoryTest {
  private static final Schema<TestMessage> SCHEMA =
      new GenericSchemaFactory().createSchema(TestMessage.class);

  @Override
  protected Schema<TestMessage> schema() {
    return SCHEMA;
  }
}
