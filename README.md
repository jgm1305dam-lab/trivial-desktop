Enlace Youtube: https://youtu.be/5r7VUHX47-U

# Trivial Multijugador (Cliente Desktop)

Cliente de escritorio escrito con Kotlin y Compose Multiplatform que permite jugar a un trivial en modo PVE (contra el servidor) y un modo PVP local (dos jugadores por turnos compartiendo el mismo equipo).

## Características

- Interfaz en Kotlin + Compose Multiplatform.
- Conexión por WebSocket a un servidor Ktor.
- Modo **PVE**:
  - Selección de número de preguntas (5–20).
  - Dificultad: Fácil, Media, Difícil o Mixta.
  - Selección de categorías (Ciencia, Historia, Geografía, Arte, Arte y literatura, Entretenimiento, Tecnología, Deportes, General).
  - Contador de puntos acumulados, racha y multiplicador x2 a partir de racha 5.
  - Límite de tiempo por pregunta opcional.
- Modo **PVP local**:
  - Dos jugadores (Player1 y Player2) por turnos en el mismo equipo.
  - Marcador independiente por jugador: puntos, racha, correctas.
- Estadísticas globales persistentes:
  - Partidas jugadas, máxima puntuación, racha más larga.
  - Preguntas correctas totales.
  - Porcentaje de aciertos por categoría y dificultad.
  - Ranking de jugadores por mejor puntuación.
  - Botón para **reiniciar estadísticas**.

## Requisitos

- JDK 17
- Gradle (wrapper incluido en el proyecto)
- Servidor del trivial en ejecución (ver repositorio del servidor), por defecto en `localhost:8080`.

## Puesta en marcha

```bash
# Clonar el repositorio
git clone https://github.com/tu-usuario/trivial-desktop.git
cd trivial-desktop

# Ejecutar cliente desktop
./gradlew :dev.jgonzalez.trivial.desktop:run
