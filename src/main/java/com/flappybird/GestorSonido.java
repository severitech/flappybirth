package com.flappybird;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Clase GestorSonido - Genera y reproduce efectos de sonido usando OpenAL
 *
 * Crea sonidos proceduralmente (sin archivos externos) mediante ondas
 * sinusoidales generadas en memoria y subidas a buffers de OpenAL.
 * Soporta tres efectos: salto, punto anotado y game over.
 */
public class GestorSonido {

    // Tasa de muestreo de audio en muestras por segundo (estándar CD)
    private static final int TASA_MUESTREO = 44100;

    // Handle del dispositivo de audio del sistema (micrófono/altavoces)
    private long dispositivo;

    // Handle del contexto de OpenAL (análogo al contexto de OpenGL)
    private long contexto;

    // ID del buffer OpenAL con el sonido de salto (tono agudo corto)
    private int bufferSalto;

    // ID del buffer OpenAL con el sonido de punto anotado (tono más agudo)
    private int bufferPunto;

    // ID del buffer OpenAL con el sonido de game over (tono descendente)
    private int bufferGameOver;

    // ID de la fuente de sonido (emisor virtual en el espacio 3D)
    private int fuente;

    // Bandera que indica si el sistema de audio se inicializó correctamente
    private boolean disponible = false;

    /**
     * Inicializa el subsistema de audio: abre el dispositivo, crea el contexto
     * y genera los buffers de sonido proceduralmente.
     */
    public void inicializar() {
        try {
            // Abrir el dispositivo de audio por defecto del sistema operativo
            dispositivo = alcOpenDevice((ByteBuffer) null);
            if (dispositivo == NULL) {
                // Si no hay dispositivo de audio, continuar sin sonido
                System.err.println("AUDIO: No se encontró dispositivo de audio");
                return;
            }

            // Crear el contexto de audio sin atributos especiales
            contexto = alcCreateContext(dispositivo, (IntBuffer) null);
            if (contexto == NULL) {
                // Si el contexto falla, liberar el dispositivo y continuar sin sonido
                System.err.println("AUDIO: No se pudo crear el contexto de audio");
                alcCloseDevice(dispositivo);
                return;
            }

            // Hacer que este contexto de audio sea el activo en el hilo actual
            alcMakeContextCurrent(contexto);

            // Cargar las capacidades de OpenAL para este dispositivo
            ALCCapabilities alcCaps = ALC.createCapabilities(dispositivo);

            // Inicializar las funciones AL del contexto actual
            AL.createCapabilities(alcCaps);

            // Generar sonido de salto: La5 (880 Hz), duración 100ms, volumen medio
            bufferSalto = generarTono(880f, 0.10f, 0.5f);

            // Generar sonido de punto: Do6 (1047 Hz), duración 150ms, más suave
            bufferPunto = generarTono(1047f, 0.15f, 0.45f);

            // Generar sonido de game over: tono descendente de 440 Hz a 110 Hz en 0.5s
            bufferGameOver = generarTonoDescendente(440f, 110f, 0.5f, 0.65f);

            // Crear la fuente de sonido 3D (emite el audio)
            fuente = alGenSources();

            // Volumen al máximo
            alSourcef(fuente, AL_GAIN, 1.0f);

            // Sin modificación de tono (pitch 1.0 = normal)
            alSourcef(fuente, AL_PITCH, 1.0f);

            // Posicionar la fuente en el origen del espacio 3D
            alSource3f(fuente, AL_POSITION, 0f, 0f, 0f);

            // Sistema de audio listo
            disponible = true;

        } catch (Exception e) {
            // Cualquier error de audio no debe crashear el juego
            System.err.println("AUDIO: Error de inicialización: " + e.getMessage());
        }
    }

    /**
     * Genera un tono sinusoidal puro y lo sube a un buffer de OpenAL.
     *
     * @param frecuencia Frecuencia del tono en Hz (ej: 440 = La4)
     * @param duracion   Duración en segundos
     * @param volumen    Amplitud del sonido entre 0.0 y 1.0
     * @return ID del buffer de OpenAL con el sonido generado
     */
    private int generarTono(float frecuencia, float duracion, float volumen) {
        // Calcular el número total de muestras de audio necesarias
        int numMuestras = (int)(TASA_MUESTREO * duracion);

        // Reservar memoria para las muestras (16 bits por muestra = short)
        ShortBuffer muestras = memAllocShort(numMuestras);

        for (int i = 0; i < numMuestras; i++) {
            // Tiempo en segundos de esta muestra
            float t = (float) i / TASA_MUESTREO;

            // Envolvente: sube en 5ms y baja en 20ms para evitar clicks
            float envolvente = Math.min(1.0f, Math.min(t / 0.005f, (duracion - t) / 0.020f));

            // Valor de la onda sinusoidal escalado por envolvente y volumen
            float valor = volumen * envolvente * (float) Math.sin(2 * Math.PI * frecuencia * t);

            // Convertir el rango -1..1 al rango de short (-32768..32767)
            muestras.put(i, (short)(valor * Short.MAX_VALUE));
        }

        // Crear el buffer en OpenAL y subir los datos PCM
        int buffer = alGenBuffers();
        alBufferData(buffer, AL_FORMAT_MONO16, muestras, TASA_MUESTREO);

        // Liberar la memoria Java (ya fue copiada a la GPU de audio)
        memFree(muestras);

        return buffer;
    }

    /**
     * Genera un tono que desciende gradualmente de frecuencia (efecto "game over").
     *
     * @param freqInicio Frecuencia inicial en Hz
     * @param freqFin    Frecuencia final en Hz (menor = más grave)
     * @param duracion   Duración total en segundos
     * @param volumen    Amplitud del sonido entre 0.0 y 1.0
     * @return ID del buffer de OpenAL con el sonido generado
     */
    private int generarTonoDescendente(float freqInicio, float freqFin,
                                        float duracion, float volumen) {
        // Número total de muestras
        int numMuestras = (int)(TASA_MUESTREO * duracion);

        // Reservar memoria para las muestras
        ShortBuffer muestras = memAllocShort(numMuestras);

        // Fase acumulada para evitar discontinuidades al cambiar frecuencia
        double fase = 0.0;

        for (int i = 0; i < numMuestras; i++) {
            // Tiempo actual en segundos
            float t = (float) i / TASA_MUESTREO;

            // Progreso lineal de 0.0 a 1.0
            float progreso = t / duracion;

            // Interpolación lineal de frecuencia: va de freqInicio a freqFin
            float frecActual = freqInicio + (freqFin - freqInicio) * progreso;

            // Envolvente: fade out suave en los últimos 50ms
            float envolvente = Math.min(1.0f, (duracion - t) / 0.05f);

            // Acumular la fase con la frecuencia actual (integración discreta)
            fase += 2 * Math.PI * frecActual / TASA_MUESTREO;

            // Calcular valor de la muestra
            float valor = volumen * envolvente * (float) Math.sin(fase);

            // Guardar como short de 16 bits
            muestras.put(i, (short)(valor * Short.MAX_VALUE));
        }

        // Subir al buffer de OpenAL
        int buffer = alGenBuffers();
        alBufferData(buffer, AL_FORMAT_MONO16, muestras, TASA_MUESTREO);

        // Liberar memoria Java
        memFree(muestras);

        return buffer;
    }

    /**
     * Reproduce el efecto de sonido del salto del pájaro.
     * Detiene cualquier sonido previo antes de reproducir.
     */
    public void reproducirSalto() {
        reproducir(bufferSalto);
    }

    /**
     * Reproduce el efecto de sonido al anotar un punto.
     */
    public void reproducirPunto() {
        reproducir(bufferPunto);
    }

    /**
     * Reproduce el efecto de sonido del game over.
     * Se llama una sola vez al perder.
     */
    public void reproducirGameOver() {
        reproducir(bufferGameOver);
    }

    /**
     * Método interno: asigna un buffer a la fuente y lo reproduce.
     *
     * @param buffer ID del buffer de OpenAL a reproducir
     */
    private void reproducir(int buffer) {
        // Si el audio no está disponible, ignorar silenciosamente
        if (!disponible) return;

        // Detener cualquier reproducción activa en la fuente
        alSourceStop(fuente);

        // Asignar el nuevo buffer a la fuente
        alSourcei(fuente, AL_BUFFER, buffer);

        // Iniciar la reproducción
        alSourcePlay(fuente);
    }

    /**
     * Libera todos los recursos de OpenAL al cerrar el juego.
     */
    public void limpiar() {
        // Si nunca se inicializó, no hay nada que liberar
        if (!disponible) return;

        // Eliminar la fuente de sonido de la GPU de audio
        alDeleteSources(fuente);

        // Eliminar los tres buffers de audio
        alDeleteBuffers(new int[]{bufferSalto, bufferPunto, bufferGameOver});

        // Desasociar el contexto del hilo actual
        alcMakeContextCurrent(NULL);

        // Destruir el contexto de audio
        alcDestroyContext(contexto);

        // Cerrar el dispositivo de audio del sistema
        alcCloseDevice(dispositivo);
    }
}
