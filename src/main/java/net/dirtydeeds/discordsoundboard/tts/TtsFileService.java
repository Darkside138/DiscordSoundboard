package net.dirtydeeds.discordsoundboard.tts;

import net.dirtydeeds.discordsoundboard.BotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Persists TTS audio bytes to the {@code tts/} subdirectory of the sounds folder
 * and periodically deletes files older than the configured TTL.
 */
@Service
public class TtsFileService {

    private static final Logger LOG = LoggerFactory.getLogger(TtsFileService.class);

    private final BotConfig botConfig;

    @Value("${tts.file-ttl-minutes:60}")
    private int fileTtlMinutes;

    public TtsFileService(BotConfig botConfig) {
        this.botConfig = botConfig;
    }

    /**
     * Write audio bytes to disk and return the absolute path of the saved file.
     * The returned path can be passed directly to Lavaplayer via
     * {@code SoundPlayer.playForUser()}.
     */
    public String saveAudio(TtsResult result) throws TtsException {
        try {
            Path ttsDir = getOrCreateTtsDirectory();
            String fileName = "tts_" + UUID.randomUUID() + "." + result.format();
            Path filePath = ttsDir.resolve(fileName);
            Files.write(filePath, result.audioBytes());
            LOG.debug("Saved TTS audio to {}", filePath);
            return filePath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new TtsException("Failed to save TTS audio file", e);
        }
    }

    /** Delete TTS files older than {@code tts.file-ttl-minutes} minutes. */
    @Scheduled(fixedDelayString = "${tts.cleanup-interval-ms:3600000}")
    public void cleanupOldFiles() {
        try {
            Path ttsDir = getOrCreateTtsDirectory();
            Instant cutoff = Instant.now().minus(fileTtlMinutes, ChronoUnit.MINUTES);
            Files.walkFileTree(ttsDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.creationTime().toInstant().isBefore(cutoff)) {
                        LOG.debug("Removing expired TTS file: {}", file);
                        Files.deleteIfExists(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.warn("Error during TTS file cleanup: {}", e.getMessage());
        }
    }

    private Path getOrCreateTtsDirectory() throws IOException {
        Path ttsDir = Paths.get(botConfig.getSoundFileDir()).resolve("tts");
        if (!Files.exists(ttsDir)) {
            Files.createDirectories(ttsDir);
        }
        return ttsDir;
    }
}