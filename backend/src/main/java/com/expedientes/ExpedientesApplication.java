package com.expedientes;

import com.expedientes.config.Escritorio;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@SpringBootApplication
@EnableScheduling
public class ExpedientesApplication {

    /**
     * Con --escritorio (lo pasa Expedientes.exe): si el programa ya está abierto solo abre el navegador;
     * si otro programa ocupa el puerto, lo avisa; si no, arranca el servidor, abre el navegador
     * y pone un ícono junto al reloj.
     */
    public static void main(String[] args) {
        boolean escritorio = Arrays.asList(args).contains("--escritorio");
        if (escritorio && Escritorio.puertoOcupado()) {
            if (Escritorio.esteProgramaAtiende()) {
                Escritorio.abrirNavegador();
            } else {
                Escritorio.avisarPuertoOcupado();
                System.exit(1);
            }
            return;
        }

        SpringApplication app = new SpringApplication(ExpedientesApplication.class);
        app.setHeadless(!escritorio); // el ícono junto al reloj necesita la parte gráfica de Java
        // Antes de conectar a la base: crea la carpeta de datos y trae la base vieja si hace falta
        app.addListeners((ApplicationEnvironmentPreparedEvent e) ->
                Escritorio.prepararCarpetaDatos(e.getEnvironment().getProperty("app.datos")));
        ConfigurableApplicationContext contexto = app.run(args);

        if (escritorio) {
            Escritorio.mostrarIcono(contexto);
            Escritorio.abrirNavegador();
        }
    }
}
