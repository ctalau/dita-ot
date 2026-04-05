package org.dita.dost;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.stream.Stream;
import org.dita.dost.exception.DITAOTException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TopicCacheCorrectnessTest {

  @TempDir
  Path tempDir;

  @Test
  void cachedBuildMatchesDirectBuildAndUsesCache() throws Exception {
    final Path src = tempDir.resolve("src");
    Files.createDirectories(src);
    writeSources(src, "Initial content A.");

    final Path cacheDir = tempDir.resolve("cache-a");
    final Path outInitial = tempDir.resolve("out-initial");
    final Path outCached = tempDir.resolve("out-cached");
    final Path outDirect = tempDir.resolve("out-direct");
    final Path runTemp = tempDir.resolve("run-temp");
    Files.createDirectories(runTemp);

    runHtml5(src, outInitial, cacheDir, runTemp);

    writeSources(src, "Modified content A.");
    runHtml5(src, outCached, cacheDir, runTemp);
    final String cachedRunLog = latestLog(runTemp);

    final Path directCacheDir = tempDir.resolve("cache-b");
    runHtml5(src, outDirect, directCacheDir, runTemp);

    assertEquals(hashDir(outDirect), hashDir(outCached));
    assertTrue(cachedRunLog.contains("Cache hit for "), "Expected cached run to report at least one cache hit");
  }

  @Test
  void glossaryTargetChangeInvalidatesTopicCache() throws Exception {
    final Path src = tempDir.resolve("src-glossary");
    Files.createDirectories(src);
    writeGlossarySources(src, "XML");

    final Path cacheDir = tempDir.resolve("cache-glossary");
    final Path outInitial = tempDir.resolve("out-glossary-initial");
    final Path outCached = tempDir.resolve("out-glossary-cached");
    final Path outDirect = tempDir.resolve("out-glossary-direct");
    final Path runTemp = tempDir.resolve("run-temp-glossary");
    Files.createDirectories(runTemp);

    runHtml5(src, outInitial, cacheDir, runTemp);
    writeGlossarySources(src, "Extensible Markup Language");
    runHtml5(src, outCached, cacheDir, runTemp);
    final String cachedRunLog = latestLog(runTemp);

    runHtml5(src, outDirect, tempDir.resolve("cache-glossary-direct"), runTemp);

    assertEquals(hashDir(outDirect), hashDir(outCached));
    assertTrue(
      !cachedRunLog.contains("Cache hit for ") || !cachedRunLog.contains("topic.dita"),
      "Expected topic transform cache to be invalidated when glossary target changes"
    );
  }

  private void runHtml5(final Path srcDir, final Path outDir, final Path cacheDir, final Path runTemp)
    throws DITAOTException {
    final File ditaDir = new File("src/main").getAbsoluteFile();
    final ProcessorFactory pf = ProcessorFactory.newInstance(ditaDir);
    pf.setBaseTempDir(runTemp.toFile());
    final Processor processor = pf.newProcessor("html5");
    processor
      .setInput(srcDir.resolve("main.ditamap").toFile().getAbsoluteFile())
      .setOutputDir(outDir.toFile().getAbsoluteFile())
      .setProperty("dita.cache.dir", cacheDir.toAbsolutePath().toString())
      .setProperty("clean.temp", "yes")
      .run();
  }

  private void writeSources(final Path srcDir, final String topicAText) throws IOException {
    Files.writeString(
      srcDir.resolve("main.ditamap"),
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <map>
        <title>Cache correctness</title>
        <topicref href="topic-a.dita"/>
        <topicref href="topic-b.dita"/>
      </map>
      """,
      UTF_8
    );
    Files.writeString(
      srcDir.resolve("topic-a.dita"),
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <topic id="topic-a">
        <title>Topic A</title>
        <body><p>%s</p></body>
      </topic>
      """.formatted(topicAText),
      UTF_8
    );
    Files.writeString(
      srcDir.resolve("topic-b.dita"),
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <topic id="topic-b">
        <title>Topic B</title>
        <body><p>Stable content B.</p></body>
      </topic>
      """,
      UTF_8
    );
  }

  private String latestLog(final Path runTemp) throws IOException {
    try (Stream<Path> logs = Files.list(runTemp)) {
      final Path log = logs.filter(p -> p.getFileName().toString().endsWith(".log")).max(Comparator.naturalOrder()).orElseThrow();
      return Files.readString(log, UTF_8);
    }
  }

  private void writeGlossarySources(final Path srcDir, final String glossSurface) throws IOException {
    Files.writeString(
      srcDir.resolve("main.ditamap"),
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <map>
        <title>Glossary cache correctness</title>
        <topicref href="topic.dita"/>
      </map>
      """,
      UTF_8
    );
    Files.writeString(
      srcDir.resolve("topic.dita"),
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <topic id="topic">
        <title>Topic</title>
        <body>
          <p><term href="glossentry.dita#g">%s</term></p>
        </body>
      </topic>
      """.formatted(glossSurface),
      UTF_8
    );
    Files.writeString(
      srcDir.resolve("glossentry.dita"),
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <glossentry id="g">
        <glossterm>%s</glossterm>
      </glossentry>
      """.formatted(glossSurface),
      UTF_8
    );
  }

  private String hashDir(final Path dir) throws IOException {
    final MessageDigest digest = digest();
    try (Stream<Path> stream = Files.walk(dir)) {
      stream
        .filter(Files::isRegularFile)
        .sorted()
        .forEach(path -> {
          final Path rel = dir.relativize(path);
          digest.update(rel.toString().getBytes(UTF_8));
          digest.update((byte) 0);
          try {
            digest.update(Files.readAllBytes(path));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          digest.update((byte) 0);
        });
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  private MessageDigest digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
