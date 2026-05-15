# FlapyBirth - Flappy Bird 2 Jugadores

## Integrantes

- Douglas Padilla

## Descripción

Implementación del juego clásico Flappy Bird con soporte para **dos jugadores simultáneos** en la misma ventana. Construido con Java 21, LWJGL 3.3.3, OpenGL 3.3 core profile y OpenAL para audio. Todos los gráficos se generan proceduralmente con primitivas OpenGL (rectángulos, triángulos, círculos) sin imágenes externas. Los sonidos se generan matemáticamente con ondas sinusoidales, sin archivos de audio.

## Controles

| Acción | Jugador 1 | Jugador 2 |
|---|---|---|
| Saltar | `ESPACIO` | `W` o `FLECHA ARRIBA` |
| Iniciar / Menú | `ENTER` | `ENTER` |
| Salir | `ESC` | `ESC` |

## Instrucciones de compilación y ejecución

### Requisitos previos

- Java 21 o superior
- Maven 3.6 o superior

### Ejecutar directamente

```bash
mvn compile exec:java
```

### Generar JAR ejecutable

```bash
mvn package
java -jar target/flappy-bird-1.0.jar
```

## Descripción de cambios realizados

### 2.1 Pájaro compuesto por figuras geométricas

El pájaro está construido con 5 figuras distintas dibujadas en OpenGL:

- **Cola**: triángulo en el lado izquierdo (tono más oscuro)
- **Cuerpo**: rectángulo central con el color del jugador
- **Ala**: rectángulo animado que oscila verticalmente con función seno (aleteo continuo)
- **Pico**: triángulo naranja apuntando hacia la derecha
- **Ojo**: círculo blanco con pupila negra (detalle interno)

El pájaro se **inclina visualmente** según su velocidad vertical: cuando sube, el pico apunta arriba; cuando cae, apunta abajo. Esto se logra desplazando individualmente el pico, la cola y el ojo según `velocidadY`.

### 2.2 Modo de dos jugadores simultáneos

- Jugador 1 (amarillo): `ESPACIO`
- Jugador 2 (azul): `W` o `FLECHA ARRIBA`
- Cada jugador tiene posición, velocidad, estado y puntaje independientes
- Las tuberías son compartidas entre ambos jugadores
- El juego termina solo cuando **ambos** pájaros han muerto
- Mientras uno siga vivo, la partida continúa
- HUD diferenciado: puntaje de J1 en amarillo, J2 en azul

### 2.3 Incremento progresivo de la velocidad

- Velocidad inicial: 200 px/s
- Cada 5 puntos se sube 1 nivel y la velocidad aumenta 50 px/s
- Velocidad máxima: 500 px/s (para que el juego sea jugable)
- El nivel se muestra en el HUD y en el título de la ventana en tiempo real

### 2.4 Mejora de la interfaz del juego

- **Fondo con degradado**: cielo azul en dos tonos (claro arriba, oscuro abajo)
- **Montañas**: cuatro triángulos marrones/grises en el horizonte
- **Nubes con parallax**: cinco nubes que se mueven a velocidades distintas
- **Suelo**: banda verde con borde oscuro en la base
- **Sonido procedural**: tres efectos generados matemáticamente con OpenAL
  - Salto: tono La5 (880 Hz), corto y agudo
  - Punto anotado: tono Do6 (1047 Hz), más agudo
  - Game over: tono descendente de 440 Hz a 110 Hz
- **Pantalla de menú**: panel con título, instrucciones y pájaros de muestra
- **Pantalla de game over**: muestra puntajes finales y declara al ganador
- **HUD con fuente pixel**: texto dibujado con rectángulos (sin fuentes externas)
