# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================
# R8 Optimization Control
# ============================================
# Don't optimize and obfuscate DTO classes
-optimizations !class/merging/*,!code/simplification/*,!field/*,!method/*
-dontobfuscate

# ============================================
# Retrofit & OkHttp
# ============================================
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes EnclosingMethod

# Retrofit interfaces
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep API interfaces and their methods
-keep interface com.indybrain.indypos_Android.data.remote.api.** { *; }
-keepclassmembers interface com.indybrain.indypos_Android.data.remote.api.** { *; }

# Keep API request/response DTOs defined in API files
-keep class com.indybrain.indypos_Android.data.remote.api.*$* { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ============================================
# Gson - CRITICAL FOR API CALLS
# ============================================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-dontwarn sun.misc.**

# CRITICAL: Keep ALL DTO classes and prevent field name obfuscation
-keep class com.indybrain.indypos_Android.data.remote.dto.** { *; }
-keep class com.indybrain.indypos_Android.data.remote.api.**$* { *; }
-keep class com.indybrain.indypos_Android.domain.model.** { *; }
-keep class com.indybrain.indypos_Android.data.local.entity.** { *; }

# Prevent obfuscation of field names in these packages
-keepclassmembers class com.indybrain.indypos_Android.data.remote.dto.** {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.indybrain.indypos_Android.data.remote.api.**$* {
    <fields>;
    <init>(...);
}

# Keep Gson specific classes
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep interface com.google.gson.** { *; }

# Keep generic signature of Gson classes
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep data classes used with Gson
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Application classes that will be serialized/deserialized over Gson
-keep class * implements java.io.Serializable { *; }

# Retain declared checked exceptions for proper API responses
-keepattributes Exceptions

# ============================================
# Kotlin Coroutines
# ============================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# ============================================
# Hilt
# ============================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# ============================================
# Room
# ============================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** getInstance(...);
}

# ============================================
# Keep data classes used in the app
# ============================================
-keep class com.indybrain.indypos_Android.data.local.entity.** { *; }

# Keep all data classes with @SerializedName
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================
# Keep Parcelable implementations
# ============================================
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Apache POI - Ignore missing classes that are not available on Android
# Java AWT classes (not available on Android)
-dontwarn java.awt.**
-dontwarn java.awt.color.**
-dontwarn java.awt.geom.**
-dontwarn java.awt.image.**

# Java Beans classes (not available on Android)
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IndexedPropertyDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor

# Apache Log and Log4j classes (optional dependencies)
-dontwarn org.apache.log.Hierarchy
-dontwarn org.apache.log.Logger
-dontwarn org.apache.log4j.Level
-dontwarn org.apache.log4j.Logger
-dontwarn org.apache.log4j.Priority

# FindBugs annotations (optional dependency)
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings

# ETSI XML signatures (optional dependency for digital signatures)
-dontwarn org.etsi.uri.x01903.v13.CertifiedRolesListType
-dontwarn org.etsi.uri.x01903.v13.CounterSignatureType
-dontwarn org.etsi.uri.x01903.v13.DocumentationReferencesType
-dontwarn org.etsi.uri.x01903.v13.IncludeType
-dontwarn org.etsi.uri.x01903.v13.OtherCertStatusRefsType
-dontwarn org.etsi.uri.x01903.v13.OtherCertStatusValuesType
-dontwarn org.etsi.uri.x01903.v13.QualifierType
-dontwarn org.etsi.uri.x01903.v13.ReferenceInfoType
-dontwarn org.etsi.uri.x01903.v13.SignatureProductionPlaceType
-dontwarn org.etsi.uri.x01903.v13.UnsignedDataObjectPropertiesType

# W3C XML digital signature classes (optional dependency)
-dontwarn org.w3.x2000.x09.xmldsig.KeyInfoType
-dontwarn org.w3.x2000.x09.xmldsig.SignatureMethodType
-dontwarn org.w3.x2000.x09.xmldsig.TransformsType

# XZ compression classes (optional dependency for POI)
-dontwarn org.tukaani.xz.ARMOptions
-dontwarn org.tukaani.xz.ARMThumbOptions
-dontwarn org.tukaani.xz.FilterOptions
-dontwarn org.tukaani.xz.IA64Options
-dontwarn org.tukaani.xz.LZMA2Options
-dontwarn org.tukaani.xz.PowerPCOptions
-dontwarn org.tukaani.xz.SPARCOptions
-dontwarn org.tukaani.xz.X86Options

# Saxon XPath classes (optional dependency for POI)
-dontwarn net.sf.saxon.**

# Batik SVG classes (optional dependency for POI)
-dontwarn org.apache.batik.**

# OSGi classes (optional dependency for Log4j)
-dontwarn org.osgi.**

# BND annotations (optional dependency for Log4j)
-dontwarn aQute.bnd.annotation.**

# javax.xml.stream classes (optional dependency for XMLBeans)
-dontwarn javax.xml.stream.**

# Keep Apache POI classes that we actually use
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }

# Keep classes used by POI
-keep class org.openxmlformats.schemas.** { *; }
-keep class com.microsoft.schemas.** { *; }

# JavaParser classes (optional dependency for XMLBeans code generation)
-dontwarn com.github.javaparser.**

# Microsoft Office schemas (optional features)
-dontwarn com.microsoft.schemas.office.office.**
-dontwarn com.microsoft.schemas.office.powerpoint.**
-dontwarn com.microsoft.schemas.office.visio.**
-dontwarn com.microsoft.schemas.office.word.**
-dontwarn com.microsoft.schemas.office.x2006.digsig.**
-dontwarn com.microsoft.schemas.vml.**

# javax.imageio classes (not available on Android)
-dontwarn javax.imageio.**

# javax.swing classes (not available on Android)
-dontwarn javax.swing.**

# javax.xml.crypto classes (optional dependency for digital signatures)
-dontwarn javax.xml.crypto.**
-dontwarn javax.xml.crypto.dsig.**
-dontwarn javax.xml.crypto.dsig.dom.**
-dontwarn javax.xml.crypto.dsig.keyinfo.**
-dontwarn javax.xml.crypto.dsig.spec.**

# Apache XML Security classes (optional dependency)
-dontwarn org.apache.xml.security.**
-dontwarn org.apache.jcp.xml.dsig.internal.dom.**

# Apache Maven classes (build-time only, not needed at runtime)
-dontwarn org.apache.maven.**
-dontwarn org.apache.maven.plugin.**
-dontwarn org.apache.maven.plugins.annotations.**

# Apache Ant classes (build-time only, not needed at runtime)
-dontwarn org.apache.tools.ant.**

# PDFBox classes (optional dependency for PowerPoint to PDF conversion)
-dontwarn org.apache.pdfbox.**

# PDFBox Graphics2D classes (optional dependency)
-dontwarn de.rototor.pdfbox.graphics2d.**

# W3C DOM classes (optional features)
-dontwarn org.w3c.dom.events.**
-dontwarn org.w3c.dom.svg.**
-dontwarn org.w3c.dom.traversal.**

# IETF GSS classes (optional dependency)
-dontwarn org.ietf.jgss.**

# XML Resolver classes (optional dependency)
-dontwarn com.sun.org.apache.xml.internal.resolver.**

# OpenXMLFormats schema classes (many are optional features)
# These are already kept above, but we ignore warnings for missing optional ones
-dontwarn org.openxmlformats.schemas.drawingml.x2006.chart.**
-dontwarn org.openxmlformats.schemas.drawingml.x2006.diagram.**
-dontwarn org.openxmlformats.schemas.drawingml.x2006.main.**
-dontwarn org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.**
-dontwarn org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.**
-dontwarn org.openxmlformats.schemas.officeDocument.x2006.docPropsVTypes.**
-dontwarn org.openxmlformats.schemas.officeDocument.x2006.math.**
-dontwarn org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.**
-dontwarn org.openxmlformats.schemas.presentationml.x2006.main.**
-dontwarn org.openxmlformats.schemas.schemaLibrary.x2006.main.**
-dontwarn org.openxmlformats.schemas.spreadsheetml.x2006.main.**
-dontwarn org.openxmlformats.schemas.wordprocessingml.x2006.main.**