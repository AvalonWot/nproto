package io.nproto.benchmark;

import io.nproto.PojoMessage;
import io.nproto.Reader;
import io.nproto.schema.Schema;
import io.nproto.schema.SchemaFactory;
import io.nproto.schema.gen.AsmSchemaFactory;
import io.nproto.schema.handwritten.HandwrittenSchemaFactory;
import io.nproto.schema.reflect.AndroidUnsafeReflectiveSchemaFactory;
import io.nproto.schema.reflect.UnsafeReflectiveSchemaFactory;
import io.nproto.util.TestUtil;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
@Fork(1)
public class MergeFromBenchmark {
  public enum SchemaType {
    HANDWRITTEN(new HandwrittenSchemaFactory()),
    REFLECTIVE(new UnsafeReflectiveSchemaFactory()),
    ANDROID_REFLECTIVE(new AndroidUnsafeReflectiveSchemaFactory()),
    ASM(new AsmSchemaFactory());

    SchemaType(SchemaFactory factory) {
      this.factory = factory;
      schema = factory.createSchema(PojoMessage.class);
    }

    final void mergeFrom(PojoMessage message, Reader reader) {
      schema.mergeFrom(message, reader);
    }

    final SchemaFactory factory;
    final Schema<PojoMessage> schema;
  }

  @Param
  public SchemaType schemaType;

  private PojoMessage msg = TestUtil.newTestMessage();
  private TestUtil.PojoReader reader = new TestUtil.PojoReader(msg);

  @Benchmark
  public void mergeFrom() {
    schemaType.mergeFrom(new PojoMessage(), reader);
    reader.reset();
  }
}
