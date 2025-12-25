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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Apache POI - Ignore missing classes that are not available on Android
# Java AWT classes (not available on Android)
-dontwarn java.awt.**
-dontwarn java.awt.color.**
-dontwarn java.awt.geom.**
-dontwarn java.awt.image.**

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