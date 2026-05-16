package com.flappybird;

/**
 * Clase Tuberia - Representa un par de tuberías (superior e inferior)
 *
 * Cada instancia contiene una tubería de arriba y una de abajo con un hueco
 * en el medio por donde el pájaro debe pasar. Se mueve horizontalmente de
 * derecha a izquierda y desaparece al salir de pantalla.
 */
public class Tuberia {

    // Posición X del borde izquierdo de las tuberías en NDC
    private float x;

    // Centro Y del hueco entre las dos tuberías en NDC
    private final float centroHueco;

    // Altura del hueco en NDC : GAP_ALTO = 0.48f
    private static final float ALTO_HUECO = 0.48f;

    // Ancho de la tubería en NDC : TUBERIA_ANCHO = 0.18f
    private static final float ANCHO = 0.18f;

    // Velocidad de desplazamiento horizontal en NDC/s : 0.62f
    private float velocidad;

    // Indica si el pájaro 1 ya pasó esta tubería (para no contar el punto dos veces)
    private boolean pasadaJugador1 = false;

    // Indica si el pájaro 2 ya pasó esta tubería
    private boolean pasadaJugador2 = false;

    // Indica si el pájaro 3 ya pasó esta tubería
    private boolean pasadaJugador3 = false;

    /**
     * Constructor de la tubería
     *
     * @param x           Posición X inicial (normalmente fuera de pantalla a la derecha)
     * @param centroHueco Posición Y del centro del hueco (generada aleatoriamente)
     * @param velocidad   Velocidad de movimiento hacia la izquierda (px/s)
     */
    public Tuberia(float x, float centroHueco, float velocidad) {
        this.x = x;
        this.centroHueco = centroHueco;
        this.velocidad = velocidad;
    }

    /**
     * Actualiza la posición de la tubería moviéndola hacia la izquierda
     *
     * @param deltaTime Tiempo transcurrido desde el último frame (segundos)
     */
    public void actualizar(float deltaTime) {
        // Mover la tubería hacia la izquierda según la velocidad y el tiempo
        x -= velocidad * deltaTime;
    }

    /**
     * Dibuja ambas tuberías (la de abajo y la de arriba)
     *
     * @param renderer Renderer para dibujar los rectángulos
     */
    public void dibujar(Renderer renderer) {
        // Calcular los límites del hueco
        float huecoAbajo  = centroHueco - ALTO_HUECO / 2.0f;  // Borde inferior del hueco
        float huecoArriba = centroHueco + ALTO_HUECO / 2.0f;  // Borde superior del hueco

        // Color verde para las tuberías
        float r = 0.2f, g = 0.75f, b = 0.2f;

        // --- TUBERÍA INFERIOR ---
        // En NDC: va desde -1.0 (suelo) hasta el borde inferior del hueco.
        // Aquí: origen en esquina inferior-izquierda, alto = huecoAbajo - (-1.0)
        float altoInf = huecoAbajo + 1.0f;
        renderer.dibujarRect(x, -1.0f, ANCHO, altoInf, r, g, b);
        // Sombra: franja oscura en el borde derecho de la tubería inferior
        renderer.dibujarRect(x + ANCHO - 0.018f, -1.0f, 0.018f, altoInf, r * 0.55f, g * 0.55f, b * 0.55f);
        // Cap inferior
        renderer.dibujarRect(x - 0.015f, huecoAbajo - 0.012f, ANCHO + 0.030f, 0.012f,
                             r * 0.8f, g * 0.8f, b * 0.8f);
        // Sombra del cap inferior
        renderer.dibujarRect(x - 0.015f + (ANCHO + 0.030f) - 0.018f, huecoAbajo - 0.012f, 0.018f, 0.012f,
                             r * 0.45f, g * 0.45f, b * 0.45f);

        // --- TUBERÍA SUPERIOR ---
        float altoSup = 1.0f - huecoArriba;
        renderer.dibujarRect(x, huecoArriba, ANCHO, altoSup, r, g, b);
        // Sombra: franja oscura en el borde derecho de la tubería superior
        renderer.dibujarRect(x + ANCHO - 0.018f, huecoArriba, 0.018f, altoSup, r * 0.55f, g * 0.55f, b * 0.55f);
        // Cap superior
        renderer.dibujarRect(x - 0.015f, huecoArriba, ANCHO + 0.030f, 0.012f,
                             r * 0.8f, g * 0.8f, b * 0.8f);
        // Sombra del cap superior
        renderer.dibujarRect(x - 0.015f + (ANCHO + 0.030f) - 0.018f, huecoArriba, 0.018f, 0.012f,
                             r * 0.45f, g * 0.45f, b * 0.45f);
    }

    /**
     * Verifica si la tubería ya salió completamente de la pantalla por la izquierda
     *
     * @return true si la tubería ya no es visible
     */
    public boolean fueraDePantalla() {
        // En NDC: fuera de pantalla cuando el borde derecho pasa el límite izquierdo (-1.3)
        return x + ANCHO < -1.3f;
    }

    /**
     * Verifica si un pájaro colisiona con esta tubería usando AABB
     *
     * @param px Posición X del pájaro
     * @param py Posición Y del pájaro
     * @param pw Ancho del pájaro
     * @param ph Alto del pájaro
     * @return true si hay colisión
     */
    public boolean colisiona(float px, float py, float pw, float ph) {
        // Verificar si el pájaro está dentro del rango horizontal de la tubería
        boolean dentroX = px < x + ANCHO && px + pw > x;

        if (!dentroX) return false;  // Si no está en X, no puede colisionar

        // Calcular los bordes del hueco
        float huecoAbajo  = centroHueco - ALTO_HUECO / 2.0f;
        float huecoArriba = centroHueco + ALTO_HUECO / 2.0f;

        // El pájaro colisiona si NO está completamente dentro del hueco
        boolean enElHueco = py > huecoAbajo && py + ph < huecoArriba;

        return !enElHueco;  // Hay colisión si el pájaro NO está en el hueco
    }

    /**
     * Verifica si el jugador 1 acaba de pasar la tubería para sumar punto
     *
     * @param px Posición X del pájaro
     * @return true si acaba de pasar (solo la primera vez)
     */
    public boolean verificarPasoJugador1(float px) {
        // El pájaro pasa cuando su centro supera el centro de la tubería
        if (!pasadaJugador1 && px > x + ANCHO) {
            pasadaJugador1 = true;
            return true;
        }
        return false;
    }

    /**
     * Verifica si el jugador 2 acaba de pasar la tubería para sumar punto
     *
     * @param px Posición X del pájaro
     * @return true si acaba de pasar (solo la primera vez)
     */
    public boolean verificarPasoJugador2(float px) {
        if (!pasadaJugador2 && px > x + ANCHO) {
            pasadaJugador2 = true;
            return true;
        }
        return false;
    }

    /**
     * Verifica si el jugador 3 acaba de pasar la tubería para sumar punto
     *
     * @param px Posición X del pájaro
     * @return true si acaba de pasar (solo la primera vez)
     */
    public boolean verificarPasoJugador3(float px) {
        if (!pasadaJugador3 && px > x + ANCHO) {
            pasadaJugador3 = true;
            return true;
        }
        return false;
    }

    /**
     * Actualiza la velocidad de la tubería (para dificultad progresiva)
     *
     * @param nuevaVelocidad Nueva velocidad en píxeles por segundo
     */
    public void setVelocidad(float nuevaVelocidad) {
        this.velocidad = nuevaVelocidad;
    }

    public float getX() { return x; }
    public float getAncho() { return ANCHO; }
}
