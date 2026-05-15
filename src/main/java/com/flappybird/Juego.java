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

    // Lista de tuberías actualmente visibles en pantalla
    private final List<Tuberia> tuberias;

    // Generador de números aleatorios para la posición del hueco de las tuberías
    private final Random random;

    // Gestor de sonido para reproducir efectos de audio
    private GestorSonido gestorSonido;

    // Puntajes individuales de cada jugador (se incrementan al pasar tuberías)
    private int puntajeJ1 = 0;
    private int puntajeJ2 = 0;

    // Velocidad actual de las tuberías (aumenta con la dificultad)
    private float velocidadTuberias = 200.0f;

    // Velocidad base al inicio de cada partida
    private static final float VELOCIDAD_BASE = 200.0f;

    // Velocidad máxima para que el juego siga siendo jugable
    private static final float VELOCIDAD_MAXIMA = 500.0f;

    // Nivel actual de dificultad (visible en el HUD y el título de la ventana)
    private int nivel = 1;

    // Temporizador para controlar cuándo aparece la siguiente tubería
    private float timerTuberia = 0.0f;

    // Segundos entre la aparición de cada nueva tubería
    private static final float INTERVALO_TUBERIAS = 2.5f;

    // Posición X inicial de las tuberías (fuera de pantalla, a la derecha)
    private static final float X_SPAWN_TUBERIA = 820.0f;

    // Rango vertical permitido para el centro del hueco (para que sea jugable)
    private static final float HUECO_MIN_Y = 150.0f;
    private static final float HUECO_MAX_Y = 450.0f;

    // Posiciones iniciales de los dos pájaros
    private static final float J1_X_INICIO = 150.0f;
    private static final float J1_Y_INICIO = 300.0f;
    private static final float J2_X_INICIO = 100.0f;
    private static final float J2_Y_INICIO = 300.0f;

    // Bandera para reproducir el sonido de game over una sola vez
    private boolean sonidoGameOverReproducido = false;

    // --- Nube parallax: cada nube tiene su propio X, Y, ancho y velocidad ---

    // Posiciones X de las nubes (se actualizan en cada frame para efecto parallax)
    private final float[] nubesPosX = {100f, 300f, 600f, 50f, 450f};

    // Posiciones Y fijas de cada nube (no cambian)
    private final float[] nubesPosY = {490f, 525f, 500f, 510f, 515f};

    // Anchos de cada nube para variedad visual
    private final float[] nubesAncho = {80f, 105f, 90f, 70f, 115f};

    // Alturas de cada nube para variedad visual
    private final float[] nubesAlto  = {22f, 18f, 25f, 20f, 16f};

    // Velocidades individuales de cada nube (efecto parallax)
    private final float[] nubesVel   = {20f, 25f, 18f, 22f, 28f};

    /**
     * Constructor: inicializa los jugadores, la lista de tuberías y el estado inicial.
     */
    public Juego() {
        // Crear jugador 1 con color amarillo/naranja
        jugador1 = new Pajaro(J1_X_INICIO, J1_Y_INICIO, 1.0f, 0.8f, 0.0f);

        // Crear jugador 2 con color azul claro
        jugador2 = new Pajaro(J2_X_INICIO, J2_Y_INICIO, 0.3f, 0.7f, 1.0f);

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
                reproducirSonidoPunto();              // Reproducir efecto de audio
            }

            // Verificar si el jugador 2 pasó esta tubería
            if (jugador2.estaVivo() && t.verificarPasoJugador2(jugador2.getX())) {
                puntajeJ2++;                          // Incrementar puntaje de J2
                actualizarDificultad();               // Recalcular nivel y velocidad
                reproducirSonidoPunto();              // Reproducir efecto de audio
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
        }

        // Verificar si ambos jugadores están muertos para terminar la partida
        if (!jugador1.estaVivo() && !jugador2.estaVivo()) {
            // Solo reproducir el sonido de game over la primera vez
            if (!sonidoGameOverReproducido) {
                reproducirSonidoGameOver();           // Reproducir efecto de audio
                sonidoGameOverReproducido = true;     // Marcar como reproducido
            }
            estado = Estado.GAME_OVER;                // Cambiar al estado de game over
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
            if (nubesPosX[i] + nubesAncho[i] < 0) {
                nubesPosX[i] = 820f;   // Reaparece fuera del borde derecho
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
        // === 1. CIELO (fondo degradado simulado con dos rectángulos) ===

        // Mitad superior del cielo: azul más claro (luz del sol)
        renderer.dibujarRect(0, 300, 800, 300, 0.40f, 0.70f, 1.00f);

        // Mitad inferior del cielo: azul más oscuro (horizonte)
        renderer.dibujarRect(0, 0,   800, 300, 0.30f, 0.55f, 0.90f);

        // === 2. MONTAÑAS (triángulos marrones/grises en el horizonte) ===
        // Las montañas quedan detrás de las nubes y las tuberías

        // Montaña 1: marrón oscuro, extremo izquierdo
        renderer.dibujarTriangulo(
            -20f,  30f,    // Vértice inferior-izquierdo (fuera de pantalla)
            130f,  165f,   // Pico de la montaña
            280f,  30f,    // Vértice inferior-derecho
            0.38f, 0.32f, 0.28f  // Color marrón oscuro
        );

        // Montaña 2: gris azulado, segunda desde la izquierda (más alta)
        renderer.dibujarTriangulo(
            220f,  30f,    // Vértice inferior-izquierdo
            380f,  190f,   // Pico de la montaña
            540f,  30f,    // Vértice inferior-derecho
            0.32f, 0.28f, 0.26f  // Color gris-marrón
        );

        // Montaña 3: marrón claro, centro
        renderer.dibujarTriangulo(
            460f,  30f,    // Vértice inferior-izquierdo
            600f,  140f,   // Pico de la montaña (más baja)
            740f,  30f,    // Vértice inferior-derecho
            0.42f, 0.36f, 0.30f  // Color marrón claro
        );

        // Montaña 4: gris, extremo derecho (parcialmente fuera de pantalla)
        renderer.dibujarTriangulo(
            680f,  30f,    // Vértice inferior-izquierdo
            820f,  175f,   // Pico de la montaña
            960f,  30f,    // Vértice inferior-derecho (fuera de pantalla)
            0.35f, 0.30f, 0.27f  // Color gris oscuro
        );

        // === 3. NUBES PARALLAX (rectángulos blancos que se mueven lentamente) ===
        for (int i = 0; i < nubesPosX.length; i++) {
            // Dibujar cada nube con su posición y tamaño propios
            renderer.dibujarRect(
                nubesPosX[i],           // Posición X actual (se actualiza en actualizar)
                nubesPosY[i],           // Posición Y fija
                nubesAncho[i],          // Ancho individual de la nube
                nubesAlto[i],           // Alto individual de la nube
                0.95f, 0.95f, 0.98f     // Blanco ligeramente azulado
            );

            // Dibujar un segundo rectángulo más pequeño encima para dar volumen
            renderer.dibujarRect(
                nubesPosX[i] + nubesAncho[i] * 0.15f,  // Un poco hacia adentro en X
                nubesPosY[i] + nubesAlto[i] * 0.5f,    // Sobre la nube base
                nubesAncho[i] * 0.60f,                  // Más estrecho que la base
                nubesAlto[i] * 0.70f,                   // Un poco menos alto
                0.98f, 0.98f, 1.00f                     // Blanco puro arriba
            );
        }

        // === 4. SUELO (banda verde/marrón en la base de la pantalla) ===

        // Franja verde que simula el suelo
        renderer.dibujarRect(0, 0, 800, 30, 0.35f, 0.58f, 0.18f);

        // Línea oscura en el borde superior del suelo para dar contraste
        renderer.dibujarRect(0, 28, 800, 4, 0.25f, 0.42f, 0.12f);

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
        // Panel de fondo del menú: rectángulo oscuro semitransparente simulado
        renderer.dibujarRect(150, 170, 500, 260, 0.05f, 0.05f, 0.20f);

        // Borde interior del panel (ligeramente más claro para efecto de borde)
        renderer.dibujarRect(155, 175, 490, 250, 0.10f, 0.10f, 0.35f);

        // Título "FLAPPY BIRD" en amarillo, centrado horizontalmente
        float anchoTitulo = TextoPixel.anchoTexto("FLAPPY BIRD", 2);
        TextoPixel.dibujar(renderer, "FLAPPY BIRD",
            400 - anchoTitulo / 2, 370, 2,        // Centrado en X, alto en Y
            1.0f, 1.0f, 0.2f);                     // Color amarillo brillante

        // Subtítulo "2 JUGADORES" en blanco, tamaño normal
        float anchoSub = TextoPixel.anchoTexto("2 JUGADORES", 1);
        TextoPixel.dibujar(renderer, "2 JUGADORES",
            400 - anchoSub / 2, 335, 1,
            0.9f, 0.9f, 0.9f);                     // Blanco suave

        // Instrucción del jugador 1 en color amarillo (mismo que su pájaro)
        TextoPixel.dibujar(renderer, "J1: ESPACIO",
            175, 305, 1,
            1.0f, 0.8f, 0.0f);                     // Amarillo = color de J1

        // Instrucción del jugador 2 en color azul (mismo que su pájaro)
        TextoPixel.dibujar(renderer, "J2: W O ARRIBA",
            175, 280, 1,
            0.3f, 0.7f, 1.0f);                     // Azul = color de J2

        // Instrucción para iniciar la partida en verde
        float anchoEnter = TextoPixel.anchoTexto("ENTER: INICIAR", 1);
        TextoPixel.dibujar(renderer, "ENTER: INICIAR",
            400 - anchoEnter / 2, 210, 1,
            0.3f, 1.0f, 0.3f);                     // Verde para llamar la atención

        // Mostrar los pájaros de muestra en sus posiciones iniciales
        jugador1.dibujar(renderer);
        jugador2.dibujar(renderer);
    }

    /**
     * Dibuja el HUD (Heads-Up Display) con puntajes de ambos jugadores y el nivel.
     *
     * @param renderer Renderer para dibujar primitivas y texto
     */
    private void dibujarHUD(Renderer renderer) {
        // Barra superior negra como fondo del HUD (evita que el texto se confunda)
        renderer.dibujarRect(0, 565, 800, 35, 0.0f, 0.0f, 0.0f);

        // Puntaje del jugador 1 en amarillo (igual a su color de pájaro)
        TextoPixel.dibujar(renderer,
            "J1:" + puntajeJ1,
            10, 572, 1,
            1.0f, 0.8f, 0.0f);             // Amarillo

        // Puntaje del jugador 2 en azul (igual a su color de pájaro)
        TextoPixel.dibujar(renderer,
            "J2:" + puntajeJ2,
            120, 572, 1,
            0.3f, 0.7f, 1.0f);             // Azul

        // Nivel actual en verde, centrado en la barra HUD
        String textoNivel = "NIVEL:" + nivel;
        float anchoNivel = TextoPixel.anchoTexto(textoNivel, 1);
        TextoPixel.dibujar(renderer,
            textoNivel,
            400 - anchoNivel / 2, 572, 1,
            0.2f, 1.0f, 0.2f);             // Verde brillante

        // Atajo ESC a la derecha de la barra HUD
        TextoPixel.dibujar(renderer, "ESC:SALIR",
            590, 572, 1,
            0.7f, 0.7f, 0.7f);             // Gris
    }

    /**
     * Dibuja la pantalla de Game Over con puntajes finales y el ganador.
     *
     * @param renderer Renderer para dibujar primitivas y texto
     */
    private void dibujarGameOver(Renderer renderer) {
        // Panel oscuro de fondo para la pantalla de game over
        renderer.dibujarRect(150, 160, 500, 280, 0.05f, 0.05f, 0.15f);

        // Borde interior del panel
        renderer.dibujarRect(155, 165, 490, 270, 0.08f, 0.08f, 0.25f);

        // Título "GAME OVER" en rojo, grande y centrado
        float anchoGO = TextoPixel.anchoTexto("GAME OVER", 2);
        TextoPixel.dibujar(renderer, "GAME OVER",
            400 - anchoGO / 2, 385, 2,
            1.0f, 0.15f, 0.15f);           // Rojo intenso

        // Puntaje final del jugador 1 en su color amarillo
        TextoPixel.dibujar(renderer,
            "J1: " + puntajeJ1 + " PUNTOS",
            175, 335, 1,
            1.0f, 0.8f, 0.0f);             // Amarillo

        // Puntaje final del jugador 2 en su color azul
        TextoPixel.dibujar(renderer,
            "J2: " + puntajeJ2 + " PUNTOS",
            175, 310, 1,
            0.3f, 0.7f, 1.0f);             // Azul

        // Determinar quién ganó y asignar el texto y color apropiados
        String ganador;
        float gr, gg, gb;
        if (puntajeJ1 > puntajeJ2) {
            ganador = "GANA J1!";
            gr = 1.0f; gg = 0.8f; gb = 0.0f;   // Color del J1 (amarillo)
        } else if (puntajeJ2 > puntajeJ1) {
            ganador = "GANA J2!";
            gr = 0.3f; gg = 0.7f; gb = 1.0f;   // Color del J2 (azul)
        } else {
            ganador = "EMPATE!";
            gr = 1.0f; gg = 1.0f; gb = 1.0f;   // Blanco para empate
        }

        // Mostrar al ganador centrado y grande
        float anchoGanador = TextoPixel.anchoTexto(ganador, 2);
        TextoPixel.dibujar(renderer, ganador,
            400 - anchoGanador / 2, 262, 2,
            gr, gg, gb);

        // Instrucción para volver al menú en verde
        float anchoVolver = TextoPixel.anchoTexto("ENTER: MENU", 1);
        TextoPixel.dibujar(renderer, "ENTER: MENU",
            400 - anchoVolver / 2, 197, 1,
            0.3f, 1.0f, 0.3f);             // Verde
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
        // Límite inferior: el suelo tiene 30px de alto (dibujado en dibujar())
        if (jugador1.estaVivo() && jugador1.getY() < 30) {
            jugador1.morir();   // J1 tocó el suelo
        }
        if (jugador2.estaVivo() && jugador2.getY() < 30) {
            jugador2.morir();   // J2 tocó el suelo
        }

        // Límite superior: la barra HUD empieza en y=565
        if (jugador1.estaVivo() && jugador1.getY() + jugador1.getAlto() > 565) {
            jugador1.morir();   // J1 tocó el techo
        }
        if (jugador2.estaVivo() && jugador2.getY() + jugador2.getAlto() > 565) {
            jugador2.morir();   // J2 tocó el techo
        }
    }

    /**
     * Recalcula el nivel y la velocidad de tuberías según el puntaje máximo.
     * Cada 5 puntos se sube de nivel y la velocidad aumenta 50 px/s hasta el máximo.
     */
    private void actualizarDificultad() {
        // Usar el puntaje más alto entre ambos jugadores para definir el nivel
        int puntajeMaximo = Math.max(puntajeJ1, puntajeJ2);

        // Calcular el nuevo nivel: 1 nivel base + 1 nivel por cada 5 puntos
        int nuevoNivel = 1 + puntajeMaximo / 5;

        // Solo actualizar si el nivel realmente cambió
        if (nuevoNivel != nivel) {
            nivel = nuevoNivel;   // Actualizar el nivel mostrado en el HUD

            // Nueva velocidad = base + incremento por nivel, sin exceder el máximo
            velocidadTuberias = Math.min(VELOCIDAD_BASE + (nivel - 1) * 50, VELOCIDAD_MAXIMA);

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

        // Eliminar todas las tuberías de la partida anterior
        tuberias.clear();

        // Resetear todos los contadores de la partida
        puntajeJ1 = 0;
        puntajeJ2 = 0;
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

    /** @return Nivel de dificultad actual */
    public int getNivel()     { return nivel; }

    /** @return Estado actual del juego (MENU, JUGANDO o GAME_OVER) */
    public Estado getEstado() { return estado; }
}
