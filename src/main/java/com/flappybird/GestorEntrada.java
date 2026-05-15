package com.flappybird;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;

/**
 * Clase GestorEntrada - Maneja el input del teclado via GLFW
 *
 * Registra un callback de teclado que detecta las teclas presionadas
 * y notifica al juego para que reaccione: saltar, cambiar estado o cerrar.
 */
public class GestorEntrada {

    // Referencia al juego para poder llamar los métodos de acción
    private final Juego juego;

    // Handle de la ventana GLFW para registrar el callback y cerrarla
    private final long ventanaHandle;

    /**
     * Constructor del gestor de entrada
     *
     * @param ventanaHandle Handle de la ventana GLFW
     * @param juego         Instancia del juego que reacciona al input
     */
    public GestorEntrada(long ventanaHandle, Juego juego) {
        this.ventanaHandle = ventanaHandle;
        this.juego = juego;
    }

    /**
     * Registra el callback de teclado en GLFW
     *
     * El callback se llama automáticamente por GLFW cada vez que se
     * presiona o suelta una tecla.
     */
    public void inicializar() {
        // Registrar el callback de teclado usando lambda de Java
        GLFW.glfwSetKeyCallback(ventanaHandle, (window, key, scancode, action, mods) -> {

            // Solo reaccionar cuando se PRESIONA la tecla (no cuando se suelta)
            if (action == GLFW.GLFW_PRESS) {

                switch (key) {
                    case GLFW.GLFW_KEY_SPACE:
                        // ESPACIO → Jugador 1 salta
                        juego.saltarJugador1();
                        break;

                    case GLFW.GLFW_KEY_W:
                        // W → Jugador 2 salta
                        juego.saltarJugador2();
                        break;

                    case GLFW.GLFW_KEY_ENTER:
                        // ENTER → Avanzar entre estados del juego
                        // MENU → JUGANDO, GAME_OVER → MENU, etc.
                        juego.presionarEnter();
                        break;

                    case GLFW.GLFW_KEY_ESCAPE:
                        // ESC → Solicitar cierre de la ventana
                        GLFW.glfwSetWindowShouldClose(window, true);
                        break;

                    default:
                        // Ignorar cualquier otra tecla
                        break;
                }
            }
        });
    }
}
