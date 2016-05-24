package io.nproto.util;

import io.nproto.ByteString;
import io.nproto.Internal;
import io.nproto.JavaType;
import io.nproto.PojoMessage;
import io.nproto.Reader;
import io.nproto.WireFormat;
import io.nproto.descriptor.AnnotationBeanDescriptorFactory;
import io.nproto.descriptor.BeanDescriptor;
import io.nproto.descriptor.BeanDescriptorFactory;
import io.nproto.descriptor.PropertyDescriptor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Internal
public final class TestUtil {
  private TestUtil() {
  }

  public static PojoMessage newTestMessage() {
    PojoMessage msg = new PojoMessage();
    msg.uint32Field = 1;
    msg.int32Field = 2;
    msg.fixedInt32Field = 3;
    msg.sInt32Field = 4;
    msg.sFixedInt32Field = 5;

    msg.uint64Field = 5;
    msg.int64Field = 6;
    msg.fixedInt64Field = 7;
    msg.sInt64Field = 8;
    msg.sFixedInt64Field = 9;

    msg.stringField = "hello world";
    msg.bytesField = ByteString.copyFromUtf8("here are some bytes");
    msg.messageField = new Object();

    msg.uint32ListField = Arrays.asList(1, 2);
    msg.int32ListField = Arrays.asList(3, 4);
    msg.fixedInt32ListField = Arrays.asList(5, 6);
    msg.sInt32ListField = Arrays.asList(7, 8);
    msg.sFixedInt32ListField = Arrays.asList(9, 10);

    msg.uint64ListField = Arrays.asList(1L, 2L);
    msg.int64ListField = Arrays.asList(3L, 4L);
    msg.fixedInt64ListField = Arrays.asList(5L, 6L);
    msg.sInt64ListField = Arrays.asList(7L, 8L);
    msg.sFixedInt64ListField = Arrays.asList(9L, 10L);

    msg.stringListField = Arrays.asList("ab", "cd");
    msg.bytesListField = Arrays.asList(ByteString.copyFromUtf8("ab"), ByteString.copyFromUtf8("cd"));
    msg.messageListField = Arrays.asList(new Object(), new Object());
    return msg;
  }

  public static final class PojoDescriptorFactory implements BeanDescriptorFactory {
    private static final PojoDescriptorFactory INSTANCE = new PojoDescriptorFactory();

    private static final Field ENUM_FIELD = pojoField("enumField");
    private static final Field BOOL_FIELD = pojoField("boolField");
    private static final Field UINT32_FIELD = pojoField("uint32Field");
    private static final Field INT32_FIELD = pojoField("int32Field");
    private static final Field SINT32_FIELD = pojoField("sInt32Field");
    private static final Field FIXED32_FIELD = pojoField("fixedInt32Field");
    private static final Field SFIXED32_FIELD = pojoField("sFixedInt32Field");
    private static final Field UINT64_FIELD = pojoField("uint64Field");
    private static final Field INT64_FIELD = pojoField("int64Field");
    private static final Field SINT64_FIELD = pojoField("sInt64Field");
    private static final Field FIXED64_FIELD = pojoField("fixedInt64Field");
    private static final Field SFIXED64_FIELD = pojoField("sFixedInt64Field");
    private static final Field STRING_FIELD = pojoField("stringField");
    private static final Field BYTES_FIELD = pojoField("bytesField");
    private static final Field MESSAGE_FIELD = pojoField("messageField");
    private static final Field ENUM_LIST_FIELD = pojoField("enumListField");
    private static final Field BOOL_LIST_FIELD = pojoField("boolListField");
    private static final Field UINT32_LIST_FIELD = pojoField("uint32ListField");
    private static final Field INT32_LIST_FIELD = pojoField("int32ListField");
    private static final Field SINT32_LIST_FIELD = pojoField("sInt32ListField");
    private static final Field FIXED32_LIST_FIELD = pojoField("fixedInt32ListField");
    private static final Field SFIXED32_LIST_FIELD = pojoField("sFixedInt32ListField");
    private static final Field UINT64_LIST_FIELD = pojoField("uint64ListField");
    private static final Field INT64_LIST_FIELD = pojoField("int64ListField");
    private static final Field SINT64_LIST_FIELD = pojoField("sInt64ListField");
    private static final Field FIXED64_LIST_FIELD = pojoField("fixedInt64ListField");
    private static final Field SFIXED64_LIST_FIELD = pojoField("sFixedInt64ListField");
    private static final Field STRING_LIST_FIELD = pojoField("stringListField");
    private static final Field BYTES_LIST_FIELD = pojoField("bytesListField");
    private static final Field MESSAGE_LIST_FIELD = pojoField("messageListField");
    private static final BeanDescriptor DESCRIPTOR = descriptor();

    private PojoDescriptorFactory() {
    }

    public static PojoDescriptorFactory getInstance() {
      return INSTANCE;
    }

    private static Field pojoField(String name) {
      try {
        return PojoMessage.class.getDeclaredField(name);
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
    }

    private static BeanDescriptor descriptor() {
      List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>(30);
      properties.add(new PropertyDescriptor(
              ENUM_FIELD, 1, WireFormat.FieldType.ENUM));
      properties.add(new PropertyDescriptor(
              BOOL_FIELD, 2, WireFormat.FieldType.BOOL));
      properties.add(new PropertyDescriptor(
              UINT32_FIELD, 3, WireFormat.FieldType.UINT32));
      properties.add(new PropertyDescriptor(
              INT32_FIELD, 4, WireFormat.FieldType.INT32));
      properties.add(new PropertyDescriptor(
              SINT32_FIELD, 5, WireFormat.FieldType.SINT32));
      properties.add(new PropertyDescriptor(
              FIXED32_FIELD, 6, WireFormat.FieldType.FIXED32));
      properties.add(new PropertyDescriptor(
              SFIXED32_FIELD, 7, WireFormat.FieldType.SFIXED32));
      properties.add(new PropertyDescriptor(
              UINT64_FIELD, 8, WireFormat.FieldType.UINT64));
      properties.add(new PropertyDescriptor(
              INT64_FIELD, 9, WireFormat.FieldType.INT64));
      properties.add(new PropertyDescriptor(
              SINT64_FIELD, 10, WireFormat.FieldType.SINT64));
      properties.add(new PropertyDescriptor(
              FIXED64_FIELD, 11, WireFormat.FieldType.FIXED64));
      properties.add(new PropertyDescriptor(
              SFIXED64_FIELD, 12, WireFormat.FieldType.SFIXED64));
      properties.add(new PropertyDescriptor(
              STRING_FIELD, 13, WireFormat.FieldType.STRING));
      properties.add(new PropertyDescriptor(
              BYTES_FIELD, 14, WireFormat.FieldType.BYTES));
      properties.add(new PropertyDescriptor(
              MESSAGE_FIELD, 15, WireFormat.FieldType.MESSAGE));
      properties.add(new PropertyDescriptor(
              ENUM_LIST_FIELD, 16, WireFormat.FieldType.ENUM));
      properties.add(new PropertyDescriptor(
              BOOL_LIST_FIELD, 17, WireFormat.FieldType.BOOL));
      properties.add(new PropertyDescriptor(
              UINT32_LIST_FIELD, 18, WireFormat.FieldType.UINT32));
      properties.add(new PropertyDescriptor(
              INT32_LIST_FIELD, 19, WireFormat.FieldType.INT32));
      properties.add(new PropertyDescriptor(
              SINT32_LIST_FIELD, 20, WireFormat.FieldType.SINT32));
      properties.add(new PropertyDescriptor(
              FIXED32_LIST_FIELD, 21, WireFormat.FieldType.FIXED32));
      properties.add(new PropertyDescriptor(
              SFIXED32_LIST_FIELD, 22, WireFormat.FieldType.SFIXED32));
      properties.add(new PropertyDescriptor(
              UINT64_LIST_FIELD, 23, WireFormat.FieldType.UINT64));
      properties.add(new PropertyDescriptor(
              INT64_LIST_FIELD, 24, WireFormat.FieldType.INT64));
      properties.add(new PropertyDescriptor(
              SINT64_LIST_FIELD, 25, WireFormat.FieldType.SINT64));
      properties.add(new PropertyDescriptor(
              FIXED64_LIST_FIELD, 26, WireFormat.FieldType.FIXED64));
      properties.add(new PropertyDescriptor(
              SFIXED64_LIST_FIELD, 27, WireFormat.FieldType.SFIXED64));
      properties.add(new PropertyDescriptor(
              STRING_LIST_FIELD, 28, WireFormat.FieldType.STRING));
      properties.add(new PropertyDescriptor(
              BYTES_LIST_FIELD, 29, WireFormat.FieldType.BYTES));
      properties.add(new PropertyDescriptor(
              MESSAGE_LIST_FIELD, 30, WireFormat.FieldType.MESSAGE));
      return new BeanDescriptor(properties);
    }

    @Override
    public BeanDescriptor descriptorFor(Class<?> clazz) {
      if (!PojoMessage.class.isAssignableFrom(clazz)) {
        throw new IllegalArgumentException("Unsupported class: " + clazz.getName());
      }
      return DESCRIPTOR;
    }
  }

  public static final class PojoReader implements Reader {
    private final FieldValue[] fieldValues;
    private int index;

    public PojoReader(PojoMessage msg) {
      fieldValues = fieldValuesFor(msg);
    }

    public void reset() {
      index = 0;
    }

    @Override
    public int fieldNumber() {
      if (index >= fieldValues.length) {
        return READ_DONE;
      }
      return fieldValues[index].fieldNumber;
    }

    @Override
    public boolean skipField() {
      return ++index < fieldValues.length;
    }

    @Override
    public double readDouble() {
      return fieldValues[index++].getDouble();
    }

    @Override
    public float readFloat() {
      return fieldValues[index++].getFloat();
    }

    @Override
    public long readUInt64() {
      return fieldValues[index++].getLong();
    }

    @Override
    public long readInt64() {
      return fieldValues[index++].getLong();
    }

    @Override
    public int readInt32() {
      return fieldValues[index++].getInt();
    }

    @Override
    public long readFixed64() {
      return fieldValues[index++].getLong();
    }

    @Override
    public int readFixed32() {
      return fieldValues[index++].getInt();
    }

    @Override
    public boolean readBool() {
      return fieldValues[index++].getBool();
    }

    @Override
    public String readString() {
      return (String) fieldValues[index++].value;
    }

    @Override
    public Object readMessage() {
      return fieldValues[index++].value;
    }

    @Override
    public ByteString readBytes() {
      return (ByteString) fieldValues[index++].value;
    }

    @Override
    public int readUInt32() {
      return fieldValues[index++].getInt();
    }

    @Override
    public Enum<?> readEnum() {
      return (Enum<?>) fieldValues[index++].value;
    }

    @Override
    public int readSFixed32() {
      return fieldValues[index++].getInt();
    }

    @Override
    public long readSFixed64() {
      return fieldValues[index++].getLong();
    }

    @Override
    public int readSInt32() {
      return fieldValues[index++].getInt();
    }

    @Override
    public long readSInt64() {
      return fieldValues[index++].getLong();
    }
  }

  private static FieldValue[] fieldValuesFor(Object msg) {
    List<FieldValue> fieldValues = new ArrayList<FieldValue>();
    List<PropertyDescriptor> protoProperties =
            AnnotationBeanDescriptorFactory.getInstance().descriptorFor(msg.getClass())
                    .getPropertyDescriptors();
    for (PropertyDescriptor info : protoProperties) {
      Object value;
      try {
        value = info.field.get(msg);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
      if (value instanceof List) {
        List<?> entries = (List<?>) value;
        for (Object entry : entries) {
          addFieldValue(info, entry, fieldValues);
        }
      } else if (value != null) {
        addFieldValue(info, value, fieldValues);
      }
    }
    return fieldValues.toArray(new FieldValue[fieldValues.size()]);
  }

  private static void addFieldValue(PropertyDescriptor info, Object value, List<FieldValue> fieldValues) {
    if (value instanceof Integer && value != Integer.valueOf(0)) {
      fieldValues.add(new FieldValue(info.fieldNumber, JavaType.INT, value));
    } else if (value instanceof Long && value != Long.valueOf(0)) {
      fieldValues.add(new FieldValue(info.fieldNumber, JavaType.LONG, value));
    } else if (value instanceof Double && Double.compare(0.0, (Double) value) != 0) {
      fieldValues.add(new FieldValue(info.fieldNumber, JavaType.DOUBLE, value));
    } else if (value instanceof Float && Float.compare(0.0f, (Float) value) != 0) {
      fieldValues.add(new FieldValue(info.fieldNumber, JavaType.FLOAT, value));
    } else if (value instanceof String && !((String) value).isEmpty()) {
      fieldValues.add(new FieldValue(info.fieldNumber, JavaType.STRING, value));
    } else if (value instanceof ByteString && !((ByteString) value).isEmpty()) {
      fieldValues.add(new FieldValue(info.fieldNumber, JavaType.BYTE_STRING, value));
    } else if (value != null) {
      fieldValues.add(new FieldValue(info.fieldNumber, JavaType.MESSAGE, value));
    }
  }

  private static final class FieldValue {
    final int fieldNumber;
    final JavaType javaType;
    final Object value;

    FieldValue(int fieldNumber, JavaType javaType, Object value) {
      this.fieldNumber = fieldNumber;
      this.javaType = javaType;
      this.value = value;
    }

    double getDouble() {
      return (Double) value;
    }

    float getFloat() {
      return (Float) value;
    }

    int getInt() {
      return (Integer) value;
    }

    long getLong() {
      return (Long) value;
    }

    boolean getBool() {
      return (Boolean) value;
    }
  }
}