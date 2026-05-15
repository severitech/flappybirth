package com.flappybird;

import org.lwjgl.glfw.GLFW;

/**
 * Clase GestorEntrada - Maneja el input del teclado via GLFW
 *
 * Registra un callback de teclado que detecta las teclas presionadas
 * y notifica al juego para que reaccione: saltar, cambiar estado o cerrar.
 *
 * Controles:
 *   Jugador 1: ESPACIO para saltar
 *   Jugador 2: W o FLECHA ARRIBA para saltar
 *   ENTER: avanzar entre estados del juego
 *   ESC: cerrar la aplicación
 */
public class GestorEntrada {

    // Referencia al juego para poder llamar los métodos de acción
    private final Juego juego;

    // Handle de la ventana GLFW para registrar el callback y cerrarla al salir
    private final long ventanaHandle;

    /**
     * Constructor del gestor de entrada.
     *
     * @param ventanaHandle Handle de la ventana GLFW (obtenido con glfwCreateWindow)
     * @param juego         Instancia del juego que reacciona a las teclas
     */
    public GestorEntrada(long ventanaHandle, Juego juego) {
        this.ventanaHandle = ventanaHandle;   // Guardar handle de la ventana
        this.juego = juego;                   // Guardar referencia al juego
    }

    /**
     * Registra el callback de teclado en GLFW.
     *
     * El callback se invoca automáticamente por GLFW en cada llamada a
     * glfwPollEvents() cuando hay eventos de teclado pendientes.
     */
    public void inicializar() {
        // Registrar el callback usando una expresión lambda de Java
        // Parámetros: window=handle, key=código de tecla, scancode=código físico,
        //             action=PRESS/RELEASE/REPEAT, mods=modificadores (Shift, Ctrl...)
        GLFW.glfwSetKeyCallback(ventanaHandle, (window, key, scancode, action, mods) -> {

            // Solo reaccionar cuando la tecla se PRESIONA (no al soltarla)
            if (action == GLFW.GLFW_PRESS) {

                switch (key) {

                    case GLFW.GLFW_KEY_SPACE:
                        // ESPACIO → Jugador 1 salta
                        juego.saltarJugador1();
                        break;

                    case GLFW.GLFW_KEY_W:
                        // W → Jugador 2 salta (control alternativo)
                        juego.saltarJugador2();
                        break;

                    case GLFW.GLFW_KEY_UP:
                        // FLECHA ARRIBA → Jugador 2 salta (control principal)
                        juego.saltarJugador2();
                        break;

                    case GLFW.GLFW_KEY_ENTER:
                        // ENTER → Avanzar estado: MENU→JUGANDO, GAME_OVER→MENU
                        juego.presionarEnter();
                        break;

                    case GLFW.GLFW_KEY_ESCAPE:
                        // ESC → Solicitar cierre de la ventana al siguiente frame
                        GLFW.glfwSetWindowShouldClose(window, true);
                        break;

                    default:
                        // Ignorar cualquier otra tecla no mapeada
                        break;
                }
            }
        });
    }
}
