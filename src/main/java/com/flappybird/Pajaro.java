package com.flappybird;

/**
 * Clase Pajaro - Representa un jugador en el juego Flappy Bird
 *
 * Contiene la física (gravedad y salto), la posición, velocidad vertical
 * y el método para dibujarse compuesto por múltiples figuras geométricas:
 * cuerpo, pico, ala, cola y ojo con pupila.
 *
 * El pájaro se inclina visualmente hacia arriba al subir y hacia abajo al caer,
 * simulando la rotación mediante el desplazamiento individual de sus partes.
 */
public class Pajaro {

    // Posición del pájaro en píxeles (origen abajo-izquierda de la ventana)
    private float x, y;

    // Velocidad vertical en píxeles por segundo (positiva = sube, negativa = baja)
    private float velocidadY;

    // Dimensiones del cuerpo principal del pájaro en NDC
    // BIRD_ANCHO = 0.10f, BIRD_ALTO = 0.10f
    private final float ancho = 0.10f;
    private final float alto  = 0.10f;

    // Indica si el pájaro está vivo (puede moverse) o muerto (se queda quieto)
    private boolean vivo;

    // Indica si el pájaro muerto está en animación de caída al suelo
    private boolean cayendo;

    // Color distintivo del pájaro (RGB) para diferenciarlo del otro jugador
    private final float colorR, colorG, colorB;

    // Gravedad en NDC/s²  .: -1.9f
    private static final float GRAVEDAD = -1.9f;

    // Velocidad de salto en NDC/s : 0.85f
    private static final float VELOCIDAD_SALTO = 0.85f;

    // Velocidad máxima de caída en NDC/s: -1.8f
    private static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    // Límite del suelo en NDC: tope superior de la franja verde (y = -0.90)
    // Equivale a 30px en una ventana de 600px: 30/300 - 1 = -0.90
    private static final float SUELO_NDC = -0.90f;

    // Temporizador para animar el ala (oscila entre arriba y abajo)
    private float tiempoAla = 0.0f;

    /**
     * Constructor del pájaro
     *
     * @param x      Posición inicial X
     * @param y      Posición inicial Y
     * @param colorR Componente roja del color del pájaro (0.0 - 1.0)
     * @param colorG Componente verde del color del pájaro (0.0 - 1.0)
     * @param colorB Componente azul del color del pájaro (0.0 - 1.0)
     */
    public Pajaro(float x, float y, float colorR, float colorG, float colorB) {
        this.x = x;              // Guardar posición inicial X
        this.y = y;              // Guardar posición inicial Y
        this.colorR = colorR;    // Guardar componente roja del color
        this.colorG = colorG;    // Guardar componente verde del color
        this.colorB = colorB;    // Guardar componente azul del color
        this.velocidadY = 0.0f;  // Empezar sin velocidad vertical
        this.vivo = true;        // El pájaro nace vivo
    }

    /**
     * Actualiza la física del pájaro: aplica gravedad y mueve según velocidad.
     *
     * @param deltaTime Tiempo transcurrido desde el último frame (en segundos)
     */
    public void actualizar(float deltaTime) {
        if (!vivo) {
            // Animación de caída al morir
            if (cayendo) {
                velocidadY += GRAVEDAD * deltaTime;
                if (velocidadY < VELOCIDAD_MAX_CAIDA) velocidadY = VELOCIDAD_MAX_CAIDA;
                y += velocidadY * deltaTime;
                // Detener en el suelo (NDC: -0.90 = tope de la franja verde)
                if (y <= SUELO_NDC) {
                    y = SUELO_NDC;
                    velocidadY = 0;
                    cayendo = false;
                }
            }
            return;
        }

        // Aplicar gravedad: la velocidad vertical disminuye con el tiempo
        velocidadY += GRAVEDAD * deltaTime;

        // Limitar la velocidad de caída máxima para evitar glitches visuales
        if (velocidadY < VELOCIDAD_MAX_CAIDA) {
            velocidadY = VELOCIDAD_MAX_CAIDA;
        }

        // Actualizar posición vertical según la velocidad actual
        y += velocidadY * deltaTime;

        // Avanzar el temporizador del ala para la animación de aleteo
        tiempoAla += deltaTime * 5.0f;
    }

    /**
     * Hace que el pájaro salte aplicando velocidad vertical positiva.
     */
    public void saltar() {
        // Solo puede saltar si está vivo
        if (!vivo) return;

        // Sobreescribir la velocidad vertical con la velocidad de salto
        velocidadY = VELOCIDAD_SALTO;
    }

    /**
     * Dibuja el pájaro completo compuesto por múltiples figuras geométricas.
     * Simula inclinación desplazando verticalmente el pico, la cola y el ojo
     * en función de la velocidad vertical actual.
     *
     * @param renderer Instancia del renderer para dibujar primitivas
     */
    public void dibujar(Renderer renderer) {
        // Determinar el color del pájaro: gris si está muerto, color normal si vivo
        float r = vivo ? colorR : 0.5f;
        float g = vivo ? colorG : 0.5f;
        float b = vivo ? colorB : 0.5f;

        // --- CÁLCULO DE INCLINACIÓN ---
        // Normalizar la velocidad vertical al rango -1..1 usando la velocidad de salto
        float factorTilt = velocidadY / VELOCIDAD_SALTO;

        // Limitar el factor a [-1, 1] para que la inclinación no sea excesiva
        factorTilt = Math.max(-1.0f, Math.min(1.0f, factorTilt));

        // Desplazamiento máximo de inclinación en NDC
        // 10px * (2/600) ≈ 0.033 NDC — escala con el tamaño del pájaro en NDC
        float inclinacion = factorTilt * 0.033f;

        // --- COLA (triángulo en el lado izquierdo del cuerpo) ---
        // La cola se desplaza en dirección opuesta al pico para simular rotación
        float offsetCola = -inclinacion * 0.5f;
        renderer.dibujarTriangulo(
            x,                    y + alto * 0.3f + offsetCola,  // Punta izquierda de la cola
            x + ancho * 0.2f,     y + alto * 0.1f + offsetCola,  // Unión inferior con cuerpo
            x + ancho * 0.2f,     y + alto * 0.7f + offsetCola,  // Unión superior con cuerpo
            r * 0.8f, g * 0.8f, b * 0.8f                          // Color ligeramente más oscuro
        );

        // --- CUERPO PRINCIPAL (rectángulo en el centro del pájaro) ---
        // El cuerpo no rota (es un rectángulo), pero las demás partes sí se desplazan
        renderer.dibujarRect(x, y, ancho, alto, r, g, b);

        // --- ALA (rectángulo pequeño que sube y baja para simular aleteo) ---
        // El offset vertical del ala oscila con función seno para el efecto de vuelo
        // 4px * (2/600) ≈ 0.013f NDC — era 4.0f en píxeles, causaba el cuadrado errante
        float offsetAla = (float) Math.sin(tiempoAla) * 0.013f;
        renderer.dibujarRect(
            x + ancho * 0.2f,           // Posición X: un poco hacia dentro del cuerpo
            y + alto * 0.4f + offsetAla, // Posición Y: centro del cuerpo + animación
            ancho * 0.5f,               // Ancho del ala: mitad del ancho del cuerpo
            alto * 0.25f,               // Alto del ala: un cuarto del alto del cuerpo
            r * 0.85f, g * 0.85f, b * 0.85f  // Color un poco más oscuro que el cuerpo
        );

        // --- PICO (triángulo apuntando hacia la derecha, inclinado según velocidad) ---
        // El pico se desplaza verticalmente según la inclinación calculada
        float offsetPico = inclinacion;
        renderer.dibujarTriangulo(
            x + ancho,          y + alto * 0.55f + offsetPico,  // Punta del pico (derecha)
            x + ancho * 0.75f,  y + alto * 0.7f  + offsetPico,  // Unión superior con cuerpo
            x + ancho * 0.75f,  y + alto * 0.4f  + offsetPico,  // Unión inferior con cuerpo
            1.0f, 0.6f, 0.0f                                      // Naranja para el pico
        );

        // --- OJO BLANCO (círculo blanco en la parte delantera superior) ---
        // El ojo sigue al pico, desplazándose 80% de la inclinación del pico
        float ojoCX = x + ancho * 0.7f;          // Centro X del ojo
        float ojoCY = y + alto * 0.7f + inclinacion * 0.8f;  // Centro Y con inclinación
        // Radio del ojo en NDC: 6px * (2/600) ≈ 0.020f
        renderer.dibujarCirculo(ojoCX, ojoCY, 0.020f, 16, 1.0f, 1.0f, 1.0f);

        // --- PUPILA (círculo negro pequeño dentro del ojo blanco) ---
        // Radio en NDC: 3px * (2/600) ≈ 0.010f
        // Offset X: 1.5px * (2/800) ≈ 0.004f  |  Offset Y: 0.5px * (2/600) ≈ 0.002f
        renderer.dibujarCirculo(ojoCX + 0.004f, ojoCY - 0.002f, 0.010f, 12, 0.0f, 0.0f, 0.0f);
    }

    /**
     * Mata al pájaro: detiene su movimiento y se dibujará en gris.
     */
    public void morir() {
        vivo = false;
        cayendo = true;
        // Impulso inicial hacia abajo para iniciar la animación de caída
        // Impulso inicial en NDC/s 
        if (velocidadY > -0.3f) velocidadY = -0.3f;
    }

    /**
     * Reinicia el pájaro a su estado inicial para una nueva partida.
     *
     * @param x Nueva posición X inicial
     * @param y Nueva posición Y inicial
     */
    public void reiniciar(float x, float y) {
        this.x = x;            // Restaurar posición X
        this.y = y;            // Restaurar posición Y
        this.velocidadY = 0;   // Sin velocidad inicial
        this.vivo = true;      // Volver a vivir
        this.cayendo = false;  // No está en caída de muerte
        this.tiempoAla = 0;    // Reiniciar animación del ala
    }

    // --- Getters para leer el estado del pájaro desde otras clases ---

    /** @return Posición X actual del pájaro en píxeles */
    public float getX() { return x; }

    /** @return Posición Y actual del pájaro en píxeles */
    public float getY() { return y; }

    /** @return Ancho del pájaro en píxeles */
    public float getAncho() { return ancho; }

    /** @return Alto del pájaro en píxeles */
    public float getAlto() { return alto; }

    /** @return true si el pájaro está vivo y puede moverse */
    public boolean estaVivo() { return vivo; }

    /** @return Velocidad vertical actual en píxeles por segundo */
    public float getVelocidadY() { return velocidadY; }
}
