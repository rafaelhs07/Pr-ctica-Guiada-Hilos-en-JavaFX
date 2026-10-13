# Evidencias Visuales de Ejecución

Este directorio está destinado a almacenar las capturas de pantalla reales obtenidas durante la ejecución de la aplicación **Monitor de Procesamiento Empresarial** de **NicaData Solutions S.A.**.

## Instrucciones para Tomar las Capturas de Pantalla

Para cumplir con la entrega académica de la práctica guiada y el reto final, ejecute la aplicación con el comando:
```powershell
mvn javafx:run
```
(O ejecute la clase principal `ni.edu.uam.practicahilos.MainApp` desde su IDE).

Tome las 4 capturas de pantalla requeridas y guárdelas en esta carpeta con los nombres sugeridos:

---

### 1. Estado Inicial (`01_estado_inicial.png`)
* **Momento:** Inmediatamente al iniciar la aplicación, antes de presionar ningún botón.
* **Qué debe observarse:**
  * Ambos paneles ("Procesamiento de Clientes" y "Procesamiento de Productos") muestran el estado **"Esperando inicio"**.
  * Barras de progreso vacías (progreso `0.0` / `0%`).
  * Contadores en `0 / 100` y `0 / 50`.
  * Botones **Iniciar** y **Reiniciar** habilitados en ambos paneles.
  * Botón **Detener** deshabilitado en ambos paneles.

---

### 2. Ambos Procesos Avanzando Simultáneamente (`02_ambos_avanzando.png`)
* **Momento:** Presione rápidamente el botón **Iniciar** en Clientes y luego **Iniciar** en Productos (aproximadamente al segundo 3 o 4).
* **Qué debe observarse:**
  * Estado **"Procesando registros..."** en ambos paneles.
  * Ambas barras de progreso avanzando activamente con valores intermedios (ej. Clientes en ~30/100 y Productos en ~25/50).
  * Botones **Detener** habilitados en ambos lados.
  * Botones **Iniciar** y **Reiniciar** deshabilitados.
  * Demuestra la ejecución concurrente multihilo de `Hilo-Clientes` y `Hilo-Productos` sin bloquear la interfaz de usuario.

---

### 3. Un Proceso Detenido Mientras el Otro Continúa (`03_proceso_detenido_otro_avanza.png`)
* **Momento:** Mientras ambos procesos están en ejecución, presione el botón **Detener** en el panel de **Productos**, dejando que **Clientes** continúe.
* **Qué debe observarse:**
  * Panel de Productos:
    * Estado: **"Proceso detenido"**.
    * La barra de progreso y contador quedan congelados en el último valor alcanzado (por ejemplo, `28 / 50` y `56%`).
    * Botón **Detener** deshabilitado; botones **Iniciar** y **Reiniciar** vuelven a estar habilitados.
  * Panel de Clientes:
    * Sigue avanzando normalmente en tiempo real (por ejemplo, `65 / 100` y `65%`).
  * Demuestra la independencia total entre hilos de trabajo y el correcto funcionamiento de `interrupt()` e `isInterrupted()`.

---

### 4. Ambos Procesos Finalizados al 100% (`04_ambos_finalizados.png`)
* **Momento:** Cuando ambos procesos concluyen todas sus iteraciones de forma natural.
* **Qué debe observarse:**
  * Panel de Clientes:
    * Estado: **"Proceso finalizado"**.
    * Contador: **"100 / 100"**.
    * Porcentaje: **"100%"**.
    * Barra de progreso completamente llena.
  * Panel de Productos:
    * Estado: **"Proceso finalizado"**.
    * Contador: **"50 / 50"**.
    * Porcentaje: **"100%"**.
    * Barra de progreso completamente llena.
  * En ambos paneles, los botones **Iniciar** y **Reiniciar** quedan habilitados para permitir nuevas simulaciones.
