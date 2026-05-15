package com.flappybird;

/**
 * Clase Pajaro - Representa un jugador en el juego Flappy Bird
 *
 * Contiene la física (gravedad y salto), la posición, velocidad vertical
 * y el método para dibujarse compuesto por múltiples figuras geométricas:
 * cuerpo, pico, ala, cola y ojo.
 */
public class Pajaro {

    // Posición del pájaro en píxeles (origen abajo-izquierda de la ventana)
    private float x, y;

    // Velocidad vertical en píxeles por segundo (positiva = sube, negativa = baja)
    private float velocidadY;

    // Dimensiones del cuerpo principal del pájaro
    private final float ancho = 40.0f;
    private final float alto  = 30.0f;

    // Indica si el pájaro está vivo (puede moverse) o muerto (se queda quieto)
    private boolean vivo;

    // Color distintivo del pájaro (RGB) para diferenciarlo del otro jugador
    private final float colorR, colorG, colorB;

    // Constante de gravedad: cuántos píxeles por segundo cuadrado cae el pájaro
    private static final float GRAVEDAD = -800.0f;

    // Velocidad vertical al saltar (positiva porque sube)
    private static final float VELOCIDAD_SALTO = 350.0f;

    // Velocidad máxima de caída para evitar que atraviese el suelo
    private static final float VELOCIDAD_MAX_CAIDA = -600.0f;

    // Temporizador para animar el ala (oscila entre arriba y abajo)
    private float tiempoAla = 0.0f;

    /**
     * Constructor del pájaro
     *
     * @param x      Posición inicial X
     * @param y      Posición inicial Y
     * @param colorR Color rojo
     * @param colorG Color verde
     * @param colorB Color azul
     */
    public Pajaro(float x, float y, float colorR, float colorG, float colorB) {
        this.x = x;
        this.y = y;
        this.colorR = colorR;
        this.colorG = colorG;
        this.colorB = colorB;
        this.velocidadY = 0.0f;
        this.vivo = true;
    }

    /**
     * Actualiza la física del pájaro: aplica gravedad y mueve según velocidad
     *
     * @param deltaTime Tiempo transcurrido desde el último frame (en segundos)
     */
    public void actualizar(float deltaTime) {
        // Si el pájaro está muerto, no actualizar física
        if (!vivo) return;

        // Aplicar gravedad: la velocidad vertical disminuye con el tiempo
        velocidadY += GRAVEDAD * deltaTime;

        // Limitar la velocidad de caída máxima para evitar glitches
        if (velocidadY < VELOCIDAD_MAX_CAIDA) {
            velocidadY = VELOCIDAD_MAX_CAIDA;
        }

        // Actualizar posición vertical según la velocidad actual
        y += velocidadY * deltaTime;

        // Avanzar el temporizador del ala para la animación
        tiempoAla += deltaTime * 5.0f;  // Frecuencia de aleteo
    }

    /**
     * Hace que el pájaro salte aplicando velocidad vertical positiva
     */
    public void saltar() {
        // Solo puede saltar si está vivo
        if (!vivo) return;

        // Sobreescribir la velocidad vertical con la velocidad de salto
        velocidadY = VELOCIDAD_SALTO;
    }

    /**
     * Dibuja el pájaro completo compuesto por múltiples figuras geométricas
     *
     * @param renderer Instancia del renderer para dibujar primitivas
     */
    public void dibujar(Renderer renderer) {
        // Si está muerto, dibujar en gris para indicar el estado
        float r = vivo ? colorR : 0.5f;
        float g = vivo ? colorG : 0.5f;
        float b = vivo ? colorB : 0.5f;

        // --- COLA (triángulo en el lado izquierdo del cuerpo) ---
        // La cola apunta hacia atrás (izquierda), formando una figura puntiaguda
        renderer.dibujarTriangulo(
            x,           y + alto * 0.3f,  // Punta izquierda de la cola
            x + ancho * 0.2f, y + alto * 0.1f,  // Unión inferior con cuerpo
            x + ancho * 0.2f, y + alto * 0.7f,  // Unión superior con cuerpo
            r * 0.8f, g * 0.8f, b * 0.8f         // Color ligeramente más oscuro
        );

        // --- CUERPO PRINCIPAL (rectángulo redondeado simulado con un rect grande) ---
        renderer.dibujarRect(x, y, ancho, alto, r, g, b);

        // --- ALA (rectángulo pequeño que sube y baja para simular aleteo) ---
        // El offset vertical del ala oscila usando la función seno
        float offsetAla = (float) Math.sin(tiempoAla) * 4.0f;
        renderer.dibujarRect(
            x + ancho * 0.2f,          // Un poco hacia la derecha del borde izquierdo
            y + alto * 0.4f + offsetAla,// Centro vertical del cuerpo + animación
            ancho * 0.5f,              // Mitad del ancho del cuerpo
            alto * 0.25f,              // Un cuarto del alto del cuerpo
            r * 0.85f, g * 0.85f, b * 0.85f  // Color un poco más oscuro que el cuerpo
        );

        // --- PICO (triángulo apuntando hacia la derecha) ---
        renderer.dibujarTriangulo(
            x + ancho,          y + alto * 0.55f,  // Punta del pico (derecha)
            x + ancho * 0.75f,  y + alto * 0.7f,   // Unión superior con cuerpo
            x + ancho * 0.75f,  y + alto * 0.4f,   // Unión inferior con cuerpo
            1.0f, 0.6f, 0.0f                         // Naranja para el pico
        );

        // --- OJO BLANCO (círculo blanco en la parte delantera superior) ---
        float ojoCX = x + ancho * 0.7f;  // Posición X del centro del ojo
        float ojoCY = y + alto * 0.7f;   // Posición Y del centro del ojo
        renderer.dibujarCirculo(ojoCX, ojoCY, 6.0f, 16, 1.0f, 1.0f, 1.0f);

        // --- PUPILA (círculo negro pequeño dentro del ojo blanco) ---
        renderer.dibujarCirculo(ojoCX + 1.5f, ojoCY - 0.5f, 3.0f, 12, 0.0f, 0.0f, 0.0f);
    }

    /**
     * Mata al pájaro: deja de moverse y se dibuja en gris
     */
    public void morir() {
        vivo = false;
        velocidadY = 0;
    }

    /**
     * Reinicia el pájaro a su estado inicial
     *
     * @param x Nueva posición X inicial
     * @param y Nueva posición Y inicial
     */
    public void reiniciar(float x, float y) {
        this.x = x;
        this.y = y;
        this.velocidadY = 0;
        this.vivo = true;
        this.tiempoAla = 0;
    }

    // --- Getters ---

    public float getX() { return x; }
    public float getY() { return y; }
    public float getAncho() { return ancho; }
    public float getAlto() { return alto; }
    public boolean estaVivo() { return vivo; }
}
