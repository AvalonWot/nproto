package com.google.protobuf.experimental.schema;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IF_ICMPEQ;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_6;
import static org.objectweb.asm.Type.getInternalName;

import com.google.protobuf.experimental.JavaType;
import com.google.protobuf.experimental.Writer;
import com.google.protobuf.experimental.descriptor.AnnotationBeanDescriptorFactory;
import com.google.protobuf.experimental.descriptor.BeanDescriptorFactory;
import com.google.protobuf.experimental.descriptor.PropertyDescriptor;
import com.google.protobuf.experimental.descriptor.PropertyType;
import com.google.protobuf.experimental.util.SchemaUtil;
import com.google.protobuf.experimental.util.UnsafeUtil;
import com.google.protobuf.experimental.Internal;
import com.google.protobuf.experimental.Reader;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Modifier;
import java.util.List;

@Internal
public final class AsmSchemaFactory implements SchemaFactory {
  private static final String SCHEMA_INTERNAL_NAME = getInternalName(Schema.class);
  private static final String WRITER_INTERNAL_NAME = getInternalName(Writer.class);
  private static final String READER_INTERNAL_NAME = getInternalName(Reader.class);
  private static final String SCHEMAUTIL_INTERNAL_NAME = getInternalName(SchemaUtil.class);
  private static final Type ENUM_TYPE = Type.getType(Enum.class);
  private static final String WRITE_TO_DESCRIPTOR = String.format("(Ljava/lang/Object;L%s;)V",
          WRITER_INTERNAL_NAME);
  private static final String MERGE_FROM_DESCRIPTOR = String.format("(Ljava/lang/Object;L%s;)V",
          READER_INTERNAL_NAME);
  private static final int MESSAGE_INDEX = 1;
  private static final int WRITER_INDEX = 2;
  private static final int READER_INDEX = 2;
  private static final int FIELD_NUMBER_INDEX = 3;
  private static final FieldProcessor[] FIELD_PROCESSORS;

  static {
    PropertyType[] propertyTypes = PropertyType.values();
    FIELD_PROCESSORS = new FieldProcessor[propertyTypes.length];
    for (int i = 0; i < propertyTypes.length; ++i) {
      FIELD_PROCESSORS[i] = new FieldProcessor(propertyTypes[i]);
    }
  }

  private final ClassLoadingStrategy classLoadingStrategy;
  private final BeanDescriptorFactory beanDescriptorFactory;

  public AsmSchemaFactory() {
    this(AnnotationBeanDescriptorFactory.getInstance());
  }

  public AsmSchemaFactory(BeanDescriptorFactory beanDescriptorFactory) {
    this(ForwardingClassLoadingStrategy.getInstance(), beanDescriptorFactory);
  }

  public AsmSchemaFactory(ClassLoadingStrategy classLoadingStrategy, BeanDescriptorFactory beanDescriptorFactory) {
    if (classLoadingStrategy == null) {
      throw new NullPointerException("classLoadingStrategy");
    }
    if (beanDescriptorFactory == null) {
      throw new NullPointerException("beanDescriptorFactory");
    }
    this.classLoadingStrategy = classLoadingStrategy;
    this.beanDescriptorFactory = beanDescriptorFactory;
  }

  @Override
  public <T> Schema<T> createSchema(Class<T> messageType) {
    try {
      @SuppressWarnings("unchecked")
      Class<Schema<T>> newClass = (Class<Schema<T>>)
              classLoadingStrategy.loadClass(getSchemaClassName(messageType), createSchemaClass(messageType));
      return newClass.newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public <T> byte[] createSchemaClass(Class<T> messageType) {
    if (messageType.isInterface() || Modifier.isAbstract(messageType.getModifiers())) {
      throw new RuntimeException(
              "The root object can neither be an abstract "
                      + "class nor interface: \"" + messageType.getName());
    }

    ClassWriter cv = new ClassWriter(0);
    //ClassVisitor cv = new CheckClassAdapter(writer);
    final String className = getSchemaClassName(messageType).replace('.', '/');
    cv.visit(V1_6, ACC_PUBLIC + ACC_FINAL, className, null, "java/lang/Object",
            new String[]{SCHEMA_INTERNAL_NAME});
    generateConstructor(cv);

    List<PropertyDescriptor> fields = beanDescriptorFactory.descriptorFor(messageType).getPropertyDescriptors();
    WriteToGenerator writeTo = new WriteToGenerator(cv);
    MergeFromGenerator mergeFrom = new MergeFromGenerator(cv, fields);
    int lastFieldNumber = Integer.MAX_VALUE;
    for (int i = 0; i < fields.size(); ++i) {
      PropertyDescriptor f = fields.get(i);
      if (lastFieldNumber == f.fieldNumber) {
        // Disallow duplicate field numbers.
        throw new RuntimeException("Duplicate field number: " + f.fieldNumber);
      }
      lastFieldNumber = f.fieldNumber;

      long offset = UnsafeUtil.objectFieldOffset(f.field);
      writeTo.addField(f, offset);
      mergeFrom.addField(f, i, offset);
    }
    writeTo.end();
    mergeFrom.end();

    // Complete the generation of the class and return a new instance.
    cv.visitEnd();
    return cv.toByteArray();
  }

  private static void generateConstructor(ClassVisitor cv) {
    MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    mv.visitInsn(RETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private static final class WriteToGenerator {
    private final MethodVisitor mv;

    WriteToGenerator(ClassVisitor cv) {
      mv = cv.visitMethod(ACC_PUBLIC, "writeTo", WRITE_TO_DESCRIPTOR, null, null);
      mv.visitCode();
    }

    void addField(PropertyDescriptor field, long offset) {
      FIELD_PROCESSORS[field.type.ordinal()].write(mv, field.fieldNumber, offset);
    }

    void end() {
      mv.visitInsn(RETURN);
      mv.visitMaxs(7, 3);
      mv.visitEnd();
    }
  }

  private static final class MergeFromGenerator {
    private final MethodVisitor mv;
    private final Label startLabel;
    private final Label endLabel;
    private final Label defaultLabel;
    private final Label[] labels;
    private final boolean tableSwitch;
    private final int lo;

    MergeFromGenerator(ClassVisitor cv, List<PropertyDescriptor> fields) {
      mv = cv.visitMethod(ACC_PUBLIC, "mergeFrom", MERGE_FROM_DESCRIPTOR, null, null);
      mv.visitCode();

      // Create the main labels and visit the start.
      startLabel = new Label();
      endLabel = new Label();
      defaultLabel = new Label();
      visitLabel(startLabel);

      // Get the field number form the reader.
      callReader(mv, "fieldNumber", "()I");

      // Make a copy of the field number and store to a local variable. The first check is against
      // MAXINT since looking for that value in the switch statement would mean that we couldn't use a
      // tableswitch (rather than lookupswitch).
      mv.visitInsn(DUP);
      mv.visitVarInsn(ISTORE, FIELD_NUMBER_INDEX);
      mv.visitLdcInsn(Reader.READ_DONE);
      mv.visitJumpInsn(IF_ICMPEQ, endLabel);

      // Load the field number again for the switch.
      mv.visitVarInsn(ILOAD, FIELD_NUMBER_INDEX);
      tableSwitch = SchemaUtil.shouldUseTableSwitch(fields);
      final int numFields = fields.size();
      if (tableSwitch) {
        // Tableswitch...

        // Determine the number of labels (i.e. cases).
        lo = fields.get(0).fieldNumber;
        int hi = fields.get(numFields - 1).fieldNumber;
        int numLabels = (hi - lo) + 1;

        // Create the labels
        labels = new Label[numLabels];
        for (int labelIndex = 0, fieldIndex = 0; fieldIndex < numFields; ++fieldIndex) {
          while (labelIndex < fields.get(fieldIndex).fieldNumber - lo) {
            // Unused entries in the table drop down to the default case.
            labels[labelIndex++] = defaultLabel;
          }
          labels[labelIndex++] = new Label();
        }

        // Create the switch statement.
        mv.visitTableSwitchInsn(lo, hi, defaultLabel, labels);
      } else {
        // Lookupswitch...

        // Create the keys and labels.
        lo = -1;
        int[] keys = new int[numFields];
        labels = new Label[numFields];
        for (int i = 0; i < numFields; ++i) {
          keys[i] = fields.get(i).fieldNumber;
          Label label = new Label();
          labels[i] = label;
        }

        // Create the switch statement.
        mv.visitLookupSwitchInsn(defaultLabel, keys, labels);
      }
    }

    void addField(PropertyDescriptor field, int fieldIndex, long offset) {
      if (tableSwitch) {
        addTableSwitchCase(field, offset);
      } else {
        addLookupSwitchCase(field, fieldIndex, offset);
      }
    }

    void addTableSwitchCase(PropertyDescriptor field, long offset) {
      // Tableswitch: Label index is the field number.
      visitLabel(labels[field.fieldNumber - lo]);
      FIELD_PROCESSORS[field.type.ordinal()].read(mv, offset);
      mv.visitJumpInsn(GOTO, startLabel);
    }

    void addLookupSwitchCase(PropertyDescriptor field, int fieldIndex, long offset) {
      // Lookupswitch: Label index is field index.
      visitLabel(labels[fieldIndex]);
      FIELD_PROCESSORS[field.type.ordinal()].read(mv, offset);
      mv.visitJumpInsn(GOTO, startLabel);
    }

    void end() {
      // Default case: skip the unknown field and check for done.
      visitLabel(defaultLabel);
      callReader(mv, "skipField", "()Z");
      mv.visitJumpInsn(IFNE, startLabel);

      visitLabel(endLabel);
      mv.visitInsn(RETURN);
      mv.visitMaxs(4, 4);
      mv.visitEnd();
    }

    private void visitLabel(Label label) {
      mv.visitLabel(label);
      mv.visitFrame(F_SAME, 0, null, 0, null);
    }
  }

  private static void callReader(MethodVisitor mv, String methodName, String methodDescriptor) {
    mv.visitVarInsn(ALOAD, READER_INDEX);
    mv.visitMethodInsn(INVOKEINTERFACE, READER_INTERNAL_NAME, methodName, methodDescriptor, true);
  }

  private static String getSchemaClassName(Class<?> messageType) {
    return messageType.getName() + "Schema";
  }

  private static final class FieldProcessor {
    private final String writeMethodName;
    private final String readMethodName;
    private final WriteType writeType;

    private enum WriteType {
      STANDARD,
      ENUM,
      ENUM_LIST,
      LIST
    }

    FieldProcessor(PropertyType propertyType) {
      JavaType jtype = propertyType.getJavaType();
      WriteType writeType = (jtype == JavaType.LIST) ?
              WriteType.LIST : (jtype == JavaType.ENUM) ? WriteType.ENUM : WriteType.STANDARD;
      switch (propertyType) {
        case DOUBLE:
          writeMethodName = "unsafeWriteDouble";
          readMethodName = "unsafeReadDouble";
          break;
        case FLOAT:
          writeMethodName = "unsafeWriteFloat";
          readMethodName = "unsafeReadFloat";
          break;
        case INT64:
          writeMethodName = "unsafeWriteInt64";
          readMethodName = "unsafeReadInt64";
          break;
        case UINT64:
          writeMethodName = "unsafeWriteUInt64";
          readMethodName = "unsafeReadUInt64";
          break;
        case INT32:
          writeMethodName = "unsafeWriteInt32";
          readMethodName = "unsafeReadInt32";
          break;
        case FIXED64:
          writeMethodName = "unsafeWriteFixed64";
          readMethodName = "unsafeReadFixed64";
          break;
        case FIXED32:
          writeMethodName = "unsafeWriteFixed32";
          readMethodName = "unsafeReadFixed32";
          break;
        case BOOL:
          writeMethodName = "unsafeWriteBool";
          readMethodName = "unsafeReadBool";
          break;
        case STRING:
          writeMethodName = "unsafeWriteString";
          readMethodName = "unsafeReadString";
          break;
        case MESSAGE:
          writeMethodName = "unsafeWriteMessage";
          readMethodName = "unsafeReadMessage";
          break;
        case BYTES:
          writeMethodName = "unsafeWriteBytes";
          readMethodName = "unsafeReadBytes";
          break;
        case UINT32:
          writeMethodName = "unsafeWriteUInt32";
          readMethodName = "unsafeReadUInt32";
          break;
        case ENUM:
          writeMethodName = "unsafeWriteEnum";
          readMethodName = "unsafeReadEnum";
          break;
        case SFIXED32:
          writeMethodName = "unsafeWriteSFixed32";
          readMethodName = "unsafeReadSFixed32";
          break;
        case SFIXED64:
          writeMethodName = "unsafeWriteSFixed64";
          readMethodName = "unsafeReadSFixed64";
          break;
        case SINT32:
          writeMethodName = "unsafeWriteSInt32";
          readMethodName = "unsafeReadSInt32";
          break;
        case SINT64:
          writeMethodName = "unsafeWriteSInt64";
          readMethodName = "unsafeReadSInt64";
          break;
        case DOUBLE_LIST:
          writeMethodName = "unsafeWriteDoubleList";
          readMethodName = "unsafeReadDoubleList";
          break;
        case FLOAT_LIST:
          writeMethodName = "unsafeWriteFloatList";
          readMethodName = "unsafeReadFloatList";
          break;
        case INT64_LIST:
          writeMethodName = "unsafeWriteInt64List";
          readMethodName = "unsafeReadInt64List";
          break;
        case UINT64_LIST:
          writeMethodName = "unsafeWriteUInt64List";
          readMethodName = "unsafeReadUInt64List";
          break;
        case INT32_LIST:
          writeMethodName = "unsafeWriteInt32List";
          readMethodName = "unsafeReadInt32List";
          break;
        case FIXED64_LIST:
          writeMethodName = "unsafeWriteFixed64List";
          readMethodName = "unsafeReadFixed64List";
          break;
        case FIXED32_LIST:
          writeMethodName = "unsafeWriteFixed32List";
          readMethodName = "unsafeReadFixed32List";
          break;
        case BOOL_LIST:
          writeMethodName = "unsafeWriteBoolList";
          readMethodName = "unsafeReadBoolList";
          break;
        case STRING_LIST:
          writeMethodName = "unsafeWriteStringList";
          readMethodName = "unsafeReadStringList";
          writeType = WriteType.STANDARD;
          break;
        case MESSAGE_LIST:
          writeMethodName = "unsafeWriteMessageList";
          readMethodName = "unsafeReadMessageList";
          writeType = WriteType.STANDARD;
          break;
        case BYTES_LIST:
          writeMethodName = "unsafeWriteBytesList";
          readMethodName = "unsafeReadBytesList";
          writeType = WriteType.STANDARD;
          break;
        case UINT32_LIST:
          writeMethodName = "unsafeWriteUInt32List";
          readMethodName = "unsafeReadUInt32List";
          break;
        case ENUM_LIST:
          writeMethodName = "unsafeWriteEnumList";
          readMethodName = "unsafeReadEnumList";
          writeType = WriteType.ENUM_LIST;
          break;
        case SFIXED32_LIST:
          writeMethodName = "unsafeWriteSFixed32List";
          readMethodName = "unsafeReadSFixed32List";
          break;
        case SFIXED64_LIST:
          writeMethodName = "unsafeWriteSFixed64List";
          readMethodName = "unsafeReadSFixed64List";
          break;
        case SINT32_LIST:
          writeMethodName = "unsafeWriteSInt32List";
          readMethodName = "unsafeReadSInt32List";
          break;
        case SINT64_LIST:
          writeMethodName = "unsafeWriteSInt64List";
          readMethodName = "unsafeReadSInt64List";
          break;
        default:
          throw new IllegalArgumentException("Unsupported FieldType: " + propertyType);
      }
      this.writeType = writeType;
    }

    void write(MethodVisitor mv, int fieldNumber, long offset) {
      switch (writeType) {
        case STANDARD:
          unsafeWrite(mv, fieldNumber, offset);
          break;
        case ENUM:
          unsafeWriteEnum(mv, fieldNumber, offset);
          break;
        case ENUM_LIST:
          unsafeWriteEnumList(mv, fieldNumber, offset);
          break;
        case LIST:
          unsafeWriteList(mv, fieldNumber, offset);
          break;
      }
    }

    void read(MethodVisitor mv, long offset) {
      mv.visitVarInsn(ALOAD, MESSAGE_INDEX);
      mv.visitLdcInsn(offset);
      mv.visitVarInsn(ALOAD, READER_INDEX);
      mv.visitMethodInsn(INVOKESTATIC, SCHEMAUTIL_INTERNAL_NAME, readMethodName,
              "(Ljava/lang/Object;JLcom/google/protobuf/experimental/Reader;)V", false);
    }

    private void unsafeWriteEnum(MethodVisitor mv, int fieldNumber, long offset) {
      mv.visitLdcInsn(fieldNumber);
      mv.visitVarInsn(ALOAD, MESSAGE_INDEX);
      mv.visitLdcInsn(offset);
      mv.visitVarInsn(ALOAD, WRITER_INDEX);
      mv.visitLdcInsn(ENUM_TYPE);
      mv.visitMethodInsn(INVOKESTATIC, SCHEMAUTIL_INTERNAL_NAME, writeMethodName,
              "(ILjava/lang/Object;JLcom/google/protobuf/experimental/Writer;Ljava/lang/Class;)V", false);
    }

    private void unsafeWrite(MethodVisitor mv, int fieldNumber, long offset) {
      mv.visitLdcInsn(fieldNumber);
      mv.visitVarInsn(ALOAD, MESSAGE_INDEX);
      mv.visitLdcInsn(offset);
      mv.visitVarInsn(ALOAD, WRITER_INDEX);
      mv.visitMethodInsn(INVOKESTATIC, SCHEMAUTIL_INTERNAL_NAME, writeMethodName,
              "(ILjava/lang/Object;JLcom/google/protobuf/experimental/Writer;)V", false);
    }

    private void unsafeWriteList(MethodVisitor mv, int fieldNumber, long offset) {
      mv.visitLdcInsn(fieldNumber);
      mv.visitVarInsn(ALOAD, MESSAGE_INDEX);
      mv.visitLdcInsn(offset);
      mv.visitVarInsn(ALOAD, WRITER_INDEX);
      mv.visitMethodInsn(INVOKESTATIC, SCHEMAUTIL_INTERNAL_NAME, writeMethodName,
              "(ILjava/lang/Object;JLcom/google/protobuf/experimental/Writer;)V", false);
    }

    private void unsafeWriteEnumList(MethodVisitor mv, int fieldNumber, long offset) {
      mv.visitLdcInsn(fieldNumber);
      mv.visitVarInsn(ALOAD, MESSAGE_INDEX);
      mv.visitLdcInsn(offset);
      mv.visitVarInsn(ALOAD, WRITER_INDEX);
      mv.visitLdcInsn(ENUM_TYPE);
      mv.visitMethodInsn(INVOKESTATIC, SCHEMAUTIL_INTERNAL_NAME, writeMethodName,
              "(ILjava/lang/Object;JLcom/google/protobuf/experimental/Writer;Ljava/lang/Class;)V", false);
    }
  }
}