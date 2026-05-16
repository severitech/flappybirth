package com.flappybird;

/**
 * Clase TextoPixel - Renderiza texto usando rectángulos pequeños (pixel font)
 *
 * Cada carácter está definido como una cuadrícula de 5x7 píxeles donde
 * 1 = cuadrado encendido y 0 = vacío. Permite mostrar texto sin necesidad
 * de cargar fuentes externas.
 */
public class TextoPixel {

    // Tamaño de un "píxel" del font en NDC, separado en X e Y porque la ventana
    // (800x600) no es cuadrada: 1px_x = 2/800 = 0.0025, 1px_y = 2/600 = 0.00333
    // Usamos 3 píxeles reales por punto de font (mismo TAM_PIXEL = 3 que antes)
    private static final float PIX_X = 3 * (2.0f / 800); // = 0.0075f por dot
    private static final float PIX_Y = 3 * (2.0f / 600); // = 0.0100f por dot

    // Ancho y alto de cada carácter en dots del font (sin cambio)
    private static final int CHAR_W = 5;
    private static final int CHAR_H = 7;

    // Separación entre caracteres en dots
    private static final int ESPACIADO = 2;

    /**
     * Dibuja una cadena de texto en la posición indicada
     *
     * @param renderer Renderer para dibujar rectángulos
     * @param texto    Texto a mostrar (mayúsculas, dígitos y algunos símbolos)
     * @param x        Posición X inicial (píxeles)
     * @param y        Posición Y inicial (píxeles)
     * @param escala   Factor de escala (1.0 = normal, 2.0 = doble tamaño)
     * @param r,g,b    Color del texto
     */
    public static void dibujar(Renderer renderer, String texto,
                                float x, float y, float escala,
                                float r, float g, float b) {
        // Tamaño de un dot en NDC para esta escala (X e Y separados por aspect ratio)
        float dotX = PIX_X * escala;
        float dotY = PIX_Y * escala;
        float cursorX = x;

        for (char c : texto.toUpperCase().toCharArray()) {
            int[] mapa = obtenerMapa(c);
            if (mapa != null) {
                for (int fila = 0; fila < CHAR_H; fila++) {
                    for (int col = 0; col < CHAR_W; col++) {
                        if (mapa[fila * CHAR_W + col] == 1) {
                            // Posición en NDC: cursor + columna * dotX
                            float px2 = cursorX + col * dotX;
                            // Invertir fila para que la base quede abajo (igual que antes)
                            float py2 = y + (CHAR_H - 1 - fila) * dotY;
                            // Cada dot es un rect de dotX × dotY en NDC
                            renderer.dibujarRect(px2, py2, dotX, dotY, r, g, b);
                        }
                    }
                }
            }
            // Avanzar cursor: (CHAR_W + ESPACIADO) dots en X
            cursorX += (CHAR_W + ESPACIADO) * dotX;
        }
    }

    /**
     * Calcula el ancho total en NDC de un texto para esta escala
     */
    public static float anchoTexto(String texto, float escala) {
        float dotX = PIX_X * escala;
        return texto.length() * (CHAR_W + ESPACIADO) * dotX;
    }

    /**
     * Retorna el mapa de bits 5x7 para el carácter indicado
     * El array tiene 35 elementos (7 filas x 5 columnas), fila 0 = arriba
     */
    private static int[] obtenerMapa(char c) {
        return switch (c) {
            case 'A' -> new int[]{
                0,1,1,1,0,
                1,0,0,0,1,
                1,0,0,0,1,
                1,1,1,1,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1
            };
            case 'B' -> new int[]{
                1,1,1,1,0,
                1,0,0,0,1,
                1,0,0,0,1,
                1,1,1,1,0,
                1,0,0,0,1,
                1,0,0,0,1,
                1,1,1,1,0
            };
            case 'C' -> new int[]{
                0,1,1,1,0,
                1,0,0,0,1,
                1,0,0,0,0,
                1,0,0,0,0,
                1,0,0,0,0,
                1,0,0,0,1,
                0,1,1,1,0
            };
            case 'D' -> new int[]{
                1,1,1,0,0,
                1,0,0,1,0,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,1,0,
                1,1,1,0,0
            };
            case 'E' -> new int[]{
                1,1,1,1,1,
                1,0,0,0,0,
                1,0,0,0,0,
                1,1,1,1,0,
                1,0,0,0,0,
                1,0,0,0,0,
                1,1,1,1,1
            };
            case 'F' -> new int[]{
                1,1,1,1,1,
                1,0,0,0,0,
                1,0,0,0,0,
                1,1,1,1,0,
                1,0,0,0,0,
                1,0,0,0,0,
                1,0,0,0,0
            };
            case 'G' -> new int[]{
                0,1,1,1,0,
                1,0,0,0,1,
                1,0,0,0,0,
                1,0,1,1,1,
                1,0,0,0,1,
                1,0,0,0,1,
                0,1,1,1,0
            };
            case 'H' -> new int[]{
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,1,1,1,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1
            };
            case 'I' -> new int[]{
                1,1,1,1,1,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                1,1,1,1,1
            };
            case 'J' -> new int[]{
                0,0,0,0,1,
                0,0,0,0,1,
                0,0,0,0,1,
                0,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                0,1,1,1,0
            };
            case 'K' -> new int[]{
                1,0,0,0,1,
                1,0,0,1,0,
                1,0,1,0,0,
                1,1,0,0,0,
                1,0,1,0,0,
                1,0,0,1,0,
                1,0,0,0,1
            };
            case 'L' -> new int[]{
                1,0,0,0,0,
                1,0,0,0,0,
                1,0,0,0,0,
                1,0,0,0,0,
                1,0,0,0,0,
                1,0,0,0,0,
                1,1,1,1,1
            };
            case 'M' -> new int[]{
                1,0,0,0,1,
                1,1,0,1,1,
                1,0,1,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1
            };
            case 'N' -> new int[]{
                1,0,0,0,1,
                1,1,0,0,1,
                1,0,1,0,1,
                1,0,0,1,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1
            };
            case 'O' -> new int[]{
                0,1,1,1,0,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                0,1,1,1,0
            };
            case 'P' -> new int[]{
                1,1,1,1,0,
                1,0,0,0,1,
                1,0,0,0,1,
                1,1,1,1,0,
                1,0,0,0,0,
                1,0,0,0,0,
                1,0,0,0,0
            };
            case 'Q' -> new int[]{
                0,1,1,1,0,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,1,0,1,
                1,0,0,1,0,
                0,1,1,0,1
            };
            case 'R' -> new int[]{
                1,1,1,1,0,
                1,0,0,0,1,
                1,0,0,0,1,
                1,1,1,1,0,
                1,0,1,0,0,
                1,0,0,1,0,
                1,0,0,0,1
            };
            case 'S' -> new int[]{
                0,1,1,1,1,
                1,0,0,0,0,
                1,0,0,0,0,
                0,1,1,1,0,
                0,0,0,0,1,
                0,0,0,0,1,
                1,1,1,1,0
            };
            case 'T' -> new int[]{
                1,1,1,1,1,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0
            };
            case 'U' -> new int[]{
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                0,1,1,1,0
            };
            case 'V' -> new int[]{
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                0,1,0,1,0,
                0,0,1,0,0
            };
            case 'W' -> new int[]{
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                1,0,1,0,1,
                1,0,1,0,1,
                1,1,0,1,1,
                1,0,0,0,1
            };
            case 'X' -> new int[]{
                1,0,0,0,1,
                0,1,0,1,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,1,0,1,0,
                1,0,0,0,1
            };
            case 'Y' -> new int[]{
                1,0,0,0,1,
                1,0,0,0,1,
                0,1,0,1,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0
            };
            case 'Z' -> new int[]{
                1,1,1,1,1,
                0,0,0,0,1,
                0,0,0,1,0,
                0,0,1,0,0,
                0,1,0,0,0,
                1,0,0,0,0,
                1,1,1,1,1
            };
            case '0' -> new int[]{
                0,1,1,1,0,
                1,0,0,1,1,
                1,0,1,0,1,
                1,1,0,0,1,
                1,0,0,0,1,
                1,0,0,0,1,
                0,1,1,1,0
            };
            case '1' -> new int[]{
                0,0,1,0,0,
                0,1,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,1,1,1,0
            };
            case '2' -> new int[]{
                0,1,1,1,0,
                1,0,0,0,1,
                0,0,0,0,1,
                0,0,0,1,0,
                0,0,1,0,0,
                0,1,0,0,0,
                1,1,1,1,1
            };
            case '3' -> new int[]{
                1,1,1,1,0,
                0,0,0,0,1,
                0,0,0,0,1,
                0,1,1,1,0,
                0,0,0,0,1,
                0,0,0,0,1,
                1,1,1,1,0
            };
            case '4' -> new int[]{
                0,0,0,1,0,
                0,0,1,1,0,
                0,1,0,1,0,
                1,0,0,1,0,
                1,1,1,1,1,
                0,0,0,1,0,
                0,0,0,1,0
            };
            case '5' -> new int[]{
                1,1,1,1,1,
                1,0,0,0,0,
                1,0,0,0,0,
                1,1,1,1,0,
                0,0,0,0,1,
                0,0,0,0,1,
                1,1,1,1,0
            };
            case '6' -> new int[]{
                0,1,1,1,0,
                1,0,0,0,0,
                1,0,0,0,0,
                1,1,1,1,0,
                1,0,0,0,1,
                1,0,0,0,1,
                0,1,1,1,0
            };
            case '7' -> new int[]{
                1,1,1,1,1,
                0,0,0,0,1,
                0,0,0,1,0,
                0,0,1,0,0,
                0,1,0,0,0,
                0,1,0,0,0,
                0,1,0,0,0
            };
            case '8' -> new int[]{
                0,1,1,1,0,
                1,0,0,0,1,
                1,0,0,0,1,
                0,1,1,1,0,
                1,0,0,0,1,
                1,0,0,0,1,
                0,1,1,1,0
            };
            case '9' -> new int[]{
                0,1,1,1,0,
                1,0,0,0,1,
                1,0,0,0,1,
                0,1,1,1,1,
                0,0,0,0,1,
                0,0,0,0,1,
                0,1,1,1,0
            };
            case ':' -> new int[]{
                0,0,0,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,0,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,0,0,0
            };
            case '-' -> new int[]{
                0,0,0,0,0,
                0,0,0,0,0,
                0,0,0,0,0,
                1,1,1,1,1,
                0,0,0,0,0,
                0,0,0,0,0,
                0,0,0,0,0
            };
            case '!' -> new int[]{
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,0,0,0,
                0,0,1,0,0
            };
            case '|' -> new int[]{
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0,
                0,0,1,0,0
            };
            case ' ' -> null; // Espacio en blanco, no dibuja nada
            default  -> null;
        };
    }
}
