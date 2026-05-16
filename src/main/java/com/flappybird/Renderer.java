package com.flappybird;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgramiv;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderiv;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * Clase Renderer - Encargada de todo el renderizado con OpenGL
 *
 * Compila los shaders, crea los buffers (VAO/VBO) y expone métodos para
 * dibujar rectángulos, triángulos y círculos en la pantalla usando OpenGL 3.3 core.
 */
public class Renderer {

    // Identificador del programa de shaders compilado en la GPU
    private int programaShader;

    // VAO/VBO del quad estático reutilizable (rectángulos)
    private int vao;
    private int vbo;

    // VAO/VBO dinámico reutilizable para triángulos y círculos.
    // Se pre-asignan una sola vez y se actualizan con glBufferData cada llamada,
    // evitando crear/destruir objetos de GPU cada frame (causa de parpadeos).
    private int vaoDin;
    private int vboDin;

    // -------------------------------------------------------------------------
    //
    // Entrada : vec3 aPos  —
    // Salida  : gl_Position en NDC directo,
    // Operación: finalPos = aPos.xy * uScale + uOffset  —
    //
    // uOffset y uScale llegan en NDC (-1..1) desde Java,
    // Ya NO se hace ninguna conversión de píxeles a NDC aquí: todo el juego
    // -------------------------------------------------------------------------
    private static final String VERTEX_SHADER_SRC =
            "#version 330 core\n" +
            "layout (location = 0) in vec3 aPos;\n" +
            // Posición NDC del origen del quad (esquina inferior-izquierda)
            "uniform vec2 uOffset;\n" +
            // Tamaño NDC del quad (ancho, alto)
            "uniform vec2 uScale;\n" +
            "void main() {\n" +
            // Misma línea clave que usa el : escala el quad y lo traslada en NDC
            "    vec2 finalPos = aPos.xy * uScale + uOffset;\n" +
            "    gl_Position = vec4(finalPos, aPos.z, 1.0);\n" +
            "}\n";

    // -------------------------------------------------------------------------
    // FRAGMENT SHADER - idéntico al del ingeniero (AppFlappyBird)
    //
    // uColor : vec3 RGB uniforme 
    // Salida : fragColor con alpha = 1.0 
    // -------------------------------------------------------------------------
    private static final String FRAGMENT_SHADER_SRC =
            "#version 330 core\n" +
            "out vec4 fragColor;\n" +
            "uniform vec3 uColor;\n" +
            "void main() {\n" +
            "    fragColor = vec4(uColor, 1.0);\n" +
            "}\n";

    /**
     * Inicializa el renderer: compila shaders y crea el quad base en la GPU
     */
    public void inicializar() {
        // Compilar y enlazar los shaders en un programa de GPU
        programaShader = crearProgramaShader();

        // Crear el VAO que almacenará la configuración del quad
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // Quad base unitario: de 0 a 1 en X e Y, z = 0 siempre.
        // Dos triángulos (6 vértices) forman el rectángulo reutilizable.
        float[] vertices = {
            0.0f, 0.0f, 0.0f,   // Vértice inferior-izquierdo
            1.0f, 0.0f, 0.0f,   // Vértice inferior-derecho
            1.0f, 1.0f, 0.0f,   // Vértice superior-derecho

            0.0f, 0.0f, 0.0f,   // Vértice inferior-izquierdo
            1.0f, 1.0f, 0.0f,   // Vértice superior-derecho
            0.0f, 1.0f, 0.0f    // Vértice superior-izquierdo
        };

        // Crear el VBO y subir los datos de vértices a la GPU
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        // Indicar a OpenGL cómo leer los datos del VBO:
        // stride=12 bytes (3 floats * 4 bytes), offset=0
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Desenlazar para evitar modificaciones accidentales
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        // VAO/VBO dinámico: pre-asignado vacío, se rellena en cada llamada
        // con glBufferData(..., GL_STREAM_DRAW) sin crear nuevos objetos de GPU
        vaoDin = glGenVertexArrays();
        glBindVertexArray(vaoDin);
        vboDin = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboDin);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    /**
     * Dibuja un rectángulo sólido en la posición y escala indicadas
     *
     * @param x      Posición X del borde izquierdo (píxeles)
     * @param y      Posición Y del borde inferior (píxeles)
     * @param ancho  Ancho del rectángulo (píxeles)
     * @param alto   Alto del rectángulo (píxeles)
     * @param r      Componente roja del color (0.0 - 1.0)
     * @param g      Componente verde del color (0.0 - 1.0)
     * @param b      Componente azul del color (0.0 - 1.0)
     */
    public void dibujarRect(float x, float y, float ancho, float alto,
                            float r, float g, float b) {
        glUseProgram(programaShader);

        int locOffset = glGetUniformLocation(programaShader, "uOffset");
        glUniform2f(locOffset, x, y);

        int locScale = glGetUniformLocation(programaShader, "uScale");
        glUniform2f(locScale, ancho, alto);

        int locColor = glGetUniformLocation(programaShader, "uColor");
        glUniform3f(locColor, r, g, b);

        // Enlazar el VAO del quad base y dibujar 6 vértices (2 triángulos)
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }

    /**
     * Dibuja un triángulo definido por tres puntos en coordenadas de píxeles
     *
     * @param x1, y1  Primer vértice
     * @param x2, y2  Segundo vértice
     * @param x3, y3  Tercer vértice
     * @param r, g, b Color RGB
     */
    public void dibujarTriangulo(float x1, float y1, float x2, float y2,
                                  float x3, float y3,
                                  float r, float g, float b) {
        glUseProgram(programaShader);

        // uOffset=(0,0) y uScale=(1,1): los vértices ya vienen en píxeles absolutos,
        // así que no se aplica traslación ni escala extra (solo la conversión a NDC)
        int locOffset = glGetUniformLocation(programaShader, "uOffset");
        glUniform2f(locOffset, 0, 0);

        int locScale = glGetUniformLocation(programaShader, "uScale");
        glUniform2f(locScale, 1, 1);

        int locColor = glGetUniformLocation(programaShader, "uColor");
        glUniform3f(locColor, r, g, b);

        // Reutilizar el VAO/VBO dinámico: solo actualizamos los datos, sin crear objetos nuevos
        float[] verts = { x1, y1, 0.0f, x2, y2, 0.0f, x3, y3, 0.0f };
        glBindVertexArray(vaoDin);
        glBindBuffer(GL_ARRAY_BUFFER, vboDin);
        glBufferData(GL_ARRAY_BUFFER, verts, GL_STREAM_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    /**
     * Dibuja un círculo aproximado usando un triangle fan
     *
     * @param cx      Centro X (píxeles)
     * @param cy      Centro Y (píxeles)
     * @param radio   Radio en píxeles
     * @param segmentos Cuántos triángulos usar (más = más suave)
     * @param r, g, b Color RGB
     */
    public void dibujarCirculo(float cx, float cy, float radio, int segmentos,
                                float r, float g, float b) {
        glUseProgram(programaShader);

        // Sin traslación ni escala extra: coords absolutas en píxeles
        int locOffset = glGetUniformLocation(programaShader, "uOffset");
        glUniform2f(locOffset, 0, 0);

        int locScale = glGetUniformLocation(programaShader, "uScale");
        glUniform2f(locScale, 1, 1);

        int locColor = glGetUniformLocation(programaShader, "uColor");
        glUniform3f(locColor, r, g, b);

        // Calcular vértices del círculo: centro + un punto por segmento + cierre
        // Cada vértice es vec3 (x, y, z=0) para coincidir con la firma del shader
        int totalVertices = segmentos + 2;
        float[] verts = new float[totalVertices * 3]; // 3 floats por vértice

        // Primer vértice: centro del círculo
        verts[0] = cx;
        verts[1] = cy;
        verts[2] = 0.0f; // z = 0

        // Vértices del borde, distribuidos en ángulos iguales
        for (int i = 0; i <= segmentos; i++) {
            double angulo = 2.0 * Math.PI * i / segmentos;
            verts[(i + 1) * 3]     = cx + (float)(Math.cos(angulo) * radio);
            verts[(i + 1) * 3 + 1] = cy + (float)(Math.sin(angulo) * radio);
            verts[(i + 1) * 3 + 2] = 0.0f; // z = 0
        }

        // Reutilizar el VAO/VBO dinámico igual que en dibujarTriangulo
        glBindVertexArray(vaoDin);
        glBindBuffer(GL_ARRAY_BUFFER, vboDin);
        glBufferData(GL_ARRAY_BUFFER, verts, GL_STREAM_DRAW);
        glDrawArrays(GL_TRIANGLE_FAN, 0, totalVertices);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    /**
     * Compila vertex y fragment shader, los enlaza en un programa y lo retorna
     *
     * @return ID del programa de shaders en la GPU
     */
    private int crearProgramaShader() {
        // Compilar el vertex shader
        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, VERTEX_SHADER_SRC);
        glCompileShader(vs);
        verificarErrorShader(vs, "VERTEX");

        // Compilar el fragment shader
        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, FRAGMENT_SHADER_SRC);
        glCompileShader(fs);
        verificarErrorShader(fs, "FRAGMENT");

        // Enlazar ambos shaders en un programa completo
        int programa = glCreateProgram();
        glAttachShader(programa, vs);
        glAttachShader(programa, fs);
        glLinkProgram(programa);
        verificarErrorPrograma(programa);

        // Los shaders individuales ya no son necesarios una vez enlazados
        glDeleteShader(vs);
        glDeleteShader(fs);

        return programa;
    }

    /**
     * Verifica si hubo errores al compilar un shader e imprime el log
     */
    private void verificarErrorShader(int shader, String tipo) {
        int[] exito = new int[1];
        glGetShaderiv(shader, GL_COMPILE_STATUS, exito);
        if (exito[0] == GL_FALSE) {
            System.err.println("ERROR AL COMPILAR SHADER " + tipo + ":");
            System.err.println(glGetShaderInfoLog(shader));
        }
    }

    /**
     * Verifica si hubo errores al enlazar el programa de shaders
     */
    private void verificarErrorPrograma(int programa) {
        int[] exito = new int[1];
        glGetProgramiv(programa, GL_LINK_STATUS, exito);
        if (exito[0] == GL_FALSE) {
            System.err.println("ERROR AL ENLAZAR PROGRAMA DE SHADERS:");
            System.err.println(glGetProgramInfoLog(programa));
        }
    }

    /**
     * Libera los recursos de GPU cuando ya no se necesitan
     */
    public void limpiar() {
        glDeleteProgram(programaShader);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vboDin);
        glDeleteVertexArrays(vaoDin);
    }
}
