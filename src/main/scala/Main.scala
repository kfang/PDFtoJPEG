import java.awt.Desktop
import java.awt.image.BufferedImage
import java.io.{FileOutputStream, File}
import java.text.DecimalFormat
import javafx.application.{Platform, Application}
import javafx.concurrent.Task
import javafx.event.{ActionEvent, EventHandler}
import javafx.geometry.{Insets, Pos}
import javafx.scene.Scene
import javafx.scene.control.Alert.AlertType
import javafx.scene.control._
import javafx.scene.layout._
import javafx.stage.{DirectoryChooser, FileChooser, Stage}

import org.apache.pdfbox.pdmodel.{PDPage, PDDocument}
import org.apache.pdfbox.util.ImageIOUtil
import scala.collection.JavaConversions._
import scala.util.Try

class Main extends Application {

  override def start(primaryStage: Stage): Unit = {
    primaryStage.setTitle("PDF to PNG")

    var loadedDoc: Option[(File, PDDocument)] = None
    var saveDirectory: Option[File] = None

    //choose pdf elements
    val choosePDFLabel = new Label("PDF:")
    val chooseButton = new Button("Browse for PDF")
    val pdfTextField = new TextField("choose a pdf")
    pdfTextField.setPrefWidth(300)
    pdfTextField.setEditable(false)

    //choose directory elements
    val chooseDirLabel = new Label("Save Directory:")
    val directoryButton = new Button("Save To")
    val directoryTextField = new TextField("choose a save directory")
    directoryTextField.setPrefWidth(300.0)
    directoryTextField.setEditable(false)

    //conversion elements
    val progress = new ProgressBar()
    val convert = new Button("Convert")
    val progressLabel = new Label("0/0")
    progress.setPrefWidth(400)
    progress.setVisible(false)
    progressLabel.setVisible(false)

    //output information label
    val outputLabel = new Label("")
    outputLabel.setVisible(false)
    outputLabel.setPadding(new Insets(0, 0, 0, 3))


    //bind the choose pdf button
    chooseButton.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        //initialize the file chooser
        val fc = new FileChooser()
        fc.setTitle("Choose PDF")

        //open the file chooser
        val file = fc.showOpenDialog(primaryStage)

        //attempt to load the PDF
        val tryDoc = Try(PDDocument.load(file)).toOption

        //if the PDF is valid, set the label, otherwise give user an alert
        if(tryDoc.isDefined) {
          loadedDoc = Some((file, tryDoc.get))
          pdfTextField.setText(file.getAbsolutePath)

          val parentDirectory = new File(file.getParent)
          if(parentDirectory.isDirectory){
            saveDirectory = Some(parentDirectory)
            directoryTextField.setText(parentDirectory.getAbsolutePath)
          }

        } else {
          val alert = new Alert(AlertType.ERROR, "Not a Valid PDF")
          alert.show()
        }

      }
    })

    //bind the choose directory element
    directoryButton.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        val dc = new DirectoryChooser()
        val dir = dc.showDialog(primaryStage)
        if(dir != null){
          saveDirectory = Some(dir)
          directoryTextField.setText(dir.getAbsolutePath)
        }
      }
    })

    //bind the convert button
    convert.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        if(loadedDoc.isDefined && saveDirectory.isDefined){

          outputLabel.setVisible(false)
          progress.setVisible(true)
          progressLabel.setVisible(true)
          progress.setProgress(0)
          progressLabel.setText("0/0")

          //task to generate the JPEGs
          val task = new Task[Unit] {
            override def call(): Unit = {

              val dir = saveDirectory.get.getAbsolutePath
              val pdf = loadedDoc.get._2
              val pages = pdf.getDocumentCatalog.getAllPages.toList
              val df = new DecimalFormat("0000")
              val prefix = loadedDoc.get._1.getName.stripSuffix(".pdf")

              pages.zipWithIndex.foreach({
                case (page, index) =>

                  val image = page.asInstanceOf[PDPage].convertToImage(BufferedImage.TYPE_INT_RGB, 400)
                  val filename = df.format(index) + ".jpg"
                  val file = new File(dir + "/" + prefix + "-" + filename)
                  val os = new FileOutputStream(file)
                  ImageIOUtil.writeImage(image, "jpg", os , 800)
                  os.flush()
                  os.close()

                  updateProgress(index + 1, pages.size)

                  Platform.runLater(new Runnable {
                    override def run(): Unit = {
                      progressLabel.setText(s"${index + 1}/${pages.size}")
                      progress.setProgress(getProgress)
                    }
                  })
              })

              Platform.runLater(new Runnable {
                override def run(): Unit = {
                  outputLabel.setText(s"${pages.size} files were written to ${saveDirectory.get.getAbsolutePath}")
                  outputLabel.setVisible(true)
                }
              })

              Desktop.getDesktop.open(saveDirectory.get)
            }
          }

          val thread = new Thread(task, "pdf-to-jpg")
          thread.setDaemon(true)
          thread.start()

        } else {
          val alert = new Alert(AlertType.ERROR, "You must specify a PDF and Directory")
          alert.show()
        }
      }
    })


    //setup the layout
    val root = new GridPane()
    root.setHgap(3.0)

    val choosePDFLabelTextField = new HBox()
    choosePDFLabelTextField.getChildren.addAll(choosePDFLabel, pdfTextField)
    choosePDFLabelTextField.setSpacing(3.0)
    choosePDFLabelTextField.setAlignment(Pos.CENTER_RIGHT)
    root.add(choosePDFLabelTextField, 0, 0)
    root.add(chooseButton, 1, 0)

    val chooseDirLabelTextField = new HBox()
    chooseDirLabelTextField.setSpacing(3.0)
    chooseDirLabelTextField.setAlignment(Pos.CENTER_RIGHT)
    chooseDirLabelTextField.getChildren.addAll(chooseDirLabel, directoryTextField)
    root.add(chooseDirLabelTextField, 0, 1)
    root.add(directoryButton, 1, 1)

    root.add(progress, 0, 2)
    val convertBtnLabel = new HBox()  //we want to put the button and label in the same box
    convertBtnLabel.setSpacing(3.0)
    convertBtnLabel.setAlignment(Pos.BOTTOM_LEFT)
    convertBtnLabel.getChildren.addAll(convert, progressLabel)
    root.add(convertBtnLabel, 1, 2)

    root.add(outputLabel, 0, 3, 2, 2)

    primaryStage.setScene(new Scene(root))
    primaryStage.show()
  }
}

object Main {
  def main (args: Array[String]){
    Application.launch(classOf[Main], args: _*)
  }
}
