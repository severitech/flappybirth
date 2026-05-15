package com.flappybird;

/**
 * Clase Tuberia - Representa un par de tuberías (superior e inferior)
 *
 * Cada instancia contiene una tubería de arriba y una de abajo con un hueco
 * en el medio por donde el pájaro debe pasar. Se mueve horizontalmente de
 * derecha a izquierda y desaparece al salir de pantalla.
 */
public class Tuberia {

    // Posición X del borde izquierdo de las tuberías (en píxeles)
    private float x;

    // Centro Y del hueco entre las dos tuberías (en píxeles)
    private final float centroHueco;

    // Altura del hueco libre entre tubería superior e inferior
    private static final float ALTO_HUECO = 160.0f;

    // Ancho fijo de ambas tuberías
    private static final float ANCHO = 60.0f;

    // Altura total de la ventana (para calcular las tuberías)
    private static final float ALTO_VENTANA = 600.0f;

    // Velocidad de desplazamiento horizontal (negativa = va a la izquierda)
    private float velocidad;

    // Indica si el pájaro 1 ya pasó esta tubería (para no contar el punto dos veces)
    private boolean pasadaJugador1 = false;

    // Indica si el pájaro 2 ya pasó esta tubería
    private boolean pasadaJugador2 = false;

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
        // Va desde el suelo (y=0) hasta el borde inferior del hueco
        renderer.dibujarRect(x, 0, ANCHO, huecoAbajo, r, g, b);

        // Borde superior de la tubería inferior (cap más ancho)
        renderer.dibujarRect(x - 5, huecoAbajo - 15, ANCHO + 10, 15, r * 0.8f, g * 0.8f, b * 0.8f);

        // --- TUBERÍA SUPERIOR ---
        // Va desde el borde superior del hueco hasta el techo de la ventana
        renderer.dibujarRect(x, huecoArriba, ANCHO, ALTO_VENTANA - huecoArriba, r, g, b);

        // Borde inferior de la tubería superior (cap más ancho)
        renderer.dibujarRect(x - 5, huecoArriba, ANCHO + 10, 15, r * 0.8f, g * 0.8f, b * 0.8f);
    }

    /**
     * Verifica si la tubería ya salió completamente de la pantalla por la izquierda
     *
     * @return true si la tubería ya no es visible
     */
    public boolean fueraDePantalla() {
        // La tubería desaparece cuando su borde derecho cruza el x=0
        return x + ANCHO < 0;
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
