package io.nproto.schema.reflect;

import static io.nproto.UnsafeUtil.fieldOffset;

import io.nproto.ByteString;
import io.nproto.FieldType;
import io.nproto.JavaType;
import io.nproto.UnsafeUtil;
import io.nproto.Writer;
import io.nproto.schema.Field;
import io.nproto.schema.Schema;
import io.nproto.schema.SchemaUtil;
import io.nproto.schema.SchemaUtil.FieldInfo;

import sun.plugin.dom.exception.InvalidStateException;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

final class UnsafeReflectiveSchema<T> implements Schema<T> {
  private static final int ENTRIES_PER_FIELD = 2;
  private static final int LONG_LENGTH = 8;
  private static final int FIELD_LENGTH = ENTRIES_PER_FIELD * LONG_LENGTH;

  private final long[] data;
  private final long dataOffset;
  private final long dataLimit;

  // Array that holds lazy entries for fields.
  private WeakReference<Field[]> fields;

  //private final int[] fieldNumbers;
  //private final FieldType[] fieldTypes;
  //private final byte[] fieldTypes;

  static <T> UnsafeReflectiveSchema<T> newInstance(Class<T> messageType) {
    return new UnsafeReflectiveSchema<T>(messageType);
  }

  private UnsafeReflectiveSchema(Class<T> messageType) {
    List<FieldInfo> fieldInfos = SchemaUtil.getAllFieldInfo(messageType);
    final int numFields = fieldInfos.size();
    //data = new long[numFields];
    //fieldNumbers = new int[numFields];
    //fieldTypes = new byte[numFields];
    data = new long[numFields * ENTRIES_PER_FIELD];
    int lastFieldNumber = Integer.MAX_VALUE;
    for (int i = 0, dataPos = 0; i < numFields; ++i) {
      FieldInfo f = fieldInfos.get(i);
      if (f.fieldNumber == lastFieldNumber) {
        throw new RuntimeException("Duplicate field number: " + f.fieldNumber);
      }
      data[dataPos++] = (((long) f.fieldType.id()) << 32) | f.fieldNumber;
      data[dataPos++] = fieldOffset(f.field);
      //data[i] = fieldOffset(f.field);
      //fieldNumbers[i] = f.fieldNumber;
      //fieldTypes[i] = (byte) f.fieldType.id();
    }
    dataOffset = UnsafeUtil.arrayBaseOffset(long[].class);
    dataLimit = dataOffset + (data.length * LONG_LENGTH);
  }

  @Override
  public Iterator<Field> iterator() {
    Field[] fields = getOrCreateFields();
    return new FieldIterator(fields);
  }

  @Override
  public void writeTo(T message, Writer writer) {
    //for(int i = 0; i < data.length; ++i) {
    for(long pos = dataOffset; pos < dataLimit; pos += FIELD_LENGTH) {
      // Switching on the field type ID to avoid the lookup of FieldType.
      final int fieldNumber = getFieldNumber(getLong(pos));
      switch (getFieldTypeId(getLong(pos))) {
        case 1: //DOUBLE:
          SchemaUtil.unsafeWriteDouble(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 2: //FLOAT:
          SchemaUtil.unsafeWriteFloat(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 3: //INT64:
          SchemaUtil.unsafeWriteInt64(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 4: //UINT64:
          SchemaUtil.unsafeWriteUInt64(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 5: //INT32:
          SchemaUtil.unsafeWriteInt32(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 6: //FIXED64:
          SchemaUtil.unsafeWriteFixed64(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 7: //FIXED32:
          SchemaUtil.unsafeWriteFixed32(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 8: //BOOL:
          SchemaUtil.unsafeWriteBool(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 9: //STRING:
          SchemaUtil.unsafeWriteString(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 10: //MESSAGE:
          SchemaUtil.unsafeWriteMessage(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 11: //BYTES:
          SchemaUtil.unsafeWriteBytes(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 12: //UINT32:
          SchemaUtil.unsafeWriteUInt32(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 13: //ENUM:
          SchemaUtil.unsafeWriteEnum(fieldNumber, message, getLong(pos + LONG_LENGTH), writer, Enum.class);
          break;
        case 14: //SFIXED32:
          SchemaUtil.unsafeWriteSFixed32(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 15: //SFIXED64:
          SchemaUtil.unsafeWriteSFixed64(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 16: //SINT32:
          SchemaUtil.unsafeWriteSInt32(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 17: //SINT64:
          SchemaUtil.unsafeWriteSInt64(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 18: //DOUBLE_LIST:
          SchemaUtil.unsafeWriteDoubleList(fieldNumber, message, getLong(pos + LONG_LENGTH), false, writer);
          break;
        case 35: //PACKED_DOUBLE_LIST:
          SchemaUtil.unsafeWriteDoubleList(fieldNumber, message, getLong(pos + LONG_LENGTH), true, writer);
          break;
        case 19: //FLOAT_LIST:
          SchemaUtil.unsafeWriteFloatList(fieldNumber, message, getLong(pos + LONG_LENGTH), false, writer);
          break;
        case 36: //PACKED_FLOAT_LIST:
          SchemaUtil.unsafeWriteFloatList(fieldNumber, message, getLong(pos + LONG_LENGTH), true, writer);
          break;
        case 20: //INT64_LIST:
          SchemaUtil.unsafeWriteInt64List(fieldNumber, message, getLong(pos + LONG_LENGTH), false, writer);
          break;
        case 37: //PACKED_INT64_LIST:
          SchemaUtil.unsafeWriteInt64List(fieldNumber, message, getLong(pos + LONG_LENGTH), true, writer);
          break;
        case 21: //UINT64_LIST:
          SchemaUtil.unsafeWriteUInt64List(fieldNumber, message, getLong(pos + LONG_LENGTH), false, writer);
          break;
        case 38: //PACKED_UINT64_LIST:
          SchemaUtil.unsafeWriteUInt64List(fieldNumber, message, getLong(pos + LONG_LENGTH), true, writer);
          break;
        case 22: //INT32_LIST:
          SchemaUtil.unsafeWriteInt32List(fieldNumber, message, getLong(pos + LONG_LENGTH), false, writer);
          break;
        case 39: //PACKED_INT32_LIST:
          SchemaUtil.unsafeWriteInt32List(fieldNumber, message, getLong(pos + LONG_LENGTH), true, writer);
          break;
        case 23: //FIXED64_LIST:
          SchemaUtil.unsafeWriteFixed64List(fieldNumber, message, getLong(pos + LONG_LENGTH), false, writer);
          break;
        case 40: //PACKED_FIXED64_LIST:
          SchemaUtil.unsafeWriteFixed64List(fieldNumber, message, getLong(pos + LONG_LENGTH), true, writer);
          break;
        case 24: //FIXED32_LIST:
          SchemaUtil.unsafeWriteFixed32List(fieldNumber, message, getLong(pos + LONG_LENGTH), false, writer);
          break;
        case 41: //PACKED_FIXED32_LIST:
          SchemaUtil.unsafeWriteFixed32List(fieldNumber, message, getLong(pos + LONG_LENGTH), true, writer);
          break;
        case 25: //BOOL_LIST:
          SchemaUtil.unsafeWriteBoolList(fieldNumber, message, getLong(pos + LONG_LENGTH), false, writer);
          break;
        case 42: //PACKED_BOOL_LIST:
          SchemaUtil.unsafeWriteBoolList(fieldNumber, message, getLong(pos + LONG_LENGTH), true, writer);
          break;
        case 26: //STRING_LIST:
          SchemaUtil.unsafeWriteStringList(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 27: //MESSAGE_LIST:
          SchemaUtil.unsafeWriteMessageList(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 28: //BYTES_LIST:
          SchemaUtil.unsafeWriteBytesList(fieldNumber, message, getLong(pos + LONG_LENGTH), writer);
          break;
        case 29: //UINT32_LIST:
          SchemaUtil.unsafeWriteUInt32List(fieldNumber, message, getLong(pos + LONG_LENGTH), false, writer);
          break;
        case 43: //PACKED_UINT32_LIST:
          SchemaUtil.unsafeWriteUInt32List(fieldNumber, message, getLong(pos + LONG_LENGTH), true, writer);
          break;
        case 30: //ENUM_LIST:
          SchemaUtil.unsafeWriteEnumList(fieldNumber, message, getLong(pos + LONG_LENGTH), false, writer, Enum.class);
          break;
        case 44: //PACKED_ENUM_LIST:
          SchemaUtil.unsafeWriteEnumList(fieldNumber, message, getLong(pos + LONG_LENGTH), true, writer, Enum.class);
          break;
        case 31: //SFIXED32_LIST:
          SchemaUtil.unsafeWriteSFixed32List(fieldNumber, message, getLong(pos + LONG_LENGTH), false, writer);
          break;
        case 45: //PACKED_SFIXED32_LIST:
          SchemaUtil.unsafeWriteSFixed32List(fieldNumber, message, getLong(pos + LONG_LENGTH), true, writer);
          break;
        case 32: //SFIXED64_LIST:
          SchemaUtil.unsafeWriteSFixed64List(fieldNumber, message, getLong(pos + LONG_LENGTH), false, writer);
          break;
        case 46: //PACKED_SFIXED64_LIST:
          SchemaUtil.unsafeWriteSFixed64List(fieldNumber, message, getLong(pos + LONG_LENGTH), true, writer);
          break;
        case 33: //SINT32_LIST:
          SchemaUtil.unsafeWriteSInt32List(fieldNumber, message, getLong(pos + LONG_LENGTH), false, writer);
          break;
        case 47: //PACKED_SINT32_LIST:
          SchemaUtil.unsafeWriteSInt32List(fieldNumber, message, getLong(pos + LONG_LENGTH), true, writer);
          break;
        case 34: //SINT64_LIST:
          SchemaUtil.unsafeWriteSInt64List(fieldNumber, message, getLong(pos + LONG_LENGTH), false, writer);
          break;
        case 48: //PACKED_SINT64_LIST:
          SchemaUtil.unsafeWriteSInt64List(fieldNumber, message, getLong(pos + LONG_LENGTH), true, writer);
          break;
        default:
          throw new IllegalArgumentException("Unsupported fieldType: " + getFieldType(getLong(pos)));
      }
    }
  }

  private long getLong(long pos) {
    return UnsafeUtil.getLong(data, pos);
  }

  private Field[] getOrCreateFields() {
    Field[] temp = fields != null ? fields.get() : null;
    if (temp == null) {
      int numFields = data.length / ENTRIES_PER_FIELD;
      temp = new Field[numFields];
      for(int i = 0; i < numFields; ++i) {
        temp[i] = new FieldImpl(data, i * ENTRIES_PER_FIELD);
      }
      fields = new WeakReference<Field[]>(temp);
    }
    return temp;
  }

  private static FieldType getFieldType(long data) {
    return FieldType.forId(getFieldTypeId(data));
  }

  private static int getFieldNumber(long data) {
    return (int) data;
  }

  private static byte getFieldTypeId(long data) {
    return (byte) (data >> 32);
  }

  private static final class FieldIterator implements Iterator<Field> {
    private int fieldIndex;
    private final Field[] fields;

    FieldIterator(Field[] fields) {
      this.fields = fields;
    }

    @Override
    public boolean hasNext() {
      return fieldIndex < fields.length;
    }

    @Override
    public Field next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      return fields[fieldIndex++];
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private static final class FieldImpl implements Field {
    private final long[] data;
    private final int dataPos;

    FieldImpl(long[] data, int dataPos) {
      this.data = data;
      this.dataPos = dataPos;
    }

    @Override
    public int number() {
      return getFieldNumber(data[dataPos]);
    }

    @Override
    public FieldType type() {
      return getFieldType(data[dataPos]);
    }

    private long valueOffset() {
      return data[dataPos + 1];
    }

    @Override
    public int intValue(Object message) {
      if (type().getJavaType() != JavaType.INT) {
        throw new InvalidStateException("Incorrect java type: " + type().getJavaType());
      }
      return UnsafeUtil.getInt(message, valueOffset());
    }

    @Override
    public <E extends Enum<E>> Enum<E> enumValue(Object message, Class<E> clazz) {
      if (type().getJavaType() != JavaType.ENUM) {
        throw new InvalidStateException("Incorrect java type: " + type().getJavaType());
      }
      return clazz.cast(UnsafeUtil.getObject(message, valueOffset()));
    }

    @Override
    public long longValue(Object message) {
      if (type().getJavaType() != JavaType.LONG) {
        throw new InvalidStateException("Incorrect java type: " + type().getJavaType());
      }
      return UnsafeUtil.getLong(message, valueOffset());
    }

    @Override
    public double doubleValue(Object message) {
      if (type().getJavaType() != JavaType.DOUBLE) {
        throw new InvalidStateException("Incorrect java type: " + type().getJavaType());
      }
      return UnsafeUtil.getDouble(message, valueOffset());
    }

    @Override
    public float floatValue(Object message) {
      if (type().getJavaType() != JavaType.FLOAT) {
        throw new InvalidStateException("Incorrect java type: " + type().getJavaType());
      }
      return UnsafeUtil.getFloat(message, valueOffset());
    }

    @Override
    public Object messageValue(Object message) {
      if (type().getJavaType() != JavaType.MESSAGE) {
        throw new InvalidStateException("Incorrect java type: " + type().getJavaType());
      }
      return UnsafeUtil.getObject(message, valueOffset());
    }

    @Override
    public String stringValue(Object message) {
      if (type().getJavaType() != JavaType.STRING) {
        throw new InvalidStateException("Incorrect java type: " + type().getJavaType());
      }
      return (String) UnsafeUtil.getObject(message, valueOffset());
    }

    @Override
    public ByteString bytesValue(Object message) {
      if (type().getJavaType() != JavaType.BYTE_STRING) {
        throw new InvalidStateException("Incorrect java type: " + type().getJavaType());
      }
      return (ByteString) UnsafeUtil.getObject(message, valueOffset());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <L> List<L> values(Object message, Class<? extends List<L>> clazz) {
      if (type().getJavaType() != JavaType.LIST) {
        throw new InvalidStateException("Incorrect java type: " + type().getJavaType());
      }
      // TODO(nathanmittler): check the type parameter before casting.
      return (List<L>) UnsafeUtil.getObject(message, valueOffset());
    }
  }
}
