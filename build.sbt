import com.github.retronym.SbtOneJar._

name := "pdfToJpeg"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.pdfbox" % "pdfbox" % "1.8.10"
)

oneJarSettings