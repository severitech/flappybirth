package com.flappybird;

import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;

/**
 * Clase Renderer - Encargada de todo el renderizado con OpenGL
 *
 * Compila los shaders, crea los buffers (VAO/VBO) y expone métodos para
 * dibujar rectángulos, triángulos y círculos en la pantalla usando OpenGL 3.3 core.
 */
public class Renderer {

    // Identificador del programa de shaders compilado en la GPU
    private int programaShader;

    // VAO = Vertex Array Object: guarda la configuración de atributos de vértices
    private int vao;

    // VBO = Vertex Buffer Object: guarda los datos de vértices en la GPU
    private int vbo;

    // Código fuente del vertex shader: transforma posiciones de píxeles a NDC
    private static final String VERTEX_SHADER_SRC =
            "#version 330 core\n" +
            "layout (location = 0) in vec2 aPos;\n" +
            "uniform vec2 offset;\n" +   // Posición en pantalla (píxeles)
            "uniform vec2 scale;\n" +    // Escala (ancho, alto)
            "void main() {\n" +
            "    vec2 pos = (aPos * scale + offset);\n" +
            // Convertir de píxeles a NDC: dividir por la mitad del tamaño de ventana
            "    pos.x = (pos.x / 400.0) - 1.0;\n" +  // 400 = mitad del ancho (800/2)
            "    pos.y = (pos.y / 300.0) - 1.0;\n" +  // 300 = mitad del alto  (600/2)
            "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
            "}\n";

    // Código fuente del fragment shader: pinta cada fragmento con el color uniforme
    private static final String FRAGMENT_SHADER_SRC =
            "#version 330 core\n" +
            "out vec4 FragColor;\n" +
            "uniform vec3 color;\n" +   // Color RGB pasado desde Java
            "void main() {\n" +
            "    FragColor = vec4(color, 1.0);\n" +
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

        // Definir los vértices de un quad unitario (de 0 a 1 en X e Y)
        // Dos triángulos forman un rectángulo completo
        float[] vertices = {
            0.0f, 0.0f,   // Vértice inferior-izquierdo
            1.0f, 0.0f,   // Vértice inferior-derecho
            1.0f, 1.0f,   // Vértice superior-derecho

            0.0f, 0.0f,   // Vértice inferior-izquierdo
            1.0f, 1.0f,   // Vértice superior-derecho
            0.0f, 1.0f    // Vértice superior-izquierdo
        };

        // Crear el VBO y subir los datos de vértices a la GPU
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        // Indicar a OpenGL cómo leer los datos del VBO:
        // location=0, 2 floats, sin normalizar, stride=8 bytes, offset=0
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Desenlazar para evitar modificaciones accidentales
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
        // Activar el programa de shaders
        glUseProgram(programaShader);

        // Pasar la posición como uniforme al shader
        int locOffset = glGetUniformLocation(programaShader, "offset");
        glUniform2f(locOffset, x, y);

        // Pasar la escala (ancho, alto) como uniforme al shader
        int locScale = glGetUniformLocation(programaShader, "scale");
        glUniform2f(locScale, ancho, alto);

        // Pasar el color como uniforme al shader
        int locColor = glGetUniformLocation(programaShader, "color");
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
        // Activar shaders
        glUseProgram(programaShader);

        // Para el triángulo usamos offset=0,0 y scale=1,1 para pasar coords directas
        int locOffset = glGetUniformLocation(programaShader, "offset");
        glUniform2f(locOffset, 0, 0);

        int locScale = glGetUniformLocation(programaShader, "scale");
        glUniform2f(locScale, 1, 1);

        int locColor = glGetUniformLocation(programaShader, "color");
        glUniform3f(locColor, r, g, b);

        // Crear VAO/VBO temporal para el triángulo
        int triVao = glGenVertexArrays();
        glBindVertexArray(triVao);

        int triVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, triVbo);

        // Los tres vértices del triángulo en píxeles (el shader los convierte a NDC)
        float[] verts = { x1, y1, x2, y2, x3, y3 };
        glBufferData(GL_ARRAY_BUFFER, verts, GL_STREAM_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Dibujar el triángulo
        glDrawArrays(GL_TRIANGLES, 0, 3);

        // Limpiar los recursos temporales inmediatamente
        glBindVertexArray(0);
        glDeleteBuffers(triVbo);
        glDeleteVertexArrays(triVao);
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
        // Activar shaders con offset y scale neutros
        glUseProgram(programaShader);

        int locOffset = glGetUniformLocation(programaShader, "offset");
        glUniform2f(locOffset, 0, 0);

        int locScale = glGetUniformLocation(programaShader, "scale");
        glUniform2f(locScale, 1, 1);

        int locColor = glGetUniformLocation(programaShader, "color");
        glUniform3f(locColor, r, g, b);

        // Calcular vértices del círculo: centro + un punto por segmento + cierre
        int totalVertices = segmentos + 2;
        float[] verts = new float[totalVertices * 2];

        // El primer vértice es el centro del círculo
        verts[0] = cx;
        verts[1] = cy;

        // Los siguientes vértices forman el borde del círculo
        for (int i = 0; i <= segmentos; i++) {
            // Ángulo actual en radianes
            double angulo = 2.0 * Math.PI * i / segmentos;
            verts[(i + 1) * 2]     = cx + (float)(Math.cos(angulo) * radio);
            verts[(i + 1) * 2 + 1] = cy + (float)(Math.sin(angulo) * radio);
        }

        // Crear VAO/VBO temporal
        int cirVao = glGenVertexArrays();
        glBindVertexArray(cirVao);

        int cirVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, cirVbo);
        glBufferData(GL_ARRAY_BUFFER, verts, GL_STREAM_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Triangle fan: primer vértice como centro, los demás forman el borde
        glDrawArrays(GL_TRIANGLE_FAN, 0, totalVertices);

        // Limpiar recursos temporales
        glBindVertexArray(0);
        glDeleteBuffers(cirVbo);
        glDeleteVertexArrays(cirVao);
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
    }
}
