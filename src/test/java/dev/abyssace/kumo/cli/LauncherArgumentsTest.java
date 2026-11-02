package dev.abyssace.kumo.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/** Pruebas del proceso lanzado sin Ollama: ejecuta solo una clase de eco creada por el test. */
class LauncherArgumentsTest {
    @TempDir Path directory;
    private static final List<String> ARGUMENTS = List.of("ask", "texto con espacios", "",
            "{\"message\":\"hola\"}", "$literal", "comilla 'simple' y \"doble\"",
            "español áéíóú", "C:\\carpeta con espacios\\", "--language", "es");

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void powershellConservaArgumentosVaciosAcentosComillasYBarras() throws Exception {
        Path launcher = prepare("kumo.ps1");
        StringBuilder script = new StringBuilder("$ProgressPreference='SilentlyContinue'; & ").append(quote(launcher.toString()));
        for (String argument : ARGUMENTS) { script.append(' ').append(quote(argument)); }
        String encoded = Base64.getEncoder().encodeToString(script.toString().getBytes(StandardCharsets.UTF_16LE));
        run(List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-OutputFormat", "Text",
                "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded));
    }

    @Test
    void shellConservaArgumentosVaciosAcentosComillasYBarras() throws Exception {
        Path launcher = prepare("kumo");
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path shell = windows ? Path.of(System.getenv("ProgramFiles"), "Git", "bin", "bash.exe") : Path.of("/bin/sh");
        assumeTrue(Files.isRegularFile(shell), "Se necesita un shell POSIX; Windows nativo se prueba por separado.");
        // Un archivo de prueba evita confundir el parseo Java->MSYS con el del propio lanzador.
        StringBuilder invocation = new StringBuilder("exec sh ")
                .append(shellQuote(launcher.toString().replace('\\', '/')));
        for (String argument : ARGUMENTS) { invocation.append(' ').append(shellQuote(argument)); }
        Path runner = directory.resolve("invoke.sh");
        Files.writeString(runner, invocation + "\n");
        run(List.of(shell.toString(), runner.toString().replace('\\', '/')));
    }

    private Path prepare(String name) throws Exception {
        Path root = directory.resolve("portable con espacios");
        Path bin = Files.createDirectories(root.resolve("bin"));
        Files.createDirectories(root.resolve("lib"));
        Path source = directory.resolve("EchoArgs.java");
        Files.writeString(source, """
                import java.nio.charset.StandardCharsets;
                import java.util.Base64;
                public final class EchoArgs {
                    public static void main(String[] args) {
                        for (String value : args) {
                            System.out.println(Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));
                        }
                    }
                }
                """);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", directory.toString(), source.toString()));
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "EchoArgs");
        try (var jar = new JarOutputStream(Files.newOutputStream(root.resolve("kumo.jar")), manifest)) {
            jar.putNextEntry(new JarEntry("EchoArgs.class"));
            jar.write(Files.readAllBytes(directory.resolve("EchoArgs.class")));
            jar.closeEntry();
        }
        return Files.copy(Path.of("bin", name), bin.resolve(name));
    }

    private void run(List<String> command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true);
        builder.environment().put("JAVA_HOME", System.getProperty("java.home").replace('\\', '/'));
        Process process = builder.start();
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        if (!completed) { process.destroyForcibly(); }
        assertTrue(completed, "El lanzador debe terminar.");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
        List<String> expected = ARGUMENTS.stream()
                .map(s -> Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8))).toList();
        assertEquals(expected, output.lines().toList(), output);
    }

    private String quote(String value) { return "'" + value.replace("'", "''") + "'"; }
    private String shellQuote(String value) { return "'" + value.replace("'", "'\"'\"'") + "'"; }
}
