# Práctica Guiada: Hilos en JavaFX y Reto Final Concurrente

## NicaData Solutions S.A. — Monitor de Procesamiento Empresarial

Este proyecto implementa la solución completa de la **"Práctica Guiada: Hilos en JavaFX"** y su **Reto Final**, cumpliendo con los estándares de diseño concurrente, actualización segura de interfaces gráficas y control del ciclo de vida de hilos en Java.

---

## 📌 1. Objetivo y Caso Empresarial

### Caso Empresarial
La empresa **NicaData Solutions S.A.** requiere procesar y validar volúmenes masivos de registros corporativos (clientes y productos). Al ejecutar estas tareas de cálculo intensivo y latencia de red/disco, una arquitectura secuencial clásica bloquearía por completo la interfaz gráfica, ocasionando que la ventana aparezca como *"No responde"*, que los eventos de clic se ignoren y que el usuario experimente una aplicación congelada.

### Solución Implementada
Se diseñó un **Monitor de Procesamiento Empresarial** interactivo en **JavaFX 21** con dos paneles independientes:
1. **Panel de Clientes (Práctica Base):** Simula el procesamiento secuencial de 100 registros con un retardo de 100 ms por registro (~10 segundos de duración total).
2. **Panel de Productos (Reto Final):** Simula el procesamiento de 50 registros con un retardo de 150 ms por registro (~7.5 segundos de duración total).

Ambos procesos se ejecutan sobre hilos dedicados de segundo plano (`Hilo-Clientes` y `Hilo-Productos`), manteniendo la ventana fluida, responsiva y capaz de procesar eventos en tiempo real.

---

## 💻 2. Requisitos del Entorno

* **Java Development Kit (JDK):** Versión 21 (LTS). Compatible con Eclipse Temurin 21 u OpenJDK 21.
* **Apache Maven:** Versión 3.8 o superior.
* **JavaFX:** Versión 21.0.6 (`javafx-controls`, `javafx-fxml`).
* **Sistema Operativo:** Windows 10/11 (o Linux / macOS).
* **Editor/IDE Recomendado:** Antigravity IDE, IntelliJ IDEA 2024+, o VS Code con Extension Pack for Java.
* **Scene Builder:** Opcional (compatible para diseño visual de `procesador-view.fxml`).

---

## 📁 3. Estructura del Proyecto

El proyecto sigue la convención estándar de Maven y el sistema de módulos de Java (JPMS):

```text
Pr-ctica-Guiada-Hilos-en-JavaFX/
├── pom.xml                                      # Configuración Maven, dependencias JavaFX y plugins
├── README.md                                    # Documentación técnica, justificación y defensa
├── .gitignore                                   # Filtro de exclusión para target/ y archivos de IDE
├── docs/
│   └── capturas/
│       └── README.md                            # Guía de evidencias para capturas de pantalla requeridas
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── module-info.java                 # Descriptor modular JPMS (requiere y abre paquetes JavaFX)
    │   │   └── ni/
    │   │       └── edu/
    │   │           └── uam/
    │   │               └── practicahilos/
    │   │                   ├── MainApp.java     # Clase de arranque de la aplicación y gancho de cierre seguro
    │   │                   └── ProcesadorController.java # Controlador FXML y lógica de hilos
    │   └── resources/
    │       └── ni/
    │           └── edu/
    │               └── uam/
    │                   └── practicahilos/
    │                       └── procesador-view.fxml # Vista declarativa FXML compatible con Scene Builder
    └── test/
        └── java/
            └── ni/
                └── edu/
                    └── uam/
                        └── practicahilos/
                            └── ProcesadorHilosTest.java # Pruebas unitarias de concurrencia e interfaz
```

---

## 🚀 4. Cómo Abrir y Ejecutar el Proyecto

### A. Configuración de Variables de Entorno en Windows (Terminal / PowerShell)

En este equipo, el JDK 21 se encuentra instalado en:
`C:\Users\RaFael\.jdks\temurin-21.0.12.1`

Y Maven está disponible a través de IntelliJ IDEA o instalación en PATH:
`C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd`

Para configurar la sesión en PowerShell:
```powershell
$env:JAVA_HOME = "C:\Users\RaFael\.jdks\temurin-21.0.12.1"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
```

### B. Comandos Maven para Compilación y Pruebas

1. **Compilar el código fuente:**
   ```powershell
   mvn clean compile
   ```

2. **Ejecutar pruebas unitarias automatizadas (8 pruebas):**
   ```powershell
   mvn test
   ```

3. **Empaquetar el archivo JAR:**
   ```powershell
   mvn package
   ```

### C. Ejecutar la Aplicación Gráfica

Ejecute directamente el plugin de JavaFX configurado en el `pom.xml`:
```powershell
mvn javafx:run
```

O si prefiere ejecutarlo mediante IntelliJ IDEA o Antigravity IDE:
* Abra la carpeta raíz en el IDE.
* Espere a que Maven sincronice las dependencias.
* Navegue a `src/main/java/ni/edu/uam/practicahilos/MainApp.java`.
* Haga clic derecho y seleccione **Run 'MainApp.main()'**.

---

## 🧵 5. Explicación Técnica de Hilos y Concurrencia

### Los Dos `Runnable` y `Thread` Independientes
* **`hiloClientes`:**
  * **Nombre:** `"Hilo-Clientes"`.
  * **Carga de trabajo:** 100 registros con `Thread.sleep(100)` en cada ciclo.
  * **Tiempo total estimado:** ~10.0 segundos.
* **`hiloProductos`:**
  * **Nombre:** `"Hilo-Productos"`.
  * **Carga de trabajo:** 50 registros con `Thread.sleep(150)` en cada ciclo.
  * **Tiempo total estimado:** ~7.5 segundos.

Ambos hilos implementan la interfaz funcional `Runnable`. Cada vez que el usuario presiona **Iniciar**, se instancia un **nuevo objeto `Thread`**:
```java
hiloClientes = new Thread(tareaClientes, "Hilo-Clientes");
hiloClientes.setDaemon(true);
hiloClientes.start();
```

---

### ¿Cuál es la diferencia entre `start()` y `run()`?

| Característica | `start()` | `run()` |
| :--- | :--- | :--- |
| **Creación de hilo** | Solicita a la JVM y al Sistema Operativo la creación de un nuevo hilo nativo con su propia pila (`call stack`). | **NO crea un nuevo hilo**. Se ejecuta como un método ordinario. |
| **Hilo en el que corre** | Hilo secundario de fondo (`Hilo-Clientes` o `Hilo-Productos`). | El hilo que realizó la invocación (en JavaFX, el **JavaFX Application Thread**). |
| **Impacto en la UI** | La interfaz permanece interactiva, libre y fluida. | **Congela por completo la interfaz de usuario** durante los 10 o 7.5 segundos. |
| **Reutilización** | Solo se puede llamar una vez por cada instancia de `Thread`. | Puede llamarse múltiples veces, pero sin concurrencia. |

> **Regla de oro:** Nunca llame a `run()` directamente para iniciar una tarea asíncrona. Siempre invoque `start()`.

---

### Uso de `sleep()`, `Platform.runLater()` e Interrupción

#### 1. `Thread.sleep()`
Pausa deliberadamente la ejecución del hilo de trabajo en segundo plano para simular latencia de procesamiento o acceso a datos. Se ejecuta exclusivamente dentro del ciclo de fondo, **nunca** en el hilo de la interfaz.

#### 2. `Platform.runLater()`
JavaFX impone un modelo de concurrencia de **hilo único para la UI**: únicamente el `JavaFX Application Thread` puede leer o modificar propiedades de nodos gráficos (`ProgressBar`, `Label`, `Button`). 

Intentar modificar un componente directamente desde un hilo secundario genera una excepción `java.lang.IllegalStateException: Not on FX application thread`. 

`Platform.runLater(Runnable)` permite enviar un mensaje o tarea a la cola de eventos de JavaFX para que se ejecute en el siguiente momento oportuno del hilo gráfico:
```java
final int procesados = i + 1;
final double progreso = (double) procesados / TOTAL_CLIENTES;
final int porcentaje = (procesados * 100) / TOTAL_CLIENTES;

Platform.runLater(() -> {
    lblContadorClientes.setText(procesados + " / " + TOTAL_CLIENTES);
    lblPorcentajeClientes.setText(porcentaje + "%");
    progressBarClientes.setProgress(progreso);
});
```

#### 3. Interrupción Cooperativa (`interrupt()` e `isInterrupted()`)
En Java, los hilos no deben destruirse forzosamente con `Thread.stop()` porque deja recursos en estados inconsistentes. La detención moderna es **cooperativa**:
1. Al presionar **Detener**, se llama a `hilo.interrupt()`.
2. Si el hilo está ejecutando código, comprueba activamente `Thread.currentThread().isInterrupted()`.
3. Si el hilo está bloqueado en `Thread.sleep()`, la llamada arroja una excepción `InterruptedException`.
4. El bloque `catch` restaura la marca con `Thread.currentThread().interrupt()`, rompe el bucle (`break`) y finaliza de manera limpia.
5. Se notifica a la UI mediante `Platform.runLater()` actualizando el estado a **"Proceso detenido"** y conservando el progreso alcanzado.

---

### Diferencia entre Detener, Reiniciar y Volver a Iniciar

1. **Detener (`onDetener`):**
   * Señaliza la interrupción del hilo activo mediante `interrupt()`.
   * El hilo finaliza su ciclo de vida y libera sus recursos.
   * La interfaz muestra **"Proceso detenido"** y **conserva** el último valor del contador y de la barra de progreso (no los borra).
   * Rehabilita los botones **Iniciar** y **Reiniciar**.
2. **Reiniciar (`onReiniciar`):**
   * Solo opera cuando el proceso **no está activo**.
   * Restablece los controles a su condición inicial: estado a **"Esperando inicio"**, contador a `0 / total`, porcentaje a `0%` y barra a `0.0`.
   * Incrementa un token de ejecución (`idEjecucion`) que descarta cualquier actualización retrasada en la cola de JavaFX.
   * **No inicia automáticamente** ninguna ejecución nueva.
3. **Volver a Iniciar (`onIniciar`):**
   * Valida que no exista un hilo activo para evitar duplicados ante clics repetidos.
   * Restablece los contadores a cero.
   * Crea una **nueva instancia** de `Thread` (los hilos terminados no pueden reiniciarse).
   * Asigna `setDaemon(true)` y arranca el hilo con `start()`.

---

## ⚠️ 6. Análisis Crítico: ¿Por qué no debemos ejecutar procesos prolongados dentro de los eventos de JavaFX?

### Explicación Arquitectónica
JavaFX utiliza el patrón arquitectónico de bucle de eventos (*Event Loop*) gestionado por un único hilo principal denominado **JavaFX Application Thread**. Este hilo es el único encargado de:
1. Escuchar y despachar eventos de entrada (movimientos de ratón, clics, pulsaciones de teclas).
2. Calcular la disposición espacial de los contenedores (*layout pass*).
3. Coordinar con la tarjeta gráfica el repintado de la pantalla (*render pulse* a ~60 cuadros por segundo).

Si un manejador de eventos ejecuta una operación bloqueante o pesada (como cálculos intensivos, consultas a bases de datos, descargas HTTP o `Thread.sleep()`), **el hilo gráfico se congela**. Mientras esté atrapado, la aplicación:
* No puede responder a clics del ratón ni a pulsaciones de teclado.
* No puede mover ni redimensionar la ventana.
* Deja de emitir pulsos de renderizado, haciendo que el sistema operativo reporte *"La aplicación no responde"*.

---

### Ejemplo de Análisis: Método Bloqueante con `Thread.sleep(5000)`

Considere el siguiente método hipotético conectado directamente al evento `onAction` de un botón:

```java
// =========================================================================
// EJEMPLO DIDÁCTICO DE ANÁLISIS (MALA PRÁCTICA - NO USAR EN PRODUCCIÓN)
// =========================================================================
@FXML
public void onProcesarBloqueante(ActionEvent event) {
    lblEstado.setText("Iniciando procesamiento..."); // [Paso 1]
    
    try {
        // ERROR GRAVE: Duerme directamente al JavaFX Application Thread por 5 segundos
        Thread.sleep(5000); // [Paso 2]
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    
    lblEstado.setText("Procesamiento finalizado con éxito"); // [Paso 3]
}
```

#### ¿Por qué este código falla visualmente y congela la aplicación?

1. **El texto intermedio nunca se ve:**
   * En el `[Paso 1]`, se modifica la propiedad de texto de `lblEstado`. Esto no dibuja los píxeles de inmediato en el monitor; simplemente marca el nodo como "sucio" (*dirty node*) y encola una solicitud de redibujado para el siguiente pulso gráfico.
2. **El pulso gráfico queda bloqueado:**
   * Inmediatamente en el `[Paso 2]`, se invoca `Thread.sleep(5000)`. Dado que este código corre sobre el `JavaFX Application Thread`, **el hilo se duerme**.
   * Durante esos 5 segundos completos, el hilo no puede atender el pulso de renderizado pendiente. Por lo tanto, el texto `"Iniciando procesamiento..."` **nunca llega a pintarse en la pantalla**.
3. **La interfaz se actualiza de golpe:**
   * Al despertar, en el `[Paso 3]`, el texto se sobrescribe directamente con `"Procesamiento finalizado con éxito"`.
   * Cuando el método finalmente retorna y el hilo queda libre, el pulso gráfico se ejecuta y dibuja únicamente el texto final. Para el usuario, el sistema se quedó congelado 5 segundos en blanco y luego saltó de golpe al final.

#### Conclusión Didáctica
Toda operación que tome más de unos pocos milisegundos debe ejecutarse en un hilo de trabajo (`Thread` + `Runnable`), comunicando el avance intermedio y final a través de `Platform.runLater()`.

---

## 🔒 7. Cierre Controlado de la Aplicación

Para evitar procesos zombis o hilos huérfanos que sigan consumiendo CPU en segundo plano tras cerrar la ventana, el proyecto cuenta con tres capas de seguridad:
1. **Hilos demonio (`setDaemon(true)`):**
   * Configurados antes de `start()`. Permite que la JVM termine si solo quedan hilos demonio activos.
2. **Interrupción explícita al cerrar ventana (`stage.setOnCloseRequest`):**
   * En `MainApp.java`, intercepta el evento de cierre de la ventana y llama a `controller.detenerAlCerrar()`.
3. **Limpieza en el ciclo de vida de la aplicación (`Application.stop()`):**
   * Se ejecuta siempre que JavaFX finaliza, garantizando un cierre limpio e idempotente.

---

## 📋 8. Estado de Entrega y Nomenclatura del Repositorio

* **Nombre del Proyecto Maven:** `PracticaHilosJavaFX` (configurado en `pom.xml`).
* **Paquete Base Java:** `ni.edu.uam.practicahilos`.
* **Repositorio Git Actual:** `Pr-ctica-Guiada-Hilos-en-JavaFX` (URL: `https://github.com/rafaelhs07/Pr-ctica-Guiada-Hilos-en-JavaFX.git`).
* **Nombre solicitado para la entrega en la guía:** `JavaFX_Hilos_Practica`.

> **📌 Nota de Entrega:**  
> Por instrucción de la práctica, se conservó intacta la configuración del repositorio Git actual (`Pr-ctica-Guiada-Hilos-en-JavaFX`). Si la rúbrica de entrega de la universidad o plataforma institucional exige exactamente el nombre `JavaFX_Hilos_Practica`, el estudiante puede renombrar el repositorio en la configuración de GitHub o crear un fork con dicho nombre antes de enviar el enlace final.

---

## 🎓 9. Puntos Clave para Defender la Práctica en Clase

Al presentar este proyecto ante el docente o evaluador:
1. **Demuestre la fluidez de la ventana:** Inicie ambos paneles y arrastre la ventana o redimensiónela mientras ambas barras avanzan para demostrar que el hilo gráfico responde fluidamente a 60 FPS.
2. **Explique la concurrencia:** Resalte que Clientes tarda ~10 segundos (100 items x 100 ms) y Productos tarda ~7.5 segundos (50 items x 150 ms). Productos termina visiblemente antes que Clientes.
3. **Muestre la independencia de hilos:** Inicie ambos procesos y presione **Detener** únicamente en Productos. Muestre cómo Productos se congela en su estado actual mientras Clientes sigue avanzando hasta el 100%.
4. **Explique `Platform.runLater()`:** Indique que ninguna llamada gráfica se realiza dentro del método `run()` del hilo de trabajo, sino que se delegan al JavaFX Application Thread mediante lambdas con variables efectivamente finales.
5. **Explique la prevención de errores por clics rápidos:** Mencione la verificación de `isAlive()` al pulsar Iniciar y el uso de `idEjecucion` para invalidar llamadas rezagadas tras un reinicio.
