package com.flappybird;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Clase Ventana - Gestiona el ciclo de vida de la ventana y el bucle principal
 *
 * Inicializa GLFW y OpenGL 3.3 core, crea la ventana del juego y ejecuta el
 * game loop que actualiza la lógica y renderiza cada frame usando deltaTime
 * para asegurar que el movimiento sea independiente de los FPS del hardware.
 *
 * También crea e integra el sistema de sonido con OpenAL.
 */
public class Ventana {

    // Handle de la ventana GLFW (identificador opaco de la ventana nativa del SO)
    private long handle;

    // Dimensiones fijas de la ventana en píxeles
    private static final int ANCHO = 800;
    private static final int ALTO  = 600;

    // Instancia del juego que contiene toda la lógica y el estado
    private Juego juego;

    // Renderer que maneja los shaders y el dibujado de primitivas OpenGL
    private Renderer renderer;

    // Gestor de entrada que escucha los eventos del teclado
    private GestorEntrada gestorEntrada;

    // Gestor de sonido que reproduce efectos de audio con OpenAL
    private GestorSonido gestorSonido;

    /**
     * Método principal del ciclo de vida:
     * inicializar → ejecutar bucle → limpiar recursos al salir.
     */
    public void ejecutar() {
        inicializar();      // Configurar GLFW, OpenGL y todos los sistemas
        bucclePrincipal();  // Ejecutar el game loop hasta que el usuario cierre
        limpiar();          // Liberar todos los recursos del sistema
    }

    /**
     * Inicializa GLFW, OpenGL, el renderer, el juego, el audio y el input.
     */
    private void inicializar() {
        // Redirigir errores de GLFW a stderr para facilitar el debugging
        GLFWErrorCallback.createPrint(System.err).set();

        // Inicializar la librería GLFW: debe ser la primera llamada a GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("No se pudo inicializar GLFW");
        }

        // Especificar que queremos OpenGL versión 3.3
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);       // Major: 3
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);       // Minor: 3 → v3.3

        // Usar el perfil core (sin funciones deprecated de OpenGL legacy)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        // La ventana no podrá ser redimensionada por el usuario
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        // Crear la ventana con el título inicial
        handle = glfwCreateWindow(ANCHO, ALTO, "Flappy Bird - 3 Jugadores", NULL, NULL);
        if (handle == NULL) {
            throw new RuntimeException("No se pudo crear la ventana GLFW");
        }

        // Hacer que el contexto OpenGL de esta ventana sea el activo en este hilo
        glfwMakeContextCurrent(handle);

        // Activar V-Sync: sincronizar con la tasa de refresco del monitor
        glfwSwapInterval(1);

        // Cargar las capacidades de OpenGL para este contexto (obligatorio en LWJGL3)
        GL.createCapabilities();

        // Definir el área de la ventana donde OpenGL dibujará (toda la ventana)
        glViewport(0, 0, ANCHO, ALTO);

        // Crear el juego (contiene pájaros, tuberías, lógica y estado)
        juego = new Juego();

        // Crear e inicializar el renderer (compila shaders, crea VAO/VBO)
        renderer = new Renderer();
        renderer.inicializar();

        // Crear e inicializar el sistema de audio OpenAL
        gestorSonido = new GestorSonido();
        gestorSonido.inicializar();

        // Pasar el gestor de sonido al juego para que pueda reproducir efectos
        juego.setGestorSonido(gestorSonido);

        // Registrar el callback de teclado (debe hacerse después de crear el juego)
        gestorEntrada = new GestorEntrada(handle, juego);
        gestorEntrada.inicializar();

        // Mostrar la ventana al usuario (antes estaba oculta durante la inicialización)
        glfwShowWindow(handle);
    }

    /**
     * Bucle principal del juego (game loop).
     *
     * Ejecuta continuamente hasta que el usuario cierra la ventana.
     * Usa deltaTime para que el movimiento sea independiente de los FPS.
     */
    private void bucclePrincipal() {
        // Registrar el tiempo del primer frame usando el reloj de alta resolución de GLFW
        double tiempoAnterior = glfwGetTime();

        // El bucle continúa mientras el usuario no haya pedido cerrar la ventana
        while (!glfwWindowShouldClose(handle)) {

            // --- CÁLCULO DE DELTATIME ---

            // Obtener el tiempo actual en segundos desde que se inició GLFW
            double tiempoActual = glfwGetTime();

            // Delta = tiempo transcurrido desde el frame anterior
            float deltaTime = (float)(tiempoActual - tiempoAnterior);

            // Actualizar el tiempo anterior para el siguiente frame
            tiempoAnterior = tiempoActual;

            // Limitar el deltaTime máximo para evitar saltos grandes si la ventana
            // se congela o el sistema va muy lento (ej: al mover la ventana)
            if (deltaTime > 0.033f) deltaTime = 0.033f;

            // --- ACTUALIZACIÓN DE LÓGICA ---

            // Actualizar toda la física, colisiones y estado del juego
            juego.actualizar(deltaTime);

            // Refrescar el título de la ventana con puntajes y nivel actuales
            actualizarTitulo();

            // --- RENDERIZADO ---

            // Limpiar el framebuffer con negro antes de dibujar el nuevo frame
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            // Dibujar el frame actual (fondo, elementos de juego, HUD)
            juego.dibujar(renderer);

            // Intercambiar los buffers front/back (mostrar el frame recién dibujado)
            glfwSwapBuffers(handle);

            // Procesar todos los eventos pendientes de GLFW (teclado, ratón, cierre)
            glfwPollEvents();
        }
    }

    /**
     * Actualiza el título de la ventana mostrando puntajes y nivel actuales.
     * Esto le da información al jugador sin ocupar espacio en pantalla.
     */
    private void actualizarTitulo() {
        String titulo = String.format(
            "Flappy Bird | J1: %d pts | J2: %d pts | J3: %d pts | Nivel: %d",
            juego.getPuntajeJ1(),   // Puntaje del jugador 1
            juego.getPuntajeJ2(),   // Puntaje del jugador 2
            juego.getPuntajeJ3(),   // Puntaje del jugador 3
            juego.getNivel()        // Nivel de dificultad actual
        );
        glfwSetWindowTitle(handle, titulo);
    }

    /**
     * Libera todos los recursos en el orden correcto para evitar memory leaks.
     */
    private void limpiar() {
        // Liberar los recursos del renderer (shaders, VAO, VBO de la GPU)
        renderer.limpiar();

        // Liberar los recursos de audio OpenAL (buffers, fuentes, contexto)
        gestorSonido.limpiar();

        // Destruir la ventana GLFW y liberar sus callbacks internos
        glfwDestroyWindow(handle);

        // Finalizar GLFW y liberar todos los recursos del sistema de ventanas
        glfwTerminate();

        // Liberar el callback de error para evitar un memory leak en LWJGL
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }
}
