package ni.edu.uam.practicahilos;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador principal del Monitor de Procesamiento Empresarial.
 * Empresa: NicaData Solutions S.A.
 * 
 * Gestiona de forma completamente concurrente e independiente dos hilos de trabajo:
 * 1. Hilo-Clientes: simula 100 registros con 100ms de retardo (~10 segundos).
 * 2. Hilo-Productos: simula 50 registros con 150ms de retardo (~7.5 segundos).
 * 
 * Implementa control seguro con Thread, Runnable, sleep(), interrupt(), isInterrupted()
 * y Platform.runLater() para actualizar la interfaz gráfica sin congelar el JavaFX Application Thread.
 */
public class ProcesadorController implements Initializable {

    // Constantes de simulación
    public static final int TOTAL_CLIENTES = 100;
    public static final long SLEEP_CLIENTES_MS = 100L;

    public static final int TOTAL_PRODUCTOS = 50;
    public static final long SLEEP_PRODUCTOS_MS = 150L;

    // --- Componentes FXML: Panel de Clientes ---
    @FXML private Label lblEstadoClientes;
    @FXML private ProgressBar progressBarClientes;
    @FXML private Label lblPorcentajeClientes;
    @FXML private Label lblContadorClientes;
    @FXML private Button btnIniciarClientes;
    @FXML private Button btnDetenerClientes;
    @FXML private Button btnReiniciarClientes;

    // --- Componentes FXML: Panel de Productos ---
    @FXML private Label lblEstadoProductos;
    @FXML private ProgressBar progressBarProductos;
    @FXML private Label lblPorcentajeProductos;
    @FXML private Label lblContadorProductos;
    @FXML private Button btnIniciarProductos;
    @FXML private Button btnDetenerProductos;
    @FXML private Button btnReiniciarProductos;

    // Referencias a los hilos de trabajo en segundo plano
    private Thread hiloClientes;
    private Thread hiloProductos;

    // Identificadores de ejecución para invalidar actualizaciones pendientes (evita carreras y sobrescrituras)
    private int idEjecucionClientes = 0;
    private int idEjecucionProductos = 0;

    // Bandera para indicar que la aplicación ha iniciado proceso de cierre
    private volatile boolean cerrada = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarEstadoInicialClientes();
        configurarEstadoInicialProductos();
    }

    // =========================================================================
    // CONFIGURACIÓN DE ESTADOS INICIALES Y REINICIO
    // =========================================================================

    private void configurarEstadoInicialClientes() {
        lblEstadoClientes.setText("Esperando inicio");
        lblContadorClientes.setText("0 / " + TOTAL_CLIENTES);
        lblPorcentajeClientes.setText("0%");
        progressBarClientes.setProgress(0.0);

        btnIniciarClientes.setDisable(false);
        btnReiniciarClientes.setDisable(false);
        btnDetenerClientes.setDisable(true);
    }

    private void configurarEstadoInicialProductos() {
        lblEstadoProductos.setText("Esperando inicio");
        lblContadorProductos.setText("0 / " + TOTAL_PRODUCTOS);
        lblPorcentajeProductos.setText("0%");
        progressBarProductos.setProgress(0.0);

        btnIniciarProductos.setDisable(false);
        btnReiniciarProductos.setDisable(false);
        btnDetenerProductos.setDisable(true);
    }

    // =========================================================================
    // GESTIÓN DEL PANEL DE CLIENTES (Demuestra la práctica guiada base)
    // =========================================================================

    @FXML
    void onIniciarClientes(ActionEvent event) {
        // Validación: evitar duplicados ante clics rápidos si el hilo sigue vivo
        if (hiloClientes != null && hiloClientes.isAlive()) {
            return;
        }

        // Generamos un nuevo token de ejecución para invalidar callbacks previos
        final int idActual = ++idEjecucionClientes;

        // Actualizamos estado de UI en el hilo de JavaFX
        lblEstadoClientes.setText("Procesando registros...");
        lblContadorClientes.setText("0 / " + TOTAL_CLIENTES);
        lblPorcentajeClientes.setText("0%");
        progressBarClientes.setProgress(0.0);

        btnIniciarClientes.setDisable(true);
        btnReiniciarClientes.setDisable(true);
        btnDetenerClientes.setDisable(false);

        // Definimos la tarea Runnable para Clientes
        Runnable tareaClientes = () -> {
            boolean fueInterrumpido = false;

            for (int i = 0; i < TOTAL_CLIENTES; i++) {
                // Comprobación de interrupción previa a la pausa
                if (Thread.currentThread().isInterrupted()) {
                    fueInterrumpido = true;
                    break;
                }

                try {
                    // Simulación de trabajo en segundo plano
                    Thread.sleep(SLEEP_CLIENTES_MS);
                } catch (InterruptedException e) {
                    // Restauración de la bandera de interrupción según las buenas prácticas de Java
                    Thread.currentThread().interrupt();
                    fueInterrumpido = true;
                    break;
                }

                // Captura de variables finales para uso seguro en la expresión lambda de runLater
                final int procesados = i + 1;
                final double progreso = (double) procesados / TOTAL_CLIENTES;
                final int porcentaje = (procesados * 100) / TOTAL_CLIENTES;

                // Delegación de la actualización visual al JavaFX Application Thread
                Platform.runLater(() -> {
                    // Si la aplicación se cerró o pertenece a una ejecución vieja, descartar
                    if (cerrada || idActual != idEjecucionClientes) {
                        return;
                    }
                    lblContadorClientes.setText(procesados + " / " + TOTAL_CLIENTES);
                    lblPorcentajeClientes.setText(porcentaje + "%");
                    progressBarClientes.setProgress(progreso);
                });
            }

            final boolean terminoPorInterrupcion = fueInterrumpido || Thread.currentThread().isInterrupted();

            // Notificación final a la UI
            Platform.runLater(() -> {
                if (cerrada || idActual != idEjecucionClientes) {
                    return;
                }
                if (terminoPorInterrupcion) {
                    lblEstadoClientes.setText("Proceso detenido");
                } else {
                    lblEstadoClientes.setText("Proceso finalizado");
                    lblContadorClientes.setText(TOTAL_CLIENTES + " / " + TOTAL_CLIENTES);
                    lblPorcentajeClientes.setText("100%");
                    progressBarClientes.setProgress(1.0);
                }
                btnIniciarClientes.setDisable(false);
                btnReiniciarClientes.setDisable(false);
                btnDetenerClientes.setDisable(true);
            });
        };

        // Creamos una nueva instancia de Thread (nunca reutilizar un Thread terminado)
        hiloClientes = new Thread(tareaClientes, "Hilo-Clientes");
        hiloClientes.setDaemon(true); // Se marca como demonio para no trabar la JVM
        hiloClientes.start();        // start() genera el hilo real del sistema operativo
    }

    @FXML
    void onDetenerClientes(ActionEvent event) {
        lblEstadoClientes.setText("Solicitando detención...");
        btnDetenerClientes.setDisable(true);

        if (hiloClientes != null && hiloClientes.isAlive()) {
            hiloClientes.interrupt(); // Envía la señal de interrupción al hilo
        }
    }

    @FXML
    void onReiniciarClientes(ActionEvent event) {
        // Solo permitido cuando el proceso no está activo
        if (hiloClientes != null && hiloClientes.isAlive()) {
            return;
        }
        // Incrementamos el identificador para cancelar cualquier actualización rezagada
        idEjecucionClientes++;
        configurarEstadoInicialClientes();
    }

    // =========================================================================
    // GESTIÓN DEL PANEL DE PRODUCTOS (Demuestra el reto final concurrente)
    // =========================================================================

    @FXML
    void onIniciarProductos(ActionEvent event) {
        // Validación: evitar duplicados ante clics rápidos si el hilo sigue vivo
        if (hiloProductos != null && hiloProductos.isAlive()) {
            return;
        }

        // Generamos un nuevo token de ejecución para invalidar callbacks previos
        final int idActual = ++idEjecucionProductos;

        // Actualizamos estado de UI en el hilo de JavaFX
        lblEstadoProductos.setText("Procesando registros...");
        lblContadorProductos.setText("0 / " + TOTAL_PRODUCTOS);
        lblPorcentajeProductos.setText("0%");
        progressBarProductos.setProgress(0.0);

        btnIniciarProductos.setDisable(true);
        btnReiniciarProductos.setDisable(true);
        btnDetenerProductos.setDisable(false);

        // Definimos la tarea Runnable para Productos
        Runnable tareaProductos = () -> {
            boolean fueInterrumpido = false;

            for (int i = 0; i < TOTAL_PRODUCTOS; i++) {
                // Comprobación de interrupción previa a la pausa
                if (Thread.currentThread().isInterrupted()) {
                    fueInterrumpido = true;
                    break;
                }

                try {
                    // Simulación de trabajo en segundo plano
                    Thread.sleep(SLEEP_PRODUCTOS_MS);
                } catch (InterruptedException e) {
                    // Restauración de la bandera de interrupción
                    Thread.currentThread().interrupt();
                    fueInterrumpido = true;
                    break;
                }

                // Captura de variables finales para uso seguro en la expresión lambda de runLater
                final int procesados = i + 1;
                final double progreso = (double) procesados / TOTAL_PRODUCTOS;
                final int porcentaje = (procesados * 100) / TOTAL_PRODUCTOS;

                // Delegación de la actualización visual al JavaFX Application Thread
                Platform.runLater(() -> {
                    if (cerrada || idActual != idEjecucionProductos) {
                        return;
                    }
                    lblContadorProductos.setText(procesados + " / " + TOTAL_PRODUCTOS);
                    lblPorcentajeProductos.setText(porcentaje + "%");
                    progressBarProductos.setProgress(progreso);
                });
            }

            final boolean terminoPorInterrupcion = fueInterrumpido || Thread.currentThread().isInterrupted();

            // Notificación final a la UI
            Platform.runLater(() -> {
                if (cerrada || idActual != idEjecucionProductos) {
                    return;
                }
                if (terminoPorInterrupcion) {
                    lblEstadoProductos.setText("Proceso detenido");
                } else {
                    lblEstadoProductos.setText("Proceso finalizado");
                    lblContadorProductos.setText(TOTAL_PRODUCTOS + " / " + TOTAL_PRODUCTOS);
                    lblPorcentajeProductos.setText("100%");
                    progressBarProductos.setProgress(1.0);
                }
                btnIniciarProductos.setDisable(false);
                btnReiniciarProductos.setDisable(false);
                btnDetenerProductos.setDisable(true);
            });
        };

        // Creamos una nueva instancia de Thread independiente
        hiloProductos = new Thread(tareaProductos, "Hilo-Productos");
        hiloProductos.setDaemon(true);
        hiloProductos.start();
    }

    @FXML
    void onDetenerProductos(ActionEvent event) {
        lblEstadoProductos.setText("Solicitando detención...");
        btnDetenerProductos.setDisable(true);

        if (hiloProductos != null && hiloProductos.isAlive()) {
            hiloProductos.interrupt();
        }
    }

    @FXML
    void onReiniciarProductos(ActionEvent event) {
        // Solo permitido cuando el proceso no está activo
        if (hiloProductos != null && hiloProductos.isAlive()) {
            return;
        }
        idEjecucionProductos++;
        configurarEstadoInicialProductos();
    }

    // =========================================================================
    // CIERRE CONTROLADO (Invocado desde MainApp al cerrar ventana o en stop())
    // =========================================================================

    /**
     * Interrumpe de manera limpia ambos hilos si están activos al cerrar la aplicación.
     * Es idempotente: puede llamarse múltiples veces de forma segura sin producir errores.
     */
    public synchronized void detenerAlCerrar() {
        cerrada = true;

        if (hiloClientes != null && hiloClientes.isAlive()) {
            hiloClientes.interrupt();
        }

        if (hiloProductos != null && hiloProductos.isAlive()) {
            hiloProductos.interrupt();
        }
    }

    // Métodos de consulta de estado (útiles para pruebas y depuración)
    public boolean isClientesActivo() {
        return hiloClientes != null && hiloClientes.isAlive();
    }

    public boolean isProductosActivo() {
        return hiloProductos != null && hiloProductos.isAlive();
    }
}
