package ni.edu.uam.practicahilos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Clase principal de inicio para la aplicación JavaFX.
 * Caso empresarial: NicaData Solutions S.A.
 * 
 * Responsabilidades:
 * - Cargar la interfaz FXML (procesador-view.fxml).
 * - Mantener la referencia al controlador ProcesadorController.
 * - Asegurar el cierre controlado de hilos activos tanto en onCloseRequest como en stop().
 */
public class MainApp extends Application {

    private ProcesadorController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("procesador-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 760, 520);

        // Obtenemos y conservamos la referencia al controlador
        controller = fxmlLoader.getController();

        stage.setTitle("NicaData Solutions S.A. - Monitor de Procesamiento Empresarial");
        stage.setMinWidth(700);
        stage.setMinHeight(480);
        stage.setScene(scene);

        // Limpieza de hilos controlada al pulsar el botón de cerrar la ventana
        stage.setOnCloseRequest(event -> {
            if (controller != null) {
                controller.detenerAlCerrar();
            }
        });

        stage.show();
    }

    /**
     * Invocado por el ciclo de vida de JavaFX cuando la aplicación se detiene.
     * Garantiza que los hilos no queden huérfanos incluso si se cierra de otra forma.
     */
    @Override
    public void stop() throws Exception {
        if (controller != null) {
            controller.detenerAlCerrar();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
