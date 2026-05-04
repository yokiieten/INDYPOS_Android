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

# Keep attributes needed for Apache POI reflection and enum access
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Enum

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================
# R8 Optimization Control
# ============================================
# Don't optimize and obfuscate DTO classes
-optimizations !class/merging/*,!code/simplification/*,!field/*,!method/*
# Note: -dontobfuscate is too broad, we use specific keep rules instead
# -dontobfuscate

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
-keepattributes InnerClasses
-keepattributes Enum

# Retrofit interfaces
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Retrofit suspend function support (CRITICAL for API calls)
-keepclassmembers class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    <methods>;
}
-keep class kotlin.coroutines.** { *; }
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    <methods>;
}

# Keep Retrofit service interfaces and suspend functions
-keep interface com.indybrain.indypos_Android.data.remote.api.** { *; }
-keepclassmembers interface com.indybrain.indypos_Android.data.remote.api.** { *; }

# Keep API request/response DTOs defined in API files
# Keep inner classes (nested classes)
-keep class com.indybrain.indypos_Android.data.remote.api.*$* { *; }
# Keep top-level DTO classes in API files (CRITICAL - these are not inner classes!)
-keep class com.indybrain.indypos_Android.data.remote.api.*RequestDto { *; }
-keep class com.indybrain.indypos_Android.data.remote.api.*ResponseDto { *; }
-keep class com.indybrain.indypos_Android.data.remote.api.*Dto { *; }

# Keep Retrofit call adapters for suspend functions
-keep class retrofit2.KotlinExtensions { *; }
-keep class retrofit2.KotlinExtensions$* { *; }

# Keep Retrofit converters
-keep class retrofit2.converter.gson.** { *; }
-keep class retrofit2.Converter$* { *; }
-keep class retrofit2.CallAdapter$* { *; }

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

# Kotlin + Gson + R8: Gson reflects into data classes; keep metadata used for constructor/serialization.
-keep class kotlin.Metadata { *; }

# AndroidX @Keep — entry points for DTOs annotated in Kotlin (complements Gson's gson.pro).
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}

# CRITICAL: Keep ALL DTO classes and prevent field name obfuscation
-keep class com.indybrain.indypos_Android.data.remote.dto.** { *; }
-keep class com.indybrain.indypos_Android.data.remote.api.**$* { *; }
# Keep top-level DTO classes in API files (CRITICAL - these are not inner classes!)
-keep class com.indybrain.indypos_Android.data.remote.api.*RequestDto { *; }
-keep class com.indybrain.indypos_Android.data.remote.api.*ResponseDto { *; }
-keep class com.indybrain.indypos_Android.data.remote.api.*Dto { *; }
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
# Prevent obfuscation of field names in top-level DTO classes in API files
-keepclassmembers class com.indybrain.indypos_Android.data.remote.api.*RequestDto {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.indybrain.indypos_Android.data.remote.api.*ResponseDto {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.indybrain.indypos_Android.data.remote.api.*Dto {
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

# Keep coroutines for Retrofit suspend functions
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

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

# CRITICAL: Keep all data classes in API package (for Gson serialization)
# This ensures all DTO classes in API files are preserved with their fields
-keepclassmembers class com.indybrain.indypos_Android.data.remote.api.** {
    <fields>;
    <init>(...);
}

# ============================================
# Error Response DTOs (CRITICAL for error handling)
# ============================================
# Keep error response classes used for parsing API errors
-keep class com.indybrain.indypos_Android.data.repository.*ErrorResponse { *; }
-keep class com.indybrain.indypos_Android.data.repository.*Error { *; }
-keepclassmembers class com.indybrain.indypos_Android.data.repository.*ErrorResponse {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.indybrain.indypos_Android.data.repository.*Error {
    <fields>;
    <init>(...);
}

# ============================================
# Generic API Response Wrapper (CRITICAL)
# ============================================
# Keep ApiResponseDto and all its generic type parameters
-keep class com.indybrain.indypos_Android.data.remote.dto.ApiResponseDto { *; }
-keepclassmembers class com.indybrain.indypos_Android.data.remote.dto.ApiResponseDto {
    <fields>;
    <init>(...);
}

# Keep all DTO classes that might be used as generic type parameters
-keep class com.indybrain.indypos_Android.data.remote.dto.** { *; }

# CRITICAL: Keep all data classes in API package (for Gson serialization)
-keepclassmembers class com.indybrain.indypos_Android.data.remote.api.** {
    <fields>;
    <init>(...);
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
# Use wildcard to cover all XZ classes
-dontwarn org.tukaani.xz.**

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
# CRITICAL: Do NOT allow obfuscation for POI - it uses reflection and dynamic class loading
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }

# Keep names of POI classes (prevent name obfuscation)
-keepnames class org.apache.poi.** { *; }
-keepnames class org.apache.xmlbeans.** { *; }

# Keep anonymous classes and lambda expressions used by POI
-keep class org.apache.poi.**$$* { *; }
-keepclassmembers class org.apache.poi.** {
    ** lambda$*(...);
    ** $*(...);
}

# Keep POI inner classes and nested classes (CRITICAL for Excel export)
-keep class org.apache.poi.**$* { *; }

# Keep POI enums (CRITICAL - IndexedColors, FillPatternType, etc.)
-keep enum org.apache.poi.ss.usermodel.IndexedColors {
    <fields>;
    <methods>;
    public static ** valueOf(...);
    public static **[] values();
}
-keep enum org.apache.poi.ss.usermodel.FillPatternType {
    <fields>;
    <methods>;
    public static ** valueOf(...);
    public static **[] values();
}
-keep enum org.apache.poi.ss.usermodel.** {
    <fields>;
    <methods>;
    public static ** valueOf(...);
    public static **[] values();
}

# Keep POI interfaces and their implementations
-keep interface org.apache.poi.ss.usermodel.** { *; }
-keep class * implements org.apache.poi.ss.usermodel.** { *; }

# Keep POI XSSF classes (Excel XLSX format) - CRITICAL
-keep class org.apache.poi.xssf.** { *; }
-keep class org.apache.poi.xssf.usermodel.** { *; }
-keepclassmembers class org.apache.poi.xssf.** { *; }

# Keep XSSFWorkbook specifically (used in ExportService)
-keep class org.apache.poi.xssf.usermodel.XSSFWorkbook {
    <init>();
    <fields>;
    <methods>;
}

# Keep XMLBeans classes and their generated code
-keep class org.apache.xmlbeans.** { *; }
-keepclassmembers class org.apache.xmlbeans.** { *; }

# Keep classes used by POI
-keep class org.openxmlformats.schemas.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keepnames class org.openxmlformats.schemas.** { *; }
-keepnames class com.microsoft.schemas.** { *; }

# Keep POI serialization/deserialization classes (only OOXML, not OLE2)
-keep class org.apache.poi.ooxml.** { *; }
-keepclassmembers class org.apache.poi.ooxml.** { *; }
-keepnames class org.apache.poi.ooxml.** { *; }

# Keep POI openxml4j classes (used by OOXML)
-keep class org.apache.poi.openxml4j.** { *; }
-keepclassmembers class org.apache.poi.openxml4j.** { *; }
-keepnames class org.apache.poi.openxml4j.** { *; }

# Keep POI factory classes and reflection-based class loading
-keep class org.apache.poi.ss.usermodel.WorkbookFactory { *; }
-keep enum org.apache.poi.ss.usermodel.CellType { *; }
-keepclassmembers class org.apache.poi.** {
    public static ** valueOf(...);
    public static **[] values();
}

# Keep POI constructors and methods that may be called via reflection
-keepclassmembers class org.apache.poi.ss.usermodel.** {
    <init>(...);
    <methods>;
    <fields>;
}

# Keep all POI constructors (may be called via reflection)
-keepclassmembers class org.apache.poi.** {
    <init>(...);
}

# Keep POI classes that implement Serializable (for serialization)
-keep class org.apache.poi.** implements java.io.Serializable { *; }

# Keep POI methods used in ExportService
-keepclassmembers class org.apache.poi.xssf.usermodel.XSSFWorkbook {
    public <init>();
    public org.apache.poi.ss.usermodel.Sheet createSheet(java.lang.String);
    public void write(java.io.OutputStream);
    public void close();
}

-keepclassmembers class org.apache.poi.ss.usermodel.Sheet {
    public org.apache.poi.ss.usermodel.Row createRow(int);
    public org.apache.poi.ss.usermodel.Workbook getWorkbook();
}

-keepclassmembers class org.apache.poi.ss.usermodel.Workbook {
    public org.apache.poi.ss.usermodel.CellStyle createCellStyle();
    public org.apache.poi.ss.usermodel.Font createFont();
}

-keepclassmembers class org.apache.poi.ss.usermodel.Row {
    public org.apache.poi.ss.usermodel.Cell createCell(int);
}

-keepclassmembers class org.apache.poi.ss.usermodel.Cell {
    public void setCellValue(java.lang.String);
    public void setCellStyle(org.apache.poi.ss.usermodel.CellStyle);
}

-keepclassmembers class org.apache.poi.ss.usermodel.CellStyle {
    public void setFillForegroundColor(short);
    public void setFillPattern(org.apache.poi.ss.usermodel.FillPatternType);
    public void setFont(org.apache.poi.ss.usermodel.Font);
}

-keepclassmembers class org.apache.poi.ss.usermodel.Font {
    public void setBold(boolean);
}

# Keep IndexedColors enum values (used in ExportService.createExcelSheet)
-keepclassmembers enum org.apache.poi.ss.usermodel.IndexedColors {
    public static org.apache.poi.ss.usermodel.IndexedColors GREY_25_PERCENT;
    public static org.apache.poi.ss.usermodel.IndexedColors *;
    public short index;
}

# ============================================
# Apache Commons Compress - CRITICAL FOR POI
# ============================================
# POI uses Apache Commons Compress for ZIP file handling
# CRITICAL: Must keep all classes and constructors
-keep class org.apache.commons.compress.** { *; }
-keepnames class org.apache.commons.compress.** { *; }
-keepclassmembers class org.apache.commons.compress.** {
    <init>(...);
    <methods>;
    <fields>;
}

# Keep Apache Commons Compress archivers (especially ZIP)
-keep class org.apache.commons.compress.archivers.** { *; }
-keep class org.apache.commons.compress.archivers.zip.** { *; }
-keepclassmembers class org.apache.commons.compress.archivers.** {
    <init>(...);
    <init>();
    <methods>;
    <fields>;
}

# Keep classes that may be loaded dynamically via Class.forName() or reflection
-keep class org.apache.poi.xssf.model.** { *; }
-keep class org.apache.poi.xssf.streaming.** { *; }
-keep class org.apache.poi.xssf.eventusermodel.** { *; }

# Keep POI utility classes
-keep class org.apache.poi.util.** { *; }
-keepclassmembers class org.apache.poi.util.** { *; }

# ============================================
# Optional Compression Libraries - DontWarn
# ============================================
# These are optional dependencies for Apache Commons Compress
# They are not needed on Android but may be referenced

# Zstd compression (optional) - use wildcard to cover all classes
-dontwarn com.github.luben.zstd.**

# Brotli compression (optional) - use wildcard to cover all classes
-dontwarn org.brotli.**

# ASM bytecode manipulation (optional, used at build time) - use wildcard to cover all classes
-dontwarn org.objectweb.asm.**

# ============================================
# OpenCSV - CRITICAL FOR CSV EXPORT
# ============================================
# Keep OpenCSV classes that we use for CSV export
-keep class com.opencsv.** { *; }
-keep interface com.opencsv.** { *; }
-keepclassmembers class com.opencsv.** { *; }

# ============================================
# Export Service - CRITICAL FOR EXPORT FUNCTIONALITY
# ============================================
# Keep ExportService and related classes
-keep class com.indybrain.indypos_Android.data.export.** { *; }
-keepclassmembers class com.indybrain.indypos_Android.data.export.** { *; }
# Keep ExportFormat enum
-keep enum com.indybrain.indypos_Android.data.export.ExportFormat { *; }
-keep enum com.indybrain.indypos_Android.data.export.ExportDataType { *; }

# ============================================
# Language/Locale Management - CRITICAL FOR LANGUAGE SWITCHING
# ============================================
# Keep LocaleHelper object class (used for locale management)
-keep class com.indybrain.indypos_Android.core.locale.LocaleHelper { *; }
-keepclassmembers class com.indybrain.indypos_Android.core.locale.LocaleHelper {
    <methods>;
    <fields>;
}
# Keep all methods in LocaleHelper (including private methods used via reflection)
-keepclassmembers class com.indybrain.indypos_Android.core.locale.LocaleHelper {
    public static ** getLocaleFromCode(...);
    public static ** getLocaleCode(...);
    public static ** setLocale(...);
    public static ** getCurrentLocale(...);
    private static ** updateResources(...);
}

# Keep LanguageLocalDataSource (used for storing language preference)
-keep class com.indybrain.indypos_Android.data.local.LanguageLocalDataSource { *; }
-keepclassmembers class com.indybrain.indypos_Android.data.local.LanguageLocalDataSource {
    <methods>;
    <fields>;
    <init>(...);
}
# Keep all methods in LanguageLocalDataSource
-keepclassmembers class com.indybrain.indypos_Android.data.local.LanguageLocalDataSource {
    public ** saveLanguageLocale(...);
    public ** getLanguageLocale();
    public ** getCurrentLanguageOption();
}

# Keep LanguageOption enum (used for language selection) - CRITICAL
# Must preserve enum constants and values() method for language switching
-keep enum com.indybrain.indypos_Android.data.local.LanguageOption { *; }
-keepclassmembers enum com.indybrain.indypos_Android.data.local.LanguageOption {
    <fields>;
    <methods>;
    public static ** valueOf(java.lang.String);
    public static **[] values();
    public ** localeCode;
    public ** displayName;
}
# Keep all enum constants (Thai, English) - CRITICAL for LanguageOption.values()
-keepclassmembers enum com.indybrain.indypos_Android.data.local.LanguageOption {
    public static final com.indybrain.indypos_Android.data.local.LanguageOption Thai;
    public static final com.indybrain.indypos_Android.data.local.LanguageOption English;
    **[] $VALUES;
    static **[] $values();
}
# Prevent enum name obfuscation and ensure values() is not removed
-keepnames enum com.indybrain.indypos_Android.data.local.LanguageOption
-keepclassmembernames enum com.indybrain.indypos_Android.data.local.LanguageOption {
    public static **[] values();
}

# Keep LanguageSettingsViewModel (ViewModel for language settings)
-keep class com.indybrain.indypos_Android.presentation.settings.LanguageSettingsViewModel { *; }
-keepclassmembers class com.indybrain.indypos_Android.presentation.settings.LanguageSettingsViewModel {
    <methods>;
    <fields>;
    <init>(...);
}
# Keep all methods in LanguageSettingsViewModel
-keepclassmembers class com.indybrain.indypos_Android.presentation.settings.LanguageSettingsViewModel {
    public ** selectLanguage(...);
    private ** loadCurrentLanguage();
}

# Keep LanguageSettingsUiState data class
-keep class com.indybrain.indypos_Android.presentation.settings.LanguageSettingsUiState { *; }
-keepclassmembers class com.indybrain.indypos_Android.presentation.settings.LanguageSettingsUiState {
    <fields>;
    <init>(...);
    public ** copy(...);
}
# Keep all fields in LanguageSettingsUiState
-keepclassmembers class com.indybrain.indypos_Android.presentation.settings.LanguageSettingsUiState {
    public ** selectedLanguage;
}

# Keep LanguageSettingsScreen composable (may use reflection for enum values())
-keep class com.indybrain.indypos_Android.presentation.settings.LanguageSettingsScreenKt { *; }
-keepclassmembers class com.indybrain.indypos_Android.presentation.settings.LanguageSettingsScreenKt {
    public static ** LanguageSettingsScreen(...);
    private static ** LanguageOptionRow(...);
}

# MainActivity: locale + notification PendingIntent targets (StockNotificationHelper)
-keep class com.indybrain.indypos_Android.MainActivity { *; }

# Low-stock system notifications (R8 must not strip helper or channel/notify chain)
-keep class com.indybrain.indypos_Android.core.notification.StockNotificationHelper { *; }

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