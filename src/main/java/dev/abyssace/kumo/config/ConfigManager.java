package dev.abyssace.kumo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.abyssace.kumo.i18n.Messages;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Patrón: Repository de configuración + Factory Method (defaults).
 *
 * <p>Lee y escribe el archivo global {@code ~/.kumo/config.json}. La variable
 * de entorno {@code KUMO_HOME} permite mover esa carpeta (útil para portar la
 * configuración a otro disco) y la propiedad {@code -Dkumo.home} hace lo mismo
 * desde la JVM, pensada para pruebas y lanzadores avanzados. La escritura es
 * atómica para no dejar un JSON a medias si el proceso se corta.</p>
 */
public final class ConfigManager {

    public static final String KUMO_HOME_ENV = "KUMO_HOME";
    public static final String KUMO_HOME_PROPERTY = "kumo.home";
    public static final String CONFIG_FILE_NAME = "config.json";

    private final Path home;
    private final Path configFile;
    private final ObjectMapper mapper;

    public ConfigManager() {
        this(defaultHome());
    }

    public ConfigManager(Path home) {
        this.home = Objects.requireNonNull(home, "home").toAbsolutePath().normalize();
        this.configFile = this.home.resolve(CONFIG_FILE_NAME);
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /** Carpeta global de kumo, ya normalizada. */
    public Path home() {
        return home;
    }

    /** Ruta completa de {@code config.json}. */
    public Path configFile() {
        return configFile;
    }

    /** Indica si ya existe un archivo de configuración. */
    public boolean exists() {
        return Files.isRegularFile(configFile);
    }

    /**
     * Carga la configuración. Si no existe el archivo, devuelve los defaults de
     * la CLI (índice JSON) sin escribirlo.
     */
    public AppConfig load() {
        if (!exists()) {
            return ConfigFile.defaults().toAppConfig();
        }

        ConfigFile file;
        try {
            file = mapper.readValue(configFile.toFile(), ConfigFile.class);
        } catch (IOException ex) {
            throw new ConfigException(Messages.CONFIG_INVALID.formatted(configFile, ex.getMessage()), ex);
        }
        if (file == null) {
            throw new ConfigException(Messages.CONFIG_EMPTY.formatted(configFile));
        }
        return file.toAppConfig();
    }

    /** Carga la configuración y, si no existe, escribe los defaults. */
    public AppConfig loadOrCreate() {
        if (exists()) {
            return load();
        }
        AppConfig config = ConfigFile.defaults().toAppConfig();
        save(config);
        return config;
    }

    /** Guarda la configuración de forma atómica. */
    public void save(AppConfig config) {
        Objects.requireNonNull(config, "config");
        Path temp = configFile.resolveSibling(configFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(home);
            mapper.writeValue(temp.toFile(), ConfigFile.from(config));
            moveIntoPlace(temp);
        } catch (IOException ex) {
            throw new ConfigException(Messages.CONFIG_SAVE_ERROR.formatted(configFile, ex.getMessage()), ex);
        }
    }

    private void moveIntoPlace(Path temp) throws IOException {
        try {
            Files.move(temp, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, configFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Carpeta global por defecto: {@code -Dkumo.home}, {@code KUMO_HOME} o
     * {@code ~/.kumo}, por ese orden.
     */
    public static Path defaultHome() {
        String property = System.getProperty(KUMO_HOME_PROPERTY);
        if (property != null && !property.isBlank()) {
            return Path.of(property.strip()).toAbsolutePath().normalize();
        }
        return resolveHome(System.getenv(KUMO_HOME_ENV), System.getProperty("user.home"));
    }

    /** Variante pura, fácil de probar sin tocar variables de entorno. */
    static Path resolveHome(String kumoHome, String userHome) {
        if (kumoHome != null && !kumoHome.isBlank()) {
            return Path.of(kumoHome.strip()).toAbsolutePath().normalize();
        }
        if (userHome == null || userHome.isBlank()) {
            throw new ConfigException(Messages.CONFIG_HOME_UNKNOWN);
        }
        return Path.of(userHome, ".kumo").toAbsolutePath().normalize();
    }
}
