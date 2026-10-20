package com.garepas.garepasapp.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

  





@Component
@Profile("!dev")
public class ValidacionProduccion implements BeanFactoryPostProcessor, EnvironmentAware {

    private static final List<String> OBLIGATORIAS = List.of(
            "DB_URL", "DB_USERNAME", "DB_PASSWORD", "ADMIN_PASSWORD");

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        List<String> faltantes = OBLIGATORIAS.stream()
                .filter(clave -> {
                    String valor = environment.getProperty(clave);
                    return valor == null || valor.isBlank();
                })
                .toList();
        if (!faltantes.isEmpty()) {
            throw new IllegalStateException(
                    "Configuracion de produccion incompleta: faltan variables de entorno obligatorias "
                            + faltantes + ". Definilas en el entorno o en un archivo .env "
                            + "(ver .env.example). Para desarrollo local usa el perfil dev.");
        }
    }
}