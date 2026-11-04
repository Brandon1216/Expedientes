package com.expedientes.config;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Lo necesario para usar el sistema como programa de escritorio en una sola computadora. */
public final class Escritorio {

    // Debe coincidir con server.port de application.properties
    private static final int PUERTO = 8080;
    private static final String DIRECCION = "http://localhost:" + PUERTO;

    private Escritorio() {
    }

    /** true si ya hay algo atendiendo en el puerto: este programa abierto antes, u otro distinto. */
    public static boolean puertoOcupado() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("localhost", PUERTO), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** true si quien atiende el puerto es Expedientes. Reintenta por si está terminando de arrancar. */
    public static boolean esteProgramaAtiende() {
        for (int intento = 0; intento < 3; intento++) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(DIRECCION + "/api/auth/identidad").openConnection();
                c.setConnectTimeout(1000);
                c.setReadTimeout(2000);
                try (InputStream cuerpo = c.getInputStream()) {
                    if (new String(cuerpo.readAllBytes(), StandardCharsets.UTF_8).contains("\"expedientes\"")) {
                        return true;
                    }
                }
            } catch (IOException e) {
                // Otro programa, o todavía no responde: se reintenta abajo
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public static void avisarPuertoOcupado() {
        JOptionPane.showMessageDialog(null,
                "Expedientes no puede abrirse porque otro programa está usando el puerto " + PUERTO + ".\n\n"
                        + "Cierra ese programa o reinicia la computadora e inténtalo de nuevo.",
                "Expedientes", JOptionPane.ERROR_MESSAGE);
    }

    /** Dirección para entrar desde otro equipo de la red, o "" si esta computadora no tiene red. */
    public static String direccionRed(int puerto) {
        try (DatagramSocket s = new DatagramSocket()) {
            // UDP no envía nada al conectar: solo hace que el sistema elija la tarjeta de red que se usa para salir
            s.connect(InetAddress.getByName("8.8.8.8"), 53);
            InetAddress local = s.getLocalAddress();
            return local.isAnyLocalAddress() ? "" : "http://" + local.getHostAddress() + ":" + puerto;
        } catch (IOException e) {
            return "";
        }
    }

    public static void abrirNavegador() {
        try {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", DIRECCION).start();
        } catch (IOException e) {
            // Sin navegador no hay nada más que hacer; el servidor sigue funcionando
        }
    }

    /**
     * Crea la carpeta de datos. Si aún no hay base ahí pero existe la de antes (expedientes.db
     * junto al programa), la copia para no perder nada. La original se queda como respaldo.
     */
    public static void prepararCarpetaDatos(String carpeta) {
        if (carpeta == null || carpeta.isBlank()) {
            return;
        }
        try {
            Path datos = Path.of(carpeta);
            Files.createDirectories(datos);
            Path base = datos.resolve("expedientes.db");
            Path anterior = Path.of("expedientes.db").toAbsolutePath();
            if (!Files.exists(base) && Files.exists(anterior) && !anterior.equals(base.toAbsolutePath())) {
                Files.copy(anterior, base);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo preparar la carpeta de datos " + carpeta, e);
        }
    }

    /** Ícono junto al reloj de Windows con las opciones Abrir y Cerrar. */
    public static void mostrarIcono(ConfigurableApplicationContext contexto) {
        if (!SystemTray.isSupported()) {
            return;
        }
        URL logo = Escritorio.class.getResource("/static/logo.png");
        Image imagen = logo != null ? Toolkit.getDefaultToolkit().getImage(logo)
                : Toolkit.getDefaultToolkit().createImage(new byte[0]);

        PopupMenu menu = new PopupMenu();
        MenuItem abrir = new MenuItem("Abrir Expedientes");
        abrir.addActionListener(e -> abrirNavegador());
        MenuItem cerrar = new MenuItem("Cerrar Expedientes");
        cerrar.addActionListener(e -> System.exit(SpringApplication.exit(contexto)));
        menu.add(abrir);
        menu.addSeparator();
        menu.add(cerrar);

        TrayIcon icono = new TrayIcon(imagen, "Expedientes — " + DIRECCION, menu);
        icono.setImageAutoSize(true);
        icono.addActionListener(e -> abrirNavegador()); // doble clic en el ícono
        try {
            SystemTray.getSystemTray().add(icono);
        } catch (Exception e) {
            // Sin ícono el programa funciona igual
        }
    }
}
