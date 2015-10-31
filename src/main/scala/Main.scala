import java.awt.{Desktop}
import java.awt.image.BufferedImage
import java.io.{FileOutputStream, File}
import java.text.DecimalFormat
import java.util.UUID
import javafx.application.{Platform, Application}
import javafx.concurrent.Task
import javafx.event.{ActionEvent, EventHandler}
import javafx.scene.Scene
import javafx.scene.control.Alert.AlertType
import javafx.scene.control._
import javafx.scene.layout.{GridPane, StackPane}
import javafx.stage.FileChooser.ExtensionFilter
import javafx.stage.{DirectoryChooser, FileChooser, Stage}

import org.apache.pdfbox.pdmodel.{PDPage, PDDocument}
import org.apache.pdfbox.util.ImageIOUtil
import scala.collection.JavaConversions._
import scala.util.Try

class Main extends Application {

  override def start(primaryStage: Stage): Unit = {
    primaryStage.setTitle("PDF to PNG")
    val root = new GridPane()

    var loadedDoc: Option[PDDocument] = None
    var saveDirectory: Option[File] = None

    //choose pdf elements
    val button = new Button("Browse for PDF...")
    val pdfTextField = new TextField("choose a pdf")
    pdfTextField.setPrefWidth(200.0)
    pdfTextField.setEditable(false)

    //bind the choose pdf button
    button.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        //initialize the file chooser
        val fc = new FileChooser()
        fc.setTitle("Choose PDF")

        //open the file chooser
        val file = fc.showOpenDialog(primaryStage)

        //attempt to load the PDF
        loadedDoc = Try(PDDocument.load(file)).toOption

        //if the PDF is valid, set the label, otherwise give user an alert
        if(loadedDoc.isDefined) {
          pdfTextField.setText(file.getAbsolutePath)
        } else {
          val alert = new Alert(AlertType.ERROR, "Not a Valid PDF")
          alert.show()
        }

      }
    })


    //choose directory elements
    val directoryButton = new Button("Browse...")
    val directoryTextField = new TextField("choose a save directory")
    directoryTextField.setPrefWidth(200.0)
    directoryTextField.setEditable(false)

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


    //start conversion elements
    val progress = new ProgressBar()
    val convert = new Button("Convert")
    val progressLabel = new Label("0/0")

    progress.setMinWidth(350)
    progress.setVisible(false)
    progressLabel.setVisible(false)


    convert.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        if(loadedDoc.isDefined && saveDirectory.isDefined){

          progress.setVisible(true)
          progressLabel.setVisible(true)

          //task to generate the JPEGs
          val task = new Task[Unit] {
            override def call(): Unit = {

              val dir = saveDirectory.get.getAbsolutePath
              val pdf = loadedDoc.get
              val pages = pdf.getDocumentCatalog.getAllPages.toList
              val df = new DecimalFormat("0000")

              pages.zipWithIndex.foreach({
                case (page, index) =>

                  val image = page.asInstanceOf[PDPage].convertToImage(BufferedImage.TYPE_INT_RGB, 400)
                  val filename = df.format(index) + ".jpg"
                  val file = new File(dir + "/" + filename)
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

              Desktop.getDesktop.open(saveDirectory.get)
              pdf.close()
            }
          }

          val thread = new Thread(task, "pdf-to-jpg")
          thread.setDaemon(true)
          thread.start()

        } else {
          //need to alert the user
        }
      }
    })

    root.setHgap(3.0)

    root.add(pdfTextField, 0, 0)
    root.add(button, 1, 0)

    root.add(directoryTextField, 0, 1)
    root.add(directoryButton, 1, 1)

    root.add(progress, 0, 2)
    root.add(convert, 1, 2)
    root.add(progressLabel, 2, 2)

    primaryStage.setScene(new Scene(root))
    primaryStage.setWidth(500)
    primaryStage.show()
  }
}

object Main {
  def main (args: Array[String]){
    Application.launch(classOf[Main], args: _*)
  }
}
