package com.google.apps.tiktok.protobuf.experimental.schema.asm.android;

import com.google.apps.tiktok.protobuf.experimental.InternalApi;
import com.google.apps.tiktok.protobuf.experimental.schema.asm.ClassLoadingStrategy;
import com.google.apps.tiktok.protobuf.experimental.util.RandomString;

import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.file.DexFile;

import dalvik.system.DexClassLoader;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

/**
 * A strategy for loading generated schema classes on android. This creates a dex file on disk
 * containing the bytecode and then loads the file via a new classloader.
 */
@InternalApi
public final class AndroidClassLoadingStrategy implements ClassLoadingStrategy {
  private static final int DEX_COMPATIBLE_API_VERSION = 13;
  private static final String JAR_FILE_EXTENSION = ".jar";
  private static final String CLASS_FILE_EXTENSION = ".class";

  /**
   * The name of the dex file that the {@link DexClassLoader} expects to find inside
   * of a jar file that is handed to it as its argument.
   */
  private static final String DEX_CLASS_FILE = "classes.dex";

  private final File privateDirectory;

  private final DexOptions dexFileOptions = new DexOptions();
  private final CfOptions dexCompilerOptions = new CfOptions();

  /**
   * A generator for random string values.
   */
  private final RandomString randomString;

  private final ClassLoader parentClassLoader;

  /**
   * @param privateDirectory A directory that is <b>not shared with other applications</b> to be
   * used for storing generated classes and their processed forms.
   */
  public AndroidClassLoadingStrategy(ClassLoader parentClassLoader, File privateDirectory) {
    dexFileOptions.targetApiLevel = DEX_COMPATIBLE_API_VERSION;
    this.privateDirectory = privateDirectory;
    this.parentClassLoader = parentClassLoader;
    randomString = new RandomString();
  }

  @Override
  public Class<?> loadSchemaClass(Class<?> messageClass, String name, byte[] binaryRepresentation) {
    DexFile dexFile = newDexFile(name, binaryRepresentation);
    File jarFile = newJarFile();
    try {
      writeDexToJar(dexFile, jarFile);
      return loadClassFromJar(name, jarFile);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot write to zip file " + jarFile, e);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Unable to find loaded class " + name, e);
    } finally {
      if (!jarFile.delete()) {
        Logger.getAnonymousLogger().warning("Could not delete " + jarFile);
      }
    }
  }

  @Override
  public boolean isPackagePrivateAccessSupported() {
    return false;
  }

  private File newJarFile() {
    File jarFile = new File(privateDirectory, randomString.nextString() + JAR_FILE_EXTENSION);
    try {
      if (!jarFile.createNewFile()) {
        throw new IllegalStateException("Cannot create " + jarFile);
      }
      return jarFile;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private DexFile newDexFile(String name, byte[] binaryRepresentation) {
    DexFile dexFile = new DexFile(dexFileOptions);
    dexFile.add(
        CfTranslator.translate(
            name.replace('.', '/') + CLASS_FILE_EXTENSION,
            binaryRepresentation,
            dexCompilerOptions,
            dexFileOptions));
    return dexFile;
  }

  private void writeDexToJar(DexFile dexFile, File jarFile) throws IOException {
    JarOutputStream zipOutputStream = new JarOutputStream(new FileOutputStream(jarFile));
    try {
      zipOutputStream.putNextEntry(new JarEntry(DEX_CLASS_FILE));
      dexFile.writeTo(zipOutputStream, null, false);
      zipOutputStream.closeEntry();
    } finally {
      close(zipOutputStream);
    }
  }

  private Class<?> loadClassFromJar(String name, File jarFile) throws ClassNotFoundException {
    ClassLoader dexClassLoader =
        new DexClassLoader(
            jarFile.getAbsolutePath(), privateDirectory.getAbsolutePath(), null, parentClassLoader);
    return dexClassLoader.loadClass(name);
  }

  private static void close(Closeable obj) {
    try {
      obj.close();
    } catch (IOException e) {
      // Absorb.
    }
  }
}
