package com.flappybird;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Clase Ventana - Gestiona el ciclo de vida de la ventana y el bucle principal
 *
 * Inicializa GLFW y OpenGL, crea la ventana del juego, y ejecuta el game loop
 * que actualiza la lógica y renderiza cada frame usando deltaTime para asegurar
 * que el movimiento sea independiente de los FPS del hardware.
 */
public class Ventana {

    // Handle de la ventana GLFW (identificador opaco de la ventana nativa)
    private long handle;

    // Dimensiones de la ventana en píxeles
    private static final int ANCHO  = 800;
    private static final int ALTO   = 600;

    // Instancia del juego que contiene toda la lógica
    private Juego juego;

    // Renderer que maneja los shaders y el dibujado de primitivas
    private Renderer renderer;

    // Gestor de entrada que escucha el teclado
    private GestorEntrada gestorEntrada;

    /**
     * Método principal: inicializar, ejecutar el bucle y limpiar al salir
     */
    public void ejecutar() {
        inicializar();
        bucclePrincipal();
        limpiar();
    }

    /**
     * Inicializa GLFW, crea la ventana y configura el contexto OpenGL
     */
    private void inicializar() {
        // Redirigir errores de GLFW a stderr para facilitar el debugging
        GLFWErrorCallback.createPrint(System.err).set();

        // Inicializar GLFW: debe hacerse antes de cualquier llamada a GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("No se pudo inicializar GLFW");
        }

        // Configurar las pistas (hints) para la creación de la ventana
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);      // OpenGL versión 3.x
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);      // OpenGL versión x.3
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE); // Core profile
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);          // Ventana no redimensionable

        // Crear la ventana con el título inicial
        handle = glfwCreateWindow(ANCHO, ALTO, "Flappy Bird - 2 Jugadores", NULL, NULL);
        if (handle == NULL) {
            throw new RuntimeException("No se pudo crear la ventana GLFW");
        }

        // Hacer que el contexto OpenGL de esta ventana sea el activo en este hilo
        glfwMakeContextCurrent(handle);

        // Activar V-Sync (1 = sincronizar con el monitor, 0 = sin límite de FPS)
        glfwSwapInterval(1);

        // IMPORTANTE: crear las capabilities de OpenGL para este contexto
        // Sin esto, ninguna llamada a GL33 funcionará
        GL.createCapabilities();

        // Definir el viewport: el área de la ventana donde OpenGL dibujará
        glViewport(0, 0, ANCHO, ALTO);

        // Crear e inicializar todos los sistemas del juego
        juego = new Juego();
        renderer = new Renderer();
        renderer.inicializar();

        // Registrar el callback de teclado DESPUÉS de crear el juego
        gestorEntrada = new GestorEntrada(handle, juego);
        gestorEntrada.inicializar();

        // Mostrar la ventana al usuario
        glfwShowWindow(handle);
    }

    /**
     * Bucle principal del juego (game loop)
     *
     * Ejecuta continuamente hasta que el usuario cierra la ventana.
     * Calcula el deltaTime para que el movimiento sea independiente de los FPS.
     */
    private void bucclePrincipal() {
        // Tiempo del frame anterior (en segundos, usando el reloj de GLFW)
        double tiempoAnterior = glfwGetTime();

        // El bucle continúa mientras el usuario no haya pedido cerrar la ventana
        while (!glfwWindowShouldClose(handle)) {

            // --- CÁLCULO DE DELTATIME ---
            double tiempoActual = glfwGetTime();
            float deltaTime = (float)(tiempoActual - tiempoAnterior);
            tiempoAnterior = tiempoActual;

            // Limitar deltaTime para evitar saltos grandes si la ventana se congela
            if (deltaTime > 0.05f) deltaTime = 0.05f;

            // --- ACTUALIZACIÓN DE LÓGICA ---
            juego.actualizar(deltaTime);

            // Actualizar el título de la ventana con puntajes y nivel actuales
            actualizarTitulo();

            // --- RENDERIZADO ---
            // Limpiar el buffer de color con un color negro
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            // Dibujar el frame actual
            juego.dibujar(renderer);

            // Intercambiar los buffers (mostrar lo que se acaba de dibujar)
            glfwSwapBuffers(handle);

            // Procesar todos los eventos pendientes de GLFW (input, resize, etc.)
            glfwPollEvents();
        }
    }

    /**
     * Actualiza el título de la ventana con los puntajes y el nivel actuales
     */
    private void actualizarTitulo() {
        String titulo = String.format(
            "Flappy Bird | J1: %d pts | J2: %d pts | Nivel: %d",
            juego.getPuntajeJ1(),
            juego.getPuntajeJ2(),
            juego.getNivel()
        );
        glfwSetWindowTitle(handle, titulo);
    }

    /**
     * Libera todos los recursos: OpenGL, renderer y GLFW
     */
    private void limpiar() {
        // Liberar los recursos del renderer (shaders, VAO, VBO)
        renderer.limpiar();

        // Destruir la ventana GLFW y liberar sus callbacks
        glfwDestroyWindow(handle);

        // Terminar GLFW y liberar todos los recursos del sistema
        glfwTerminate();

        // Liberar el callback de error (evitar memory leak)
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }
}
