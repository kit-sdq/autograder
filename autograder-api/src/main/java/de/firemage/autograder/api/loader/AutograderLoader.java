package de.firemage.autograder.api.loader;

import de.firemage.autograder.api.AbstractLinter;
import de.firemage.autograder.api.AbstractProblemType;
import de.firemage.autograder.api.AbstractTempLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Files;

public class AutograderLoader {
    private static final String AUTOGRADER_DOWNLOAD_URL = "https://github.com/kit-sdq/autograder/releases/latest/download/autograder-cmd.jar";
    private static final String AUTOGRADER_RELEASE_PATH = "https://github.com/kit-sdq/autograder/releases/latest";

    private static final Logger LOG = LoggerFactory.getLogger(AutograderLoader.class);

    private static URLClassLoader autograderClassLoader = null;
    private static String currentTag = null;
    private static Path downloadedAutograderPath = null;

    public static void loadFromFile(Path autograderPath) throws IOException {
        loadAutograderIntoProcess(autograderPath);
    }

    public static void loadFromGithubWithExtraChecks() throws IOException {
        String tag = getAutograderVersionTag();
        if (downloadedAutograderPath == null || !downloadedAutograderPath.getFileName().startsWith(tag)) {
            downloadAutograderRelease(tag);
        }
        currentTag = tag;
        loadAutograderIntoProcess(downloadedAutograderPath);
    }

    public static boolean isCurrentVersionLoaded() throws IOException {
        if (downloadedAutograderPath == null) {
            return true;
        }

        return getAutograderVersionTag().equals(currentTag);
    }

    public static boolean isAutograderLoaded() {
        return autograderClassLoader != null;
    }

    public static AbstractLinter instantiateLinter(AbstractLinter.Builder builder) {
        return new ImplementationBinder<>(AbstractLinter.class)
                .param(AbstractLinter.Builder.class, builder)
                .classLoader(autograderClassLoader)
                .instantiate();
    }

    public static AbstractTempLocation instantiateTempLocation(Path path) {
        return new ImplementationBinder<>(AbstractTempLocation.class)
                .param(Path.class, path)
                .classLoader(autograderClassLoader)
                .instantiate();
    }

    public static AbstractTempLocation instantiateTempLocation() {
        return new ImplementationBinder<>(AbstractTempLocation.class)
                .classLoader(autograderClassLoader)
                .instantiate();
    }

    public static AbstractProblemType convertProblemType(String problemType) {
        return new ImplementationBinder<>(AbstractProblemType.class)
                .param(String.class, problemType)
                .classLoader(autograderClassLoader)
                .callStatic("fromString", AbstractProblemType.class);
    }

    private static String getAutograderVersionTag() throws IOException {
        URLConnection connection = new URL(AUTOGRADER_RELEASE_PATH).openConnection();
        connection.connect();
        // Open stream to force redirect to the latest release
        try (var inputStream = connection.getInputStream()) {
            String[] components = connection.getURL().getFile().split("/");
            return components[components.length - 1];
        }
    }

    private static void downloadAutograderRelease(String tag) throws IOException {
        Path targetPath = Files.createTempFile(tag + "_autograder_jar", ".jar");
        LOG.info("Downloading autograder JAR with version/tag {} to {}", tag, targetPath.toAbsolutePath());
        Files.deleteIfExists(targetPath);
        Files.createFile(targetPath);
        URL url = new URL(AUTOGRADER_DOWNLOAD_URL);
        ReadableByteChannel channel = Channels.newChannel(url.openStream());
        try (var outStream = new FileOutputStream(targetPath.toFile())) {
            outStream.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
        }
        downloadedAutograderPath = targetPath;
    }

    private static void loadAutograderIntoProcess(Path jar) throws IOException {
        if (autograderClassLoader != null) {
            throw new IllegalStateException("Autograder already loaded. Restart the process to load a new version.");
        }

        if (!Files.exists(jar)) {
            throw new IOException("Autograder JAR not found at " + jar.toAbsolutePath());
        }

        LOG.info("Loading autograder JAR from {}", jar.toAbsolutePath());
        autograderClassLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, AutograderLoader.class.getClassLoader());
    }
}
