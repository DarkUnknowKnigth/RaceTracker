# 🏎️ RaceTracker

**RaceTracker** es una aplicación de telemetría de alto rendimiento para Android, diseñada de manera nativa utilizando **Kotlin**, **Jetpack Compose**, y los impecables principios visuales de **Material Design 3 (M3)** con una temática exclusiva de carreras inspirada en *BMW M3 Competition*.

Este proyecto es **100% Open Source (Código Abierto)**. Cualquier desarrollador, entusiasta de las carreras o estudiante está invitado a clonar, auditar y compilar esta aplicación libremente en su propio entorno.

![RaceTracker Theme](app/src/main/res/drawable/m3_banner.png)

## 🔥 Características Principales

*   **Telemetría Inteligente (Satélite):** Utiliza `FusedLocationProviderClient` y *Foreground Services* para rastrear tu posición, procesar velocidades y acumular la distancia geodésica recorrida (`distanceTo()`) incluso cuando la pantalla está apagada.
*   **Mapa de Calor Vectorial (Heatmaps):** Integración nativa con **MapLibre**. En la pantalla de mapas, tu trayectoria no es una línea genérica; el motor renderiza mini-segmentos de mapa de calor reaccionando en vivo: azul para zonas lentas, rojo candente para zonas de alta aceleración.
*   **Dashboard M3 & LED Dinámico:** Una Interfaz de Usuario de clase mundial (UI/UX) que elimina los estorbos. Cuenta con un cronómetro en vivo e incluye un aro lumínico central (LED Dinámico) que muta gradualmente del azul al escarlata al sobrepasar los 100km/h. 
*   **Fuerzas G & Top Speed:** Detecta desaceleraciones súbitas (frenadas máximas registradas en G-Force) y despliega tus rachas de Top Speed y kilómetros completados.
*   **Compartir Logros:** Botones nativos y limpios de Android (`Intent.ACTION_SEND`) listos para disparar tu Récord Deportivo directamente a tus amigos vía WhatsApp, X, u otras aplicaciones de mensajería.
*   **Base de Datos Nativa:** Persistencia absoluta a través del motor `Room Database` de Android para mantener el registro fiel de todas tus sesiones de competencia.

---

## 🛠️ Cómo Compilar e Instalar (Open Source)

Dado que este proyecto está liberado, todo lo que necesitas es descargar tu copia de **Android Studio** para volverlo realidad en tu teléfono.

### Requisitos Mínimos
- **Android Studio** (Giraffe, Hedgehog, Iguana o superior)
- SDK de **Android 13+ (API 33)** o superior.
- Un dispositivo Android físico o emulador con servicios de ubicación.

### Instrucciones de Instalación

1.  **Clona este repositorio** en tu computadora:
    ```bash
    git clone https://github.com/DarkUnknowKnigth/RaceTracker.git
    ```
2.  Abre **Android Studio**.
3.  Selecciona **File > Open...** y localiza la carpeta donde clonaste el código (`RaceTracker`).
4.  Permite que el entorno de desarrollo descargue todas las dependencias modernas (`androidx.compose.material:material-icons-extended`, `org.maplibre.android`, etc). Puedes forzar esto dando clic en el botón superior derecho de "Escarabajo/Sincronizar": **Sync Project with Gradle Files**.
5.  Conecta tu teléfono Android vía USB (con *USB Debugging* activado) o inicia un Emulador.
6.  Haz clic en el botón verde de **Play (▶ Run 'app')** en la barra de herramientas superior.

¡Listo! La aplicación se instalará en tu terminal y estará completamente funcional.

## 🤝 Contribuir
RaceTracker está vivo y siempre busca bajar milésimas de segundo en su código. Si sabes cómo perfeccionar el filtro *Moving Average* del GPS, quieres inyectar soporte para OBD2, o mejorar la precisión del motor, ¡los *Pull Requests* son siempre bienvenidos!

---
> Compilado, diseñado y balanceado para el máximo rendimiento.
