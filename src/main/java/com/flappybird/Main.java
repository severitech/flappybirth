package com.flappybird;

/**
 * Clase Main - Punto de entrada de la aplicación Flappy Bird
 *
 * Esta clase es la primera en ejecutarse cuando se inicia el programa.
 * Su única responsabilidad es crear la ventana del juego y arrancar el bucle principal.
 */
public class Main {

    /**
     * Método principal del programa
     *
     * @param args Argumentos de la línea de comandos (no se usan en este juego)
     */
    public static void main(String[] args) {
        // Crear una instancia de la ventana principal del juego
        Ventana ventana = new Ventana();

        // Iniciar el ciclo de vida completo: inicializar, ejecutar y limpiar
        ventana.ejecutar();
    }
}
