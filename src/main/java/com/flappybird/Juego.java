package com.flappybird;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Clase Juego - Orquestador central del juego Flappy Bird
 *
 * Maneja el estado del juego (MENU, JUGANDO, GAME_OVER), los dos pájaros,
 * la lista de tuberías, los puntajes, la dificultad progresiva y la detección
 * de colisiones usando AABB (Axis-Aligned Bounding Box).
 */
public class Juego {

    // --- Estados posibles del juego ---
    public enum Estado {
        MENU,       // Pantalla de inicio esperando que el jugador presione ENTER
        JUGANDO,    // Juego activo con pájaros y tuberías en movimiento
        GAME_OVER   // Ambos jugadores muertos, mostrando puntajes finales
    }

    // Estado actual del juego
    private Estado estado;

    // Los dos pájaros: jugador 1 (amarillo) y jugador 2 (azul)
    private final Pajaro jugador1;
    private final Pajaro jugador2;

    // Lista de tuberías actualmente en pantalla
    private final List<Tuberia> tuberias;

    // Generador de números aleatorios para posicionar las tuberías
    private final Random random;

    // Puntajes individuales de cada jugador
    private int puntajeJ1 = 0;
    private int puntajeJ2 = 0;

    // Velocidad actual de las tuberías (aumenta con la dificultad)
    private float velocidadTuberias = 200.0f;

    // Velocidad base inicial
    private static final float VELOCIDAD_BASE = 200.0f;

    // Velocidad máxima para no hacer el juego imposible
    private static final float VELOCIDAD_MAXIMA = 500.0f;

    // Nivel actual de dificultad (se muestra en la interfaz)
    private int nivel = 1;

    // Temporizador para controlar cuándo aparece la siguiente tubería
    private float timerTuberia = 0.0f;

    // Cada cuántos segundos aparece una nueva tubería
    private static final float INTERVALO_TUBERIAS = 2.5f;

    // Posición X donde aparecen las tuberías (fuera de pantalla, a la derecha)
    private static final float X_SPAWN_TUBERIA = 820.0f;

    // Límites del hueco central de las tuberías (para que no sea imposible)
    private static final float HUECO_MIN_Y = 150.0f;  // Mínimo Y del centro del hueco
    private static final float HUECO_MAX_Y = 450.0f;  // Máximo Y del centro del hueco

    // Posiciones iniciales de los pájaros
    private static final float J1_X_INICIO = 150.0f;
    private static final float J1_Y_INICIO = 300.0f;
    private static final float J2_X_INICIO = 100.0f;
    private static final float J2_Y_INICIO = 300.0f;

    /**
     * Constructor: inicializa los jugadores y el estado inicial
     */
    public Juego() {
        // Jugador 1: amarillo/naranja
        jugador1 = new Pajaro(J1_X_INICIO, J1_Y_INICIO, 1.0f, 0.8f, 0.0f);

        // Jugador 2: azul claro
        jugador2 = new Pajaro(J2_X_INICIO, J2_Y_INICIO, 0.3f, 0.7f, 1.0f);

        tuberias = new ArrayList<>();
        random = new Random();

        // El juego comienza en el menú principal
        estado = Estado.MENU;
    }

    /**
     * Actualiza toda la lógica del juego en cada frame
     *
     * @param deltaTime Tiempo transcurrido desde el último frame (segundos)
     */
    public void actualizar(float deltaTime) {
        // Solo actualizar la lógica cuando el juego está activo
        if (estado != Estado.JUGANDO) return;

        // Actualizar física de los pájaros
        jugador1.actualizar(deltaTime);
        jugador2.actualizar(deltaTime);

        // Verificar que los pájaros no salgan por arriba ni por abajo
        verificarLimitesPantalla();

        // Controlar la aparición de nuevas tuberías
        timerTuberia += deltaTime;
        if (timerTuberia >= INTERVALO_TUBERIAS) {
            timerTuberia = 0.0f;
            generarTuberia();
        }

        // Actualizar cada tubería y verificar colisiones y puntajes
        Iterator<Tuberia> it = tuberias.iterator();
        while (it.hasNext()) {
            Tuberia t = it.next();
            t.actualizar(deltaTime);

            // Eliminar tuberías que ya salieron por la izquierda
            if (t.fueraDePantalla()) {
                it.remove();
                continue;
            }

            // Verificar si los jugadores pasaron la tubería (sumar puntaje)
            if (jugador1.estaVivo() && t.verificarPasoJugador1(jugador1.getX())) {
                puntajeJ1++;
                actualizarDificultad();
            }
            if (jugador2.estaVivo() && t.verificarPasoJugador2(jugador2.getX())) {
                puntajeJ2++;
                actualizarDificultad();
            }

            // Detectar colisiones AABB entre cada pájaro y la tubería
            if (jugador1.estaVivo() && t.colisiona(
                    jugador1.getX(), jugador1.getY(),
                    jugador1.getAncho(), jugador1.getAlto())) {
                jugador1.morir();
            }
            if (jugador2.estaVivo() && t.colisiona(
                    jugador2.getX(), jugador2.getY(),
                    jugador2.getAncho(), jugador2.getAlto())) {
                jugador2.morir();
            }
        }

        // El juego termina cuando ambos jugadores están muertos
        if (!jugador1.estaVivo() && !jugador2.estaVivo()) {
            estado = Estado.GAME_OVER;
        }
    }

    /**
     * Dibuja todos los elementos del juego según el estado actual
     *
     * @param renderer Renderer para dibujar primitivas
     */
    public void dibujar(Renderer renderer) {
        // --- FONDO ---
        // Degradado de azul cielo simulado con dos rectángulos de distinto tono
        renderer.dibujarRect(0, 300, 800, 300, 0.4f, 0.7f, 1.0f);  // Mitad superior (más claro)
        renderer.dibujarRect(0, 0,   800, 300, 0.3f, 0.55f, 0.9f); // Mitad inferior (más oscuro)

        // Nubes decorativas (rectángulos blancos simples)
        renderer.dibujarRect(100, 480, 80, 25, 0.95f, 0.95f, 0.95f);
        renderer.dibujarRect(300, 520, 100, 20, 0.95f, 0.95f, 0.95f);
        renderer.dibujarRect(600, 490, 90, 22, 0.95f, 0.95f, 0.95f);

        // Suelo (rectángulo café/verde en la base)
        renderer.dibujarRect(0, 0, 800, 30, 0.4f, 0.6f, 0.2f);

        if (estado == Estado.MENU) {
            dibujarMenu(renderer);
        } else if (estado == Estado.JUGANDO || estado == Estado.GAME_OVER) {
            // Dibujar tuberías
            for (Tuberia t : tuberias) {
                t.dibujar(renderer);
            }

            // Dibujar los pájaros
            jugador1.dibujar(renderer);
            jugador2.dibujar(renderer);

            // Dibujar HUD (puntajes y nivel)
            dibujarHUD(renderer);

            if (estado == Estado.GAME_OVER) {
                dibujarGameOver(renderer);
            }
        }
    }

    /**
     * Dibuja la pantalla de menú principal con texto e instrucciones
     */
    private void dibujarMenu(Renderer renderer) {
        // Panel central del menú con borde
        renderer.dibujarRect(150, 170, 500, 260, 0.05f, 0.05f, 0.2f);
        renderer.dibujarRect(155, 175, 490, 250, 0.1f, 0.1f, 0.35f);

        // Título "FLAPPY BIRD" en blanco, centrado
        float anchoTitulo = TextoPixel.anchoTexto("FLAPPY BIRD", 2);
        TextoPixel.dibujar(renderer, "FLAPPY BIRD",
            400 - anchoTitulo / 2, 370, 2,
            1.0f, 1.0f, 0.2f);

        // Subtítulo "2 JUGADORES"
        float anchoSub = TextoPixel.anchoTexto("2 JUGADORES", 1);
        TextoPixel.dibujar(renderer, "2 JUGADORES",
            400 - anchoSub / 2, 335, 1,
            0.9f, 0.9f, 0.9f);

        // Instrucciones jugador 1 en amarillo
        TextoPixel.dibujar(renderer, "J1: ESPACIO",
            175, 300, 1,
            1.0f, 0.8f, 0.0f);

        // Instrucciones jugador 2 en azul
        TextoPixel.dibujar(renderer, "J2: W",
            175, 275, 1,
            0.3f, 0.7f, 1.0f);

        // Instrucción para iniciar
        float anchoEnter = TextoPixel.anchoTexto("ENTER: INICIAR", 1);
        TextoPixel.dibujar(renderer, "ENTER: INICIAR",
            400 - anchoEnter / 2, 210, 1,
            0.3f, 1.0f, 0.3f);

        // Dibujar los dos pájaros de muestra en el menú
        jugador1.dibujar(renderer);
        jugador2.dibujar(renderer);
    }

    /**
     * Dibuja el HUD con puntajes y nivel durante el juego usando texto pixel
     */
    private void dibujarHUD(Renderer renderer) {
        // Barra superior negra de fondo
        renderer.dibujarRect(0, 565, 800, 35, 0.0f, 0.0f, 0.0f);

        // Puntaje jugador 1 en amarillo
        TextoPixel.dibujar(renderer,
            "J1:" + puntajeJ1,
            10, 572, 1,
            1.0f, 0.8f, 0.0f);

        // Puntaje jugador 2 en azul
        TextoPixel.dibujar(renderer,
            "J2:" + puntajeJ2,
            120, 572, 1,
            0.3f, 0.7f, 1.0f);

        // Nivel en verde, centrado
        String textoNivel = "NIVEL:" + nivel;
        float anchoNivel = TextoPixel.anchoTexto(textoNivel, 1);
        TextoPixel.dibujar(renderer,
            textoNivel,
            400 - anchoNivel / 2, 572, 1,
            0.2f, 1.0f, 0.2f);

        // Instrucción ESC a la derecha
        TextoPixel.dibujar(renderer, "ESC:SALIR",
            590, 572, 1,
            0.7f, 0.7f, 0.7f);
    }

    /**
     * Dibuja la pantalla de Game Over con puntajes finales en texto
     */
    private void dibujarGameOver(Renderer renderer) {
        // Overlay oscuro
        renderer.dibujarRect(150, 160, 500, 280, 0.05f, 0.05f, 0.15f);
        renderer.dibujarRect(155, 165, 490, 270, 0.08f, 0.08f, 0.25f);

        // Título "GAME OVER" en rojo, centrado
        float anchoGO = TextoPixel.anchoTexto("GAME OVER", 2);
        TextoPixel.dibujar(renderer, "GAME OVER",
            400 - anchoGO / 2, 385, 2,
            1.0f, 0.15f, 0.15f);

        // Puntaje jugador 1
        TextoPixel.dibujar(renderer,
            "J1: " + puntajeJ1 + " PUNTOS",
            175, 330, 1,
            1.0f, 0.8f, 0.0f);

        // Puntaje jugador 2
        TextoPixel.dibujar(renderer,
            "J2: " + puntajeJ2 + " PUNTOS",
            175, 305, 1,
            0.3f, 0.7f, 1.0f);

        // Determinar ganador
        String ganador;
        float gr, gg, gb;
        if (puntajeJ1 > puntajeJ2) {
            ganador = "GANA J1!";
            gr = 1.0f; gg = 0.8f; gb = 0.0f;
        } else if (puntajeJ2 > puntajeJ1) {
            ganador = "GANA J2!";
            gr = 0.3f; gg = 0.7f; gb = 1.0f;
        } else {
            ganador = "EMPATE!";
            gr = 1.0f; gg = 1.0f; gb = 1.0f;
        }
        float anchoGanador = TextoPixel.anchoTexto(ganador, 2);
        TextoPixel.dibujar(renderer, ganador,
            400 - anchoGanador / 2, 260, 2,
            gr, gg, gb);

        // Instrucción para volver al menú
        float anchoVolver = TextoPixel.anchoTexto("ENTER: MENU", 1);
        TextoPixel.dibujar(renderer, "ENTER: MENU",
            400 - anchoVolver / 2, 195, 1,
            0.3f, 1.0f, 0.3f);
    }

    /**
     * Genera una nueva tubería con posición aleatoria del hueco
     */
    private void generarTuberia() {
        // Posición aleatoria del centro del hueco entre los límites definidos
        float centroHueco = HUECO_MIN_Y + random.nextFloat() * (HUECO_MAX_Y - HUECO_MIN_Y);

        // Crear la tubería fuera de pantalla a la derecha con la velocidad actual
        tuberias.add(new Tuberia(X_SPAWN_TUBERIA, centroHueco, velocidadTuberias));
    }

    /**
     * Verifica que los pájaros no salgan por los bordes superior e inferior
     */
    private void verificarLimitesPantalla() {
        // Límite inferior: el suelo (y=30 por el rectángulo del suelo)
        if (jugador1.estaVivo() && jugador1.getY() < 30) {
            jugador1.morir();
        }
        if (jugador2.estaVivo() && jugador2.getY() < 30) {
            jugador2.morir();
        }

        // Límite superior: techo de la ventana (y=600)
        if (jugador1.estaVivo() && jugador1.getY() + jugador1.getAlto() > 565) {
            jugador1.morir();
        }
        if (jugador2.estaVivo() && jugador2.getY() + jugador2.getAlto() > 565) {
            jugador2.morir();
        }
    }

    /**
     * Actualiza la velocidad y el nivel según el puntaje total acumulado
     * Cada 5 puntos se sube de nivel y se incrementa la velocidad en 50 px/s
     */
    private void actualizarDificultad() {
        // Usar el puntaje mayor entre los dos jugadores para definir el nivel
        int puntajeMaximo = Math.max(puntajeJ1, puntajeJ2);

        // Calcular el nivel actual basado en el puntaje (cada 5 puntos = 1 nivel)
        int nuevoNivel = 1 + puntajeMaximo / 5;

        if (nuevoNivel != nivel) {
            nivel = nuevoNivel;

            // Calcular nueva velocidad y aplicar el máximo
            velocidadTuberias = Math.min(VELOCIDAD_BASE + (nivel - 1) * 50, VELOCIDAD_MAXIMA);

            // Actualizar la velocidad de todas las tuberías existentes
            for (Tuberia t : tuberias) {
                t.setVelocidad(velocidadTuberias);
            }
        }
    }

    /**
     * Reinicia el juego al estado inicial (para jugar de nuevo)
     */
    private void reiniciar() {
        // Reiniciar pájaros a sus posiciones iniciales
        jugador1.reiniciar(J1_X_INICIO, J1_Y_INICIO);
        jugador2.reiniciar(J2_X_INICIO, J2_Y_INICIO);

        // Limpiar todas las tuberías
        tuberias.clear();

        // Resetear puntajes y dificultad
        puntajeJ1 = 0;
        puntajeJ2 = 0;
        nivel = 1;
        velocidadTuberias = VELOCIDAD_BASE;
        timerTuberia = 0.0f;
    }

    // --- Métodos llamados por GestorEntrada ---

    /** El jugador 1 intenta saltar */
    public void saltarJugador1() {
        if (estado == Estado.JUGANDO) {
            jugador1.saltar();
        }
    }

    /** El jugador 2 intenta saltar */
    public void saltarJugador2() {
        if (estado == Estado.JUGANDO) {
            jugador2.saltar();
        }
    }

    /**
     * Reacciona al ENTER según el estado actual del juego
     * MENU → inicia el juego, GAME_OVER → vuelve al menú
     */
    public void presionarEnter() {
        switch (estado) {
            case MENU:
                // Pasar del menú al juego activo
                reiniciar();
                estado = Estado.JUGANDO;
                break;
            case GAME_OVER:
                // Volver al menú principal desde game over
                estado = Estado.MENU;
                reiniciar();
                break;
            case JUGANDO:
                // No hace nada mientras se juega
                break;
        }
    }

    // --- Getters para la ventana ---

    public int getPuntajeJ1() { return puntajeJ1; }
    public int getPuntajeJ2() { return puntajeJ2; }
    public int getNivel()     { return nivel; }
    public Estado getEstado() { return estado; }
}
