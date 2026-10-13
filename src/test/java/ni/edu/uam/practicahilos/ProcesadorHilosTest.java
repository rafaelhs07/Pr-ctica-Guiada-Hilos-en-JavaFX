package ni.edu.uam.practicahilos;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para validar las reglas de negocio, constantes,
 * existencia de recursos, carga de FXML y lógica de concurrencia e interrupción de la práctica.
 */
public class ProcesadorHilosTest {

    private static boolean javafxInicializado = false;

    @BeforeAll
    static void initJavaFX() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            javafxInicializado = latch.await(3, TimeUnit.SECONDS);
        } catch (IllegalStateException e) {
            // Ya iniciado previamente
            javafxInicializado = true;
        } catch (Exception e) {
            javafxInicializado = false;
        }
    }

    @Test
    @DisplayName("Verificar que procesador-view.fxml se carga correctamente con FXMLLoader y el controlador")
    void testCargaFxmlYControlador() throws Exception {
        if (!javafxInicializado) {
            System.out.println("JavaFX toolkit no disponible en entorno actual, omitiendo prueba de UI completa.");
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] errorRef = new Throwable[1];

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("procesador-view.fxml"));
                Parent root = loader.load();
                assertNotNull(root, "La raíz del FXML no debe ser nula");

                ProcesadorController controller = loader.getController();
                assertNotNull(controller, "El controlador debe ser inyectado correctamente");
                assertFalse(controller.isClientesActivo(), "Al inicio Clientes no debe estar activo");
                assertFalse(controller.isProductosActivo(), "Al inicio Productos no debe estar activo");
            } catch (Throwable t) {
                errorRef[0] = t;
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout esperando carga de FXML en hilo JavaFX");
        if (errorRef[0] != null) {
            fail("Error al cargar FXML: " + errorRef[0].getMessage(), errorRef[0]);
        }
    }

    @Test
    @DisplayName("Verificar ciclo de vida del controlador: Iniciar, Detener, Reiniciar y Cierre controlado")
    void testCicloVidaControlador() throws Exception {
        if (!javafxInicializado) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] errorRef = new Throwable[1];

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("procesador-view.fxml"));
                loader.load();
                ProcesadorController controller = loader.getController();

                // 1. Iniciar ambos procesos
                controller.getClass().getDeclaredMethod("onIniciarClientes", javafx.event.ActionEvent.class)
                        .invoke(controller, (javafx.event.ActionEvent) null);
                controller.getClass().getDeclaredMethod("onIniciarProductos", javafx.event.ActionEvent.class)
                        .invoke(controller, (javafx.event.ActionEvent) null);

                assertTrue(controller.isClientesActivo(), "Clientes debe estar activo tras iniciar");
                assertTrue(controller.isProductosActivo(), "Productos debe estar activo tras iniciar");

                // 2. Detener un proceso (Productos) mientras Clientes continúa
                controller.getClass().getDeclaredMethod("onDetenerProductos", javafx.event.ActionEvent.class)
                        .invoke(controller, (javafx.event.ActionEvent) null);

                // Esperamos un momento breve en un hilo separado para verificar detención
                new Thread(() -> {
                    try {
                        Thread.sleep(300);
                        assertFalse(controller.isProductosActivo(), "Productos debió detenerse tras la interrupción");
                        assertTrue(controller.isClientesActivo(), "Clientes debe continuar ejecutándose");

                        // 3. Probar detenerAlCerrar (idempotente)
                        controller.detenerAlCerrar();
                        controller.detenerAlCerrar();

                        Thread.sleep(250);
                        assertFalse(controller.isClientesActivo(), "Clientes debió detenerse tras detenerAlCerrar");
                    } catch (Throwable t) {
                        errorRef[0] = t;
                    } finally {
                        latch.countDown();
                    }
                }).start();

            } catch (Throwable t) {
                errorRef[0] = t;
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout probando ciclo de vida del controlador");
        if (errorRef[0] != null) {
            fail("Error en prueba de ciclo de vida: " + errorRef[0].getMessage(), errorRef[0]);
        }
    }

    @Test
    @DisplayName("Verificar que el archivo FXML existe y es accesible en el classpath")
    void testRecursoFxmlExiste() {
        InputStream fxmlStream = getClass().getResourceAsStream("procesador-view.fxml");
        assertNotNull(fxmlStream, "El archivo procesador-view.fxml debe existir en el paquete ni.edu.uam.practicahilos");
    }

    @Test
    @DisplayName("Verificar constantes de configuración para Clientes y Productos")
    void testConstantesConfiguracion() {
        assertEquals(100, ProcesadorController.TOTAL_CLIENTES, "Clientes debe procesar 100 registros");
        assertEquals(100L, ProcesadorController.SLEEP_CLIENTES_MS, "Clientes debe dormir 100 ms por registro");

        assertEquals(50, ProcesadorController.TOTAL_PRODUCTOS, "Productos debe procesar 50 registros");
        assertEquals(150L, ProcesadorController.SLEEP_PRODUCTOS_MS, "Productos debe dormir 150 ms por registro");
    }

    @Test
    @DisplayName("Verificar cálculo matemático de progreso y porcentajes")
    void testCalculoProgresoYPorcentaje() {
        // Validación intermedia Clientes
        int actualClientes = 50;
        double progresoClientes = (double) actualClientes / ProcesadorController.TOTAL_CLIENTES;
        int porcentajeClientes = (actualClientes * 100) / ProcesadorController.TOTAL_CLIENTES;
        assertEquals(0.5, progresoClientes, 0.0001);
        assertEquals(50, porcentajeClientes);

        // Validación final Clientes
        actualClientes = 100;
        progresoClientes = (double) actualClientes / ProcesadorController.TOTAL_CLIENTES;
        porcentajeClientes = (actualClientes * 100) / ProcesadorController.TOTAL_CLIENTES;
        assertEquals(1.0, progresoClientes, 0.0001);
        assertEquals(100, porcentajeClientes);

        // Validación intermedia Productos
        int actualProductos = 25;
        double progresoProductos = (double) actualProductos / ProcesadorController.TOTAL_PRODUCTOS;
        int porcentajeProductos = (actualProductos * 100) / ProcesadorController.TOTAL_PRODUCTOS;
        assertEquals(0.5, progresoProductos, 0.0001);
        assertEquals(50, porcentajeProductos);

        // Validación final Productos
        actualProductos = 50;
        progresoProductos = (double) actualProductos / ProcesadorController.TOTAL_PRODUCTOS;
        porcentajeProductos = (actualProductos * 100) / ProcesadorController.TOTAL_PRODUCTOS;
        assertEquals(1.0, progresoProductos, 0.0001);
        assertEquals(100, porcentajeProductos);
    }

    @Test
    @DisplayName("Verificar interrupción inmediata y detención limpia de un hilo de trabajo")
    void testMecanismoInterrupcionHilo() throws InterruptedException {
        AtomicBoolean fueInterrumpido = new AtomicBoolean(false);
        AtomicInteger iteraciones = new AtomicInteger(0);

        Runnable tarea = () -> {
            for (int i = 0; i < 50; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    fueInterrumpido.set(true);
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fueInterrumpido.set(true);
                    break;
                }
                iteraciones.incrementAndGet();
            }
        };

        Thread hilo = new Thread(tarea, "Hilo-Prueba-Interrupcion");
        hilo.setDaemon(true);
        hilo.start();

        // Esperar un breve momento para que inicie la primera iteración
        Thread.sleep(150);

        // Solicitar detención
        hilo.interrupt();
        hilo.join(1000);

        assertFalse(hilo.isAlive(), "El hilo debe terminar tras la interrupción");
        assertTrue(fueInterrumpido.get(), "La bandera de interrupción debió registrarse");
        assertTrue(iteraciones.get() < 50, "El hilo no debe haber completado los 50 ciclos");
    }

    @Test
    @DisplayName("Verificar que dos hilos corren de forma concurrente e independiente")
    void testConcurrenciaIndependiente() throws InterruptedException {
        AtomicInteger contadorA = new AtomicInteger(0);
        AtomicInteger contadorB = new AtomicInteger(0);

        Thread hiloA = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {}
                contadorA.incrementAndGet();
            }
        }, "Hilo-Clientes");

        Thread hiloB = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {}
                contadorB.incrementAndGet();
            }
        }, "Hilo-Productos");

        hiloA.setDaemon(true);
        hiloB.setDaemon(true);

        hiloA.start();
        hiloB.start();

        hiloA.join(1000);
        hiloB.join(1000);

        assertEquals(5, contadorA.get());
        assertEquals(5, contadorB.get());
        assertEquals("Hilo-Clientes", hiloA.getName());
        assertEquals("Hilo-Productos", hiloB.getName());
    }
}
