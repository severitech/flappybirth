package com.flappybird;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Clase Juego - Orquestador central del juego Flappy Bird
 *
 * Maneja el estado del juego (MENU, JUGANDO, GAME_OVER), los dos pájaros,
 * la lista de tuberías, los puntajes, la dificultad progresiva, la detección
 * de colisiones AABB, el fondo animado con parallax y los efectos de sonido.
 */
public class Juego {

    // --- Estados posibles del juego ---
    public enum Estado {
        MENU,       // Pantalla de inicio esperando que el jugador presione ENTER
        JUGANDO,    // Juego activo con pájaros y tuberías en movimiento
        GAME_OVER   // Ambos jugadores muertos, mostrando puntajes finales
    }

    // Estado actual del juego (comienza en MENU)
    private Estado estado;

    // Los dos pájaros: jugador 1 (amarillo/naranja) y jugador 2 (azul)
    private final Pajaro jugador1;
    private final Pajaro jugador2;
    private final Pajaro jugador3;

    // Lista de tuberías actualmente visibles en pantalla
    private final List<Tuberia> tuberias;

    // Generador de números aleatorios para la posición del hueco de las tuberías
    private final Random random;

    // Gestor de sonido para reproducir efectos de audio
    private GestorSonido gestorSonido;

    // Puntajes individuales de cada jugador (se incrementan al pasar tuberías)
    private int puntajeJ1 = 0;
    private int puntajeJ2 = 0;
    private int puntajeJ3 = 0;

    // Velocidad actual de las tuberías en NDC/s : 0.62f
    private float velocidadTuberias = 0.62f;

    // Velocidad base al inicio de cada partida en NDC/s
    private static final float VELOCIDAD_BASE = 0.62f;

    // Velocidad máxima en NDC/s 
    private static final float VELOCIDAD_MAXIMA = 1.5f;

    // Nivel actual de dificultad (visible en el HUD y el título de la ventana)
    private int nivel = 1;

    // Temporizador para controlar cuándo aparece la siguiente tubería
    private float timerTuberia = 0.0f;

    // Segundos entre tuberías : TIEMPO_ENTRE_TUBERIAS = 1.5f
    private static final float INTERVALO_TUBERIAS = 1.5f;

    // Spawn de tuberías fuera de pantalla a la derecha .: 1.2f
    private static final float X_SPAWN_TUBERIA = 1.2f;

    // Rango vertical del centro del hueco en NDC
    // GAP_MIN_CENTRO = -0.45f, GAP_MAX_CENTRO = 0.45f
    private static final float HUECO_MIN_Y = -0.45f;
    private static final float HUECO_MAX_Y =  0.45f;

    // Posiciones iniciales de los pájaros en NDC
    // J1: borde izquierdo en x=-0.50 
    // J2: separado a la izquierda para que no se superpongan
    private static final float J1_X_INICIO = -0.55f;
    private static final float J1_Y_INICIO = -0.05f;  // centro en 0.0 (mitad de pantalla)
    private static final float J2_X_INICIO = -0.68f;
    private static final float J2_Y_INICIO = -0.05f;
    private static final float J3_X_INICIO = -0.80f;
    private static final float J3_Y_INICIO = -0.05f;

    private static final int PUNTOS_QUE_DETERMINA_EL_GANADOR = 2;

    // Bandera para reproducir el sonido de game over una sola vez
    private boolean sonidoGameOverReproducido = false;

    // --- Nube parallax en NDC: cada nube tiene X, Y, ancho, alto y velocidad ---
    // Conversión de píxeles a NDC: x_ndc = x_px/400-1, y_ndc = y_px/300-1
    //                               w_ndc = w_px/400,   h_ndc = h_px/300
    //                               vel_ndc = vel_px/400

    // Posiciones X iniciales en NDC (actualizadas cada frame)
    private final float[] nubesPosX = {-0.75f, -0.25f, 0.50f, -0.875f, 0.125f};

    // Posiciones Y fijas en NDC
    private final float[] nubesPosY = {0.633f, 0.750f, 0.667f, 0.700f, 0.717f};

    // Anchos en NDC
    private final float[] nubesAncho = {0.200f, 0.263f, 0.225f, 0.175f, 0.288f};

    // Altos en NDC
    private final float[] nubesAlto  = {0.073f, 0.060f, 0.083f, 0.067f, 0.053f};

    // Velocidades en NDC/s (efecto parallax: distintas velocidades por nube)
    private final float[] nubesVel   = {0.050f, 0.063f, 0.045f, 0.055f, 0.070f};

    /**
     * Constructor: inicializa los jugadores, la lista de tuberías y el estado inicial.
     */
    public Juego() {
        // Crear jugador 1 con color amarillo/naranja
        jugador1 = new Pajaro(J1_X_INICIO, J1_Y_INICIO, 1.0f, 0.8f, 0.0f);

        // Crear jugador 2 con color azul claro
        jugador2 = new Pajaro(J2_X_INICIO, J2_Y_INICIO, 0.3f, 0.7f, 1.0f);

        //jugador 3 con color blanco
        jugador3 = new Pajaro(J3_X_INICIO, J3_Y_INICIO, 1.0f, 1.0f, 1.0f);

        // Inicializar la lista de tuberías vacía
        tuberias = new ArrayList<>();

        // Inicializar el generador de números aleatorios
        random = new Random();

        // El juego arranca siempre en la pantalla de menú
        estado = Estado.MENU;
    }

    /**
     * Asigna el gestor de sonido al juego para reproducir efectos.
     *
     * @param gestorSonido Instancia ya inicializada del gestor de sonido
     */
    public void setGestorSonido(GestorSonido gestorSonido) {
        // Guardar la referencia para usarla al saltar, puntuar y en game over
        this.gestorSonido = gestorSonido;
    }

    /**
     * Actualiza toda la lógica del juego en cada frame del game loop.
     *
     * @param deltaTime Tiempo transcurrido desde el último frame en segundos
     */
    public void actualizar(float deltaTime) {
        // Actualizar nubes siempre (incluso en menú) para que se vea animado
        actualizarNubes(deltaTime);

        // Si no estamos jugando, no actualizar la lógica de juego
        if (estado != Estado.JUGANDO) return;

        // Actualizar la física de ambos pájaros
        jugador1.actualizar(deltaTime);
        jugador2.actualizar(deltaTime);
        jugador3.actualizar(deltaTime);

        // Verificar que ningún pájaro salga por los bordes de la pantalla
        verificarLimitesPantalla();

        // Controlar la aparición de nuevas tuberías con el temporizador
        timerTuberia += deltaTime;
        if (timerTuberia >= INTERVALO_TUBERIAS) {
            timerTuberia = 0.0f;   // Reiniciar el contador
            generarTuberia();       // Crear una nueva tubería
        }

        // Iterar sobre las tuberías para actualizarlas y detectar interacciones
        Iterator<Tuberia> it = tuberias.iterator();
        while (it.hasNext()) {
            Tuberia t = it.next();

            // Mover la tubería hacia la izquierda
            t.actualizar(deltaTime);

            // Eliminar la tubería si ya salió por el borde izquierdo
            if (t.fueraDePantalla()) {
                it.remove();
                continue;
            }

            // Verificar si el jugador 1 pasó esta tubería para sumarle un punto
            if (jugador1.estaVivo() && t.verificarPasoJugador1(jugador1.getX())) {
                puntajeJ1++;                          // Incrementar puntaje de J1
                actualizarDificultad();               // Recalcular nivel y velocidad
                reproducirSonidoPunto();                           // Reproducir efecto de audio
                verificarFinDelJuego();
            }

            // Verificar si el jugador 2 pasó esta tubería
            if (jugador2.estaVivo() && t.verificarPasoJugador2(jugador2.getX())) {
                puntajeJ2++;                          // Incrementar puntaje de J2
                actualizarDificultad();               // Recalcular nivel y velocidad
                reproducirSonidoPunto();                // Reproducir efecto de audio
                verificarFinDelJuego();
            }

            if (jugador3.estaVivo() && t.verificarPasoJugador3(jugador3.getX())) {
                puntajeJ3++;                          // Incrementar puntaje de J3
                actualizarDificultad();               // Recalcular nivel y velocidad
                reproducirSonidoPunto();              // Reproducir efecto de audio
                verificarFinDelJuego();
            }

            // Detectar colisión AABB entre el jugador 1 y esta tubería
            if (jugador1.estaVivo() && t.colisiona(
                    jugador1.getX(), jugador1.getY(),
                    jugador1.getAncho(), jugador1.getAlto())) {
                jugador1.morir();   // Matar al jugador 1 si choca
            }

            // Detectar colisión AABB entre el jugador 2 y esta tubería
            if (jugador2.estaVivo() && t.colisiona(
                    jugador2.getX(), jugador2.getY(),
                    jugador2.getAncho(), jugador2.getAlto())) {
                jugador2.morir();   // Matar al jugador 2 si choca
            }

            // Detectar colisión AABB entre el jugador 3 y esta tubería
            if (jugador3.estaVivo() && t.colisiona(
                    jugador3.getX(), jugador3.getY(),
                    jugador3.getAncho(), jugador3.getAlto())) {
                jugador3.morir();   // Matar al jugador 3 si choca
            }

            
        }

        // Verificar si todos los jugadores están muertos para terminar la partida
        if (!jugador1.estaVivo() && !jugador2.estaVivo() && !jugador3.estaVivo()) {
            // Solo reproducir el sonido de game over la primera vez
            if (!sonidoGameOverReproducido) {
                reproducirSonidoGameOver();           // Reproducir efecto de audio
                sonidoGameOverReproducido = true;     // Marcar como reproducido
            }
            estado = Estado.GAME_OVER;                // Cambiar al estado de game over
        }
    }

    private void verificarFinDelJuego(){
        if (puntajeJ1 == PUNTOS_QUE_DETERMINA_EL_GANADOR || puntajeJ2 == PUNTOS_QUE_DETERMINA_EL_GANADOR || puntajeJ3 == PUNTOS_QUE_DETERMINA_EL_GANADOR) {
            estado = Estado.GAME_OVER;
        }
    }

    /**
     * Actualiza las posiciones X de las nubes para crear el efecto parallax.
     * Las nubes a diferentes velocidades generan sensación de profundidad.
     *
     * @param deltaTime Tiempo del frame en segundos
     */
    private void actualizarNubes(float deltaTime) {
        for (int i = 0; i < nubesPosX.length; i++) {
            // Mover la nube hacia la izquierda según su velocidad individual
            nubesPosX[i] -= nubesVel[i] * deltaTime;

            // Si la nube salió completamente por la izquierda, reaparece por la derecha
            if (nubesPosX[i] + nubesAncho[i] < -1.0f) {
                nubesPosX[i] = 1.05f;  // Reaparece fuera del borde derecho en NDC
            }
        }
    }

    /**
     * Dibuja todos los elementos del juego según el estado actual.
     * El orden de dibujo determina qué queda detrás (primero = más atrás).
     *
     * @param renderer Renderer para dibujar primitivas OpenGL
     */
    public void dibujar(Renderer renderer) {
        // === 1. CIELO en NDC ===
        // Mitad superior: azul claro  (y de 0 a +1)
        renderer.dibujarRect(-1f, 0f, 2f, 1f, 0.40f, 0.70f, 1.00f);
        // Mitad inferior: azul oscuro (y de -1 a 0)
        renderer.dibujarRect(-1f, -1f, 2f, 1f, 0.30f, 0.55f, 0.90f);

        // === 2. MONTAÑAS en NDC ===
        // Conversión: x_ndc = x_px/400-1  |  y_ndc = y_px/300-1

        // Montaña 1 — marrón oscuro
        renderer.dibujarTriangulo(
            -1.050f, -0.900f,   // (-20px, 30px)
             -0.675f, -0.450f,  // (130px, 165px) pico
             -0.300f, -0.900f,  // (280px, 30px)
            0.38f, 0.32f, 0.28f
        );
        // Montaña 2 — gris marrón
        renderer.dibujarTriangulo(
            -0.450f, -0.900f,   // (220px, 30px)
             -0.050f, -0.367f,  // (380px, 190px) pico
              0.350f, -0.900f,  // (540px, 30px)
            0.32f, 0.28f, 0.26f
        );
        // Montaña 3 — marrón claro
        renderer.dibujarTriangulo(
             0.150f, -0.900f,   // (460px, 30px)
             0.500f, -0.533f,   // (600px, 140px) pico
             0.850f, -0.900f,   // (740px, 30px)
            0.42f, 0.36f, 0.30f
        );
        // Montaña 4 — gris oscuro (borde derecho)
        renderer.dibujarTriangulo(
             0.700f, -0.900f,   // (680px, 30px)
             1.050f, -0.417f,   // (820px, 175px) pico
             1.400f, -0.900f,   // (960px, 30px)
            0.35f, 0.30f, 0.27f
        );

        // === 3. NUBES PARALLAX en NDC ===
        for (int i = 0; i < nubesPosX.length; i++) {
            renderer.dibujarRect(
                nubesPosX[i], nubesPosY[i], nubesAncho[i], nubesAlto[i],
                0.95f, 0.95f, 0.98f
            );
            // Capa superior de la nube para efecto de volumen
            renderer.dibujarRect(
                nubesPosX[i] + nubesAncho[i] * 0.15f,
                nubesPosY[i] + nubesAlto[i] * 0.5f,
                nubesAncho[i] * 0.60f,
                nubesAlto[i] * 0.70f,
                0.98f, 0.98f, 1.00f
            );
        }

        // === 4. SUELO en NDC ===
        // 30px alto → 30*(2/600) = 0.10 NDC  |  desde y=-1.0 hasta y=-0.90
        renderer.dibujarRect(-1f, -1.0f,  2f, 0.100f, 0.35f, 0.58f, 0.18f);
        // Línea de contraste: 4px → 0.013 NDC  |  28px → y=-0.907
        renderer.dibujarRect(-1f, -0.907f, 2f, 0.013f, 0.25f, 0.42f, 0.12f);

        // === 5. ELEMENTOS DE JUEGO (según el estado actual) ===

        if (estado == Estado.MENU) {
            // Mostrar pantalla de menú principal
            dibujarMenu(renderer);

        } else if (estado == Estado.JUGANDO || estado == Estado.GAME_OVER) {
            // Dibujar todas las tuberías activas
            for (Tuberia t : tuberias) {
                t.dibujar(renderer);
            }

            // Dibujar ambos pájaros
            jugador1.dibujar(renderer);
            jugador2.dibujar(renderer);
            jugador3.dibujar(renderer);
            // Dibujar el HUD con puntajes y nivel
            dibujarHUD(renderer);

            // Si el juego terminó, mostrar la pantalla de game over encima
            if (estado == Estado.GAME_OVER) {
                dibujarGameOver(renderer);
            }
        }
    }

    /**
     * Dibuja la pantalla de menú principal con título, instrucciones y los pájaros.
     *
     * @param renderer Renderer para dibujar primitivas
     */
    private void dibujarMenu(Renderer renderer) {
        // Panel fondo — (150,170,500,260)px → NDC: x=-0.625, y=-0.433, w=1.25, h=0.867
        renderer.dibujarRect(-0.625f, -0.433f, 1.25f, 0.867f, 0.05f, 0.05f, 0.20f);
        // Borde interior — (155,175,490,250)px → x=-0.6125, y=-0.417, w=1.225, h=0.833
        renderer.dibujarRect(-0.6125f, -0.417f, 1.225f, 0.833f, 0.10f, 0.10f, 0.35f);

        // Título "FLAPPY BIRD" — y=370px → y_ndc=0.233, centrado en x=0
        float anchoTitulo = TextoPixel.anchoTexto("FLAPPY BIRD", 2);
        TextoPixel.dibujar(renderer, "FLAPPY BIRD",
            -anchoTitulo / 2, 0.233f, 2,
            1.0f, 1.0f, 0.2f);

        // "3 JUGADORES" — y=335px → y_ndc=0.117
        float anchoSub = TextoPixel.anchoTexto("3 JUGADORES", 1);
        TextoPixel.dibujar(renderer, "3 JUGADORES",
            -anchoSub / 2, 0.117f, 1,
            0.9f, 0.9f, 0.9f);

        // "J1: ESPACIO" — (175,305)px → x=-0.5625, y=0.017
        TextoPixel.dibujar(renderer, "J1: ESPACIO",
            -0.5625f, 0.017f, 1,
            1.0f, 0.8f, 0.0f);

        // "J2: W O ARRIBA" — (175,280)px → x=-0.5625, y=-0.067
        TextoPixel.dibujar(renderer, "J2: W O ARRIBA",
            -0.5625f, -0.067f, 1,
            0.3f, 0.7f, 1.0f);
        
        TextoPixel.dibujar(renderer, "J3: Y",
            -0.5625f, -0.150f, 1,
            1.0f, 1.0f, 1.0f);

        // "ENTER: INICIAR" — y=210px → y_ndc=-0.300, centrado
        float anchoEnter = TextoPixel.anchoTexto("ENTER: INICIAR", 1);
        TextoPixel.dibujar(renderer, "ENTER: INICIAR",
            -anchoEnter / 2, -0.300f, 1,
            0.3f, 1.0f, 0.3f);

        jugador1.dibujar(renderer);
        jugador2.dibujar(renderer);
        jugador3.dibujar(renderer);
    }

    /**
     * Dibuja el HUD (Heads-Up Display) con puntajes de ambos jugadores y el nivel.
     *
     * @param renderer Renderer para dibujar primitivas y texto
     */
    private void dibujarHUD(Renderer renderer) {
        // Barra HUD superior — (0,565,800,35)px → NDC: x=-1, y=0.883, w=2, h=0.117
        renderer.dibujarRect(-1f, 0.883f, 2f, 0.117f, 0.0f, 0.0f, 0.0f);

        // Texto y_ndc = 572px → 572/300-1 = 0.907
        // J1 — x=10px → -0.975
        TextoPixel.dibujar(renderer, "J1:" + puntajeJ1,
            -0.975f, 0.907f, 1,
            1.0f, 0.8f, 0.0f);

        // J2 — x=120px → -0.700
        TextoPixel.dibujar(renderer, "J2:" + puntajeJ2,
            -0.700f, 0.907f, 1,
            0.3f, 0.7f, 1.0f);

            // J3 — x=230px → -0.425
        TextoPixel.dibujar(renderer, "J3:" + puntajeJ3,
            -0.425f, 0.907f, 1,
            1.0f, 1.0f, 1.0f);

        // Nivel — centrado en x=0
        String textoNivel = "NIVEL:" + nivel;
        float anchoNivel = TextoPixel.anchoTexto(textoNivel, 1);
        TextoPixel.dibujar(renderer, textoNivel,
            -anchoNivel / 2, 0.907f, 1,
            0.2f, 1.0f, 0.2f);

        // ESC — x=590px → 0.475
        TextoPixel.dibujar(renderer, "ESC:SALIR",
            0.475f, 0.907f, 1,
            0.7f, 0.7f, 0.7f);
    }

    /**
     * Dibuja la pantalla de Game Over con puntajes finales y el ganador.
     *
     * @param renderer Renderer para dibujar primitivas y texto
     */
    private void dibujarGameOver(Renderer renderer) {
        // Panel fondo — (150,160,500,280)px → x=-0.625, y=-0.467, w=1.25, h=0.933
        renderer.dibujarRect(-0.625f, -0.467f, 1.25f, 0.933f, 0.05f, 0.05f, 0.15f);
        // Borde — (155,165,490,270)px → x=-0.6125, y=-0.450, w=1.225, h=0.900
        renderer.dibujarRect(-0.6125f, -0.450f, 1.225f, 0.900f, 0.08f, 0.08f, 0.25f);

        // "GAME OVER" — y=385px → 0.283, centrado
        float anchoGO = TextoPixel.anchoTexto("GAME OVER", 2);
        TextoPixel.dibujar(renderer, "GAME OVER",
            -anchoGO / 2, 0.283f, 2,
            1.0f, 0.15f, 0.15f);

        // "J1: X PUNTOS" — (175,335)px → x=-0.5625, y=0.117
        TextoPixel.dibujar(renderer, "J1: " + puntajeJ1 + " PUNTOS",
            -0.5625f, 0.117f, 1,
            1.0f, 0.8f, 0.0f);

        // "J2: X PUNTOS" — (175,310)px → x=-0.5625, y=0.033
        TextoPixel.dibujar(renderer, "J2: " + puntajeJ2 + " PUNTOS",
            -0.5625f, 0.033f, 1,
            0.3f, 0.7f, 1.0f);
        // "J3: X PUNTOS" — (175,285)px → x=-0.5625, y=-0.050
        TextoPixel.dibujar(renderer, "J3: " + puntajeJ3 + " PUNTOS",
            -0.5625f, -0.050f, 1,
            1.0f, 1.0f, 1.0f);

        String ganador;
        float gr, gg, gb;
        if (puntajeJ1 > puntajeJ2 && puntajeJ1 > puntajeJ3) {
            ganador = "GANA J1!";
            gr = 1.0f; gg = 0.8f; gb = 0.0f;
        } else if (puntajeJ2 > puntajeJ1 && puntajeJ2 > puntajeJ3) {
            ganador = "GANA J2!";
            gr = 0.3f; gg = 0.7f; gb = 1.0f;
        } else if (puntajeJ3 > puntajeJ1 && puntajeJ3 > puntajeJ2) {
            ganador = "GANA J3!";
            gr = 0.2f; gg = 1.0f; gb = 0.2f;
        } else {
            ganador = "EMPATE!";
            gr = 1.0f; gg = 1.0f; gb = 1.0f;
        }

        // Ganador — y=262px → -0.127, centrado
        float anchoGanador = TextoPixel.anchoTexto(ganador, 2);
        TextoPixel.dibujar(renderer, ganador,
            -anchoGanador / 2, -0.127f, 2,
            gr, gg, gb);

        // "ENTER: MENU" — y=197px → -0.343, centrado
        float anchoVolver = TextoPixel.anchoTexto("ENTER: MENU", 1);
        TextoPixel.dibujar(renderer, "ENTER: MENU",
            -anchoVolver / 2, -0.343f, 1,
            0.3f, 1.0f, 0.3f);
    }

    /**
     * Genera una nueva tubería con posición aleatoria del hueco y la agrega a la lista.
     */
    private void generarTuberia() {
        // Elegir aleatoriamente la posición Y del centro del hueco dentro del rango válido
        float centroHueco = HUECO_MIN_Y + random.nextFloat() * (HUECO_MAX_Y - HUECO_MIN_Y);

        // Crear la tubería fuera de pantalla con la velocidad de dificultad actual
        tuberias.add(new Tuberia(X_SPAWN_TUBERIA, centroHueco, velocidadTuberias));
    }

    /**
     * Verifica que los pájaros no salgan por los límites inferior y superior.
     * Si un pájaro toca el suelo o el techo, muere.
     */
    private void verificarLimitesPantalla() {
        // Límite inferior en NDC: -0.90 = tope de la franja verde del suelo
        // Equivale a birdBottom  (ajustado por nuestro suelo visual)
        if (jugador1.estaVivo() && jugador1.getY() < -0.90f) {
            jugador1.morir();
        }
        if (jugador2.estaVivo() && jugador2.getY() < -0.90f) {
            jugador2.morir();
        }
        if (jugador3.estaVivo() && jugador3.getY() < -0.90f) {
            jugador3.morir();
        }

        // Límite superior en NDC: 0.883 = borde inferior de la barra HUD
        // birdTop >= 1.0f (adaptado al HUD)
        if (jugador1.estaVivo() && jugador1.getY() + jugador1.getAlto() > 0.883f) {
            jugador1.morir();
        }
        if (jugador2.estaVivo() && jugador2.getY() + jugador2.getAlto() > 0.883f) {
            jugador2.morir();
        }
        if (jugador3.estaVivo() && jugador3.getY() + jugador3.getAlto() > 0.883f) {
            jugador3.morir();
        }
    }

    /**
     * Recalcula el nivel y la velocidad de tuberías según el puntaje máximo.
     * Cada 5 puntos se sube de nivel y la velocidad aumenta 50 px/s hasta el máximo.
     */
    private void actualizarDificultad() {
        // Usar el puntaje más alto entre ambos jugadores para definir el nivel
        int puntajeMaximo = Math.max(puntajeJ1, Math.max(puntajeJ2, puntajeJ3));

        // Calcular el nuevo nivel: 1 nivel base + 1 nivel por cada 5 puntos
        int nuevoNivel = 1 + puntajeMaximo / 5;

        // Solo actualizar si el nivel realmente cambió
        if (nuevoNivel != nivel) {
            nivel = nuevoNivel;   // Actualizar el nivel mostrado en el HUD

            // Nueva velocidad = base + incremento por nivel, sin exceder el máximo
            // Incremento de 0.125 NDC/s por nivel (≈50px/s * 2/800)
        velocidadTuberias = Math.min(VELOCIDAD_BASE + (nivel - 1) * 0.125f, VELOCIDAD_MAXIMA);

            // Actualizar la velocidad de todas las tuberías ya en pantalla
            for (Tuberia t : tuberias) {
                t.setVelocidad(velocidadTuberias);
            }
        }
    }

    /**
     * Reinicia el juego al estado inicial para comenzar una nueva partida.
     */
    private void reiniciar() {
        // Devolver cada pájaro a su posición y estado inicial
        jugador1.reiniciar(J1_X_INICIO, J1_Y_INICIO);
        jugador2.reiniciar(J2_X_INICIO, J2_Y_INICIO);
        jugador3.reiniciar(J3_X_INICIO, J3_Y_INICIO);

        // Eliminar todas las tuberías de la partida anterior
        tuberias.clear();

        // Resetear todos los contadores de la partida
        puntajeJ1 = 0;
        puntajeJ2 = 0;
        puntajeJ3 = 0;
        nivel = 1;
        velocidadTuberias = VELOCIDAD_BASE;
        timerTuberia = 0.0f;

        // Permitir que el sonido de game over suene en la próxima partida
        sonidoGameOverReproducido = false;
    }

    // --- Métodos de audio: solo reproducen si el gestor de sonido está disponible ---

    /** Reproduce el efecto de salto si el gestor de sonido está asignado */
    private void reproducirSonidoSalto() {
        if (gestorSonido != null) gestorSonido.reproducirSalto();
    }

    /** Reproduce el efecto de punto si el gestor de sonido está asignado */
    private void reproducirSonidoPunto() {
        if (gestorSonido != null) gestorSonido.reproducirPunto();
    }

    /** Reproduce el efecto de game over si el gestor de sonido está asignado */
    private void reproducirSonidoGameOver() {
        if (gestorSonido != null) gestorSonido.reproducirGameOver();
    }

    // --- Métodos públicos llamados por GestorEntrada ---

    /**
     * El jugador 1 intenta saltar (solo funciona mientras el juego está activo).
     */
    public void saltarJugador1() {
        if (estado == Estado.JUGANDO) {
            jugador1.saltar();            // Aplicar impulso hacia arriba
            reproducirSonidoSalto();      // Reproducir efecto de audio
        }
    }

    /**
     * El jugador 2 intenta saltar (solo funciona mientras el juego está activo).
     */
    public void saltarJugador2() {
        if (estado == Estado.JUGANDO) {
            jugador2.saltar();            // Aplicar impulso hacia arriba
            reproducirSonidoSalto();      // Reproducir efecto de audio
        }
    }

    public void saltarJugador3() {
        if (estado == Estado.JUGANDO) {
            jugador3.saltar();            // Aplicar impulso hacia arriba
            reproducirSonidoSalto();      // Reproducir efecto de audio
        }
    }

    /**
     * Reacciona a la tecla ENTER según el estado actual:
     * - MENU → inicia la partida
     * - GAME_OVER → vuelve al menú principal
     */
    public void presionarEnter() {
        switch (estado) {
            case MENU:
                // Inicializar el estado y pasar a modo de juego
                reiniciar();
                estado = Estado.JUGANDO;
                break;

            case GAME_OVER:
                // Volver al menú principal y limpiar el estado anterior
                estado = Estado.MENU;
                reiniciar();
                break;

            case JUGANDO:
                // No hacer nada mientras se está jugando
                break;
        }
    }

    // --- Getters para que Ventana pueda leer el estado y mostrarlo en el título ---

    /** @return Puntaje actual del jugador 1 */
    public int getPuntajeJ1() { return puntajeJ1; }

    /** @return Puntaje actual del jugador 2 */
    public int getPuntajeJ2() { return puntajeJ2; }

    public int getPuntajeJ3() { return puntajeJ3; }

    /** @return Nivel de dificultad actual */
    public int getNivel()     { return nivel; }

    /** @return Estado actual del juego (MENU, JUGANDO o GAME_OVER) */
    public Estado getEstado() { return estado; }
}
