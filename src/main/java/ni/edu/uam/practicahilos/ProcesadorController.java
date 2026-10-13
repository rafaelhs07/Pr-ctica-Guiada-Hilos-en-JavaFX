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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controlador principal del Monitor de Procesamiento Empresarial.
 * Empresa: NicaData Solutions S.A.
 * 
 * Gestiona de forma completamente concurrente e independiente dos flujos de trabajo:
 * 1. Hilo-Clientes: simula 100 registros con 100ms de retardo c/u (~10 segundos).
 * 2. Hilo-Productos: simula 50 registros con 150ms de retardo c/u (~7.5 segundos).
 * 
 * Comportamiento de ciclo de vida con reanudación:
 * - Iniciar: comienza desde 0 si está al inicio, o continúa desde el progreso conservado tras detenerse.
 * - Detener: interrumpe el hilo cooperativamente y conserva los registros completados.
 * - Reiniciar: única acción que borra el avance y regresa todo a 0 (solo disponible cuando el hilo no está activo).
 * - Al completar el 100%: Iniciar y Detener quedan deshabilitados; se requiere Reiniciar para una nueva ejecución.
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

    // Contadores de registros completados conservados entre ejecuciones (acceso seguro concurrente)
    private final AtomicInteger completadosClientes = new AtomicInteger(0);
    private final AtomicInteger completadosProductos = new AtomicInteger(0);

    // Referencias a los hilos de trabajo en segundo plano
    private Thread hiloClientes;
    private Thread hiloProductos;

    // Identificadores de ejecución para invalidar actualizaciones pendientes de ejecuciones previas
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
    // GESTIÓN DEL PANEL DE CLIENTES
    // =========================================================================

    @FXML
    void onIniciarClientes(ActionEvent event) {
        // Validación 1: evitar duplicados ante clics rápidos si el hilo sigue vivo
        if (hiloClientes != null && hiloClientes.isAlive()) {
            return;
        }

        // Validación 2: si ya completó todos los registros, se requiere primero pulsar Reiniciar
        if (completadosClientes.get() >= TOTAL_CLIENTES) {
            return;
        }

        // Generamos un nuevo token de ejecución para invalidar callbacks de ejecuciones previas
        final int idActual = ++idEjecucionClientes;

        // Obtenemos el punto de partida conservado (0 si es inicio, o > 0 si se reanuda)
        final int inicio = completadosClientes.get();

        // Actualizamos estado de UI en el JavaFX Application Thread
        lblEstadoClientes.setText("Procesando registros...");
        btnIniciarClientes.setDisable(true);
        btnReiniciarClientes.setDisable(true);
        btnDetenerClientes.setDisable(false);

        // Si es el primer inicio (desde cero), aseguramos que los controles reflejen el inicio
        if (inicio == 0) {
            lblContadorClientes.setText("0 / " + TOTAL_CLIENTES);
            lblPorcentajeClientes.setText("0%");
            progressBarClientes.setProgress(0.0);
        }

        // Definimos la tarea Runnable para Clientes a partir del índice conservado
        Runnable tareaClientes = () -> {
            boolean fueInterrumpido = false;

            for (int i = inicio; i < TOTAL_CLIENTES; i++) {
                // Comprobación de interrupción previa a la simulación
                if (Thread.currentThread().isInterrupted()) {
                    fueInterrumpido = true;
                    break;
                }

                try {
                    // Simulación de procesamiento de este registro
                    Thread.sleep(SLEEP_CLIENTES_MS);
                } catch (InterruptedException e) {
                    // Si se interrumpe durante el sleep, el registro actual queda PENDIENTE (no se cuenta)
                    Thread.currentThread().interrupt();
                    fueInterrumpido = true;
                    break;
                }

                // El registro completó exitosamente su procesamiento: se contabiliza
                final int procesados = completadosClientes.incrementAndGet();
                final double progreso = (double) procesados / TOTAL_CLIENTES;
                final int porcentaje = (procesados * 100) / TOTAL_CLIENTES;

                // Delegación de la actualización visual al JavaFX Application Thread
                Platform.runLater(() -> {
                    // Si la aplicación se cerró o pertenece a una ejecución vieja/reiniciada, descartar
                    if (cerrada || idActual != idEjecucionClientes) {
                        return;
                    }
                    lblContadorClientes.setText(procesados + " / " + TOTAL_CLIENTES);
                    lblPorcentajeClientes.setText(porcentaje + "%");
                    progressBarClientes.setProgress(progreso);
                });
            }

            final boolean terminoPorInterrupcion = fueInterrumpido || Thread.currentThread().isInterrupted();
            final int totalFinal = completadosClientes.get();

            // Notificación final al concluir o detenerse el hilo
            Platform.runLater(() -> {
                if (cerrada || idActual != idEjecucionClientes) {
                    return;
                }

                // Sincronizar barra, porcentaje y contador con el valor conservado
                final double progreso = (double) totalFinal / TOTAL_CLIENTES;
                final int porcentaje = (totalFinal * 100) / TOTAL_CLIENTES;
                lblContadorClientes.setText(totalFinal + " / " + TOTAL_CLIENTES);
                lblPorcentajeClientes.setText(porcentaje + "%");
                progressBarClientes.setProgress(progreso);

                if (terminoPorInterrupcion) {
                    // Proceso detenido: se conserva el avance; permite continuar con Iniciar o borrar con Reiniciar
                    lblEstadoClientes.setText("Proceso detenido");
                    btnIniciarClientes.setDisable(false);
                    btnReiniciarClientes.setDisable(false);
                    btnDetenerClientes.setDisable(true);
                } else {
                    // Proceso finalizado: completó el 100%. Iniciar queda deshabilitado; solo permite Reiniciar
                    lblEstadoClientes.setText("Proceso finalizado");
                    lblContadorClientes.setText(TOTAL_CLIENTES + " / " + TOTAL_CLIENTES);
                    lblPorcentajeClientes.setText("100%");
                    progressBarClientes.setProgress(1.0);

                    btnIniciarClientes.setDisable(true);
                    btnReiniciarClientes.setDisable(false);
                    btnDetenerClientes.setDisable(true);
                }
            });
        };

        // Creamos una nueva instancia de Thread (nunca reutilizar un Thread terminado)
        hiloClientes = new Thread(tareaClientes, "Hilo-Clientes");
        hiloClientes.setDaemon(true);
        hiloClientes.start();
    }

    @FXML
    void onDetenerClientes(ActionEvent event) {
        if (hiloClientes == null || !hiloClientes.isAlive()) {
            return;
        }

        lblEstadoClientes.setText("Solicitando detención...");
        // Deshabilitar todos los botones mientras el hilo confirma la detención (evita carreras y clics rápidos)
        btnDetenerClientes.setDisable(true);
        btnIniciarClientes.setDisable(true);
        btnReiniciarClientes.setDisable(true);

        hiloClientes.interrupt(); // Envía la señal de interrupción al hilo
    }

    @FXML
    void onReiniciarClientes(ActionEvent event) {
        // Solo permitido cuando el proceso no está activo ni deteniéndose
        if (hiloClientes != null && hiloClientes.isAlive()) {
            return;
        }
        // Incrementamos el identificador para cancelar cualquier actualización rezagada
        idEjecucionClientes++;
        // Reiniciar es la ÚNICA acción que restablece el contador a cero
        completadosClientes.set(0);
        configurarEstadoInicialClientes();
    }

    // =========================================================================
    // GESTIÓN DEL PANEL DE PRODUCTOS
    // =========================================================================

    @FXML
    void onIniciarProductos(ActionEvent event) {
        // Validación 1: evitar duplicados ante clics rápidos si el hilo sigue vivo
        if (hiloProductos != null && hiloProductos.isAlive()) {
            return;
        }

        // Validación 2: si ya completó todos los registros, se requiere primero pulsar Reiniciar
        if (completadosProductos.get() >= TOTAL_PRODUCTOS) {
            return;
        }

        // Generamos un nuevo token de ejecución para invalidar callbacks previos
        final int idActual = ++idEjecucionProductos;

        // Obtenemos el punto de partida conservado (0 si es inicio, o > 0 si se reanuda)
        final int inicio = completadosProductos.get();

        // Actualizamos estado de UI en el JavaFX Application Thread
        lblEstadoProductos.setText("Procesando registros...");
        btnIniciarProductos.setDisable(true);
        btnReiniciarProductos.setDisable(true);
        btnDetenerProductos.setDisable(false);

        if (inicio == 0) {
            lblContadorProductos.setText("0 / " + TOTAL_PRODUCTOS);
            lblPorcentajeProductos.setText("0%");
            progressBarProductos.setProgress(0.0);
        }

        // Definimos la tarea Runnable para Productos a partir del índice conservado
        Runnable tareaProductos = () -> {
            boolean fueInterrumpido = false;

            for (int i = inicio; i < TOTAL_PRODUCTOS; i++) {
                // Comprobación de interrupción previa a la pausa
                if (Thread.currentThread().isInterrupted()) {
                    fueInterrumpido = true;
                    break;
                }

                try {
                    // Simulación de procesamiento
                    Thread.sleep(SLEEP_PRODUCTOS_MS);
                } catch (InterruptedException e) {
                    // Registro queda pendiente si se interrumpe durante el sleep
                    Thread.currentThread().interrupt();
                    fueInterrumpido = true;
                    break;
                }

                // El registro completó exitosamente su procesamiento: se contabiliza
                final int procesados = completadosProductos.incrementAndGet();
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
            final int totalFinal = completadosProductos.get();

            // Notificación final al concluir o detenerse el hilo
            Platform.runLater(() -> {
                if (cerrada || idActual != idEjecucionProductos) {
                    return;
                }

                // Sincronizar barra, porcentaje y contador con el valor conservado
                final double progreso = (double) totalFinal / TOTAL_PRODUCTOS;
                final int porcentaje = (totalFinal * 100) / TOTAL_PRODUCTOS;
                lblContadorProductos.setText(totalFinal + " / " + TOTAL_PRODUCTOS);
                lblPorcentajeProductos.setText(porcentaje + "%");
                progressBarProductos.setProgress(progreso);

                if (terminoPorInterrupcion) {
                    lblEstadoProductos.setText("Proceso detenido");
                    btnIniciarProductos.setDisable(false);
                    btnReiniciarProductos.setDisable(false);
                    btnDetenerProductos.setDisable(true);
                } else {
                    lblEstadoProductos.setText("Proceso finalizado");
                    lblContadorProductos.setText(TOTAL_PRODUCTOS + " / " + TOTAL_PRODUCTOS);
                    lblPorcentajeProductos.setText("100%");
                    progressBarProductos.setProgress(1.0);

                    btnIniciarProductos.setDisable(true);
                    btnReiniciarProductos.setDisable(false);
                    btnDetenerProductos.setDisable(true);
                }
            });
        };

        // Creamos una nueva instancia de Thread independiente
        hiloProductos = new Thread(tareaProductos, "Hilo-Productos");
        hiloProductos.setDaemon(true);
        hiloProductos.start();
    }

    @FXML
    void onDetenerProductos(ActionEvent event) {
        if (hiloProductos == null || !hiloProductos.isAlive()) {
            return;
        }

        lblEstadoProductos.setText("Solicitando detención...");
        btnDetenerProductos.setDisable(true);
        btnIniciarProductos.setDisable(true);
        btnReiniciarProductos.setDisable(true);

        hiloProductos.interrupt();
    }

    @FXML
    void onReiniciarProductos(ActionEvent event) {
        if (hiloProductos != null && hiloProductos.isAlive()) {
            return;
        }
        idEjecucionProductos++;
        completadosProductos.set(0);
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

    // =========================================================================
    // MÉTODOS DE CONSULTA DE ESTADO (Para pruebas y verificación)
    // =========================================================================

    public boolean isClientesActivo() {
        return hiloClientes != null && hiloClientes.isAlive();
    }

    public boolean isProductosActivo() {
        return hiloProductos != null && hiloProductos.isAlive();
    }

    public int getCompletadosClientes() {
        return completadosClientes.get();
    }

    public int getCompletadosProductos() {
        return completadosProductos.get();
    }

    public boolean isBtnIniciarClientesDisabled() {
        return btnIniciarClientes.isDisable();
    }

    public boolean isBtnDetenerClientesDisabled() {
        return btnDetenerClientes.isDisable();
    }

    public boolean isBtnReiniciarClientesDisabled() {
        return btnReiniciarClientes.isDisable();
    }

    public boolean isBtnIniciarProductosDisabled() {
        return btnIniciarProductos.isDisable();
    }

    public boolean isBtnDetenerProductosDisabled() {
        return btnDetenerProductos.isDisable();
    }

    public boolean isBtnReiniciarProductosDisabled() {
        return btnReiniciarProductos.isDisable();
    }
}
