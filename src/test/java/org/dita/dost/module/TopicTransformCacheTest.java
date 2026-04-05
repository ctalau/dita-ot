package org.dita.dost.module;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TopicTransformCacheTest {

  private final TopicTransformCache cache = new TopicTransformCache();

  @TempDir
  Path tempDir;

  @Test
  void storeAndRestore() throws Exception {
    final Path cacheDir = tempDir.resolve("cache");
    final Path out = tempDir.resolve("topic.html");
    Files.writeString(out, "<html>cached</html>", UTF_8);

    cache.store(cacheDir, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", out);

    Files.writeString(out, "new", UTF_8);
    final boolean restored = cache.restore(cacheDir, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", out);

    assertTrue(restored);
    assertEquals("<html>cached</html>", Files.readString(out, UTF_8));
  }

  @Test
  void restoreMiss() throws Exception {
    final Path out = tempDir.resolve("topic.html");
    final boolean restored = cache.restore(
      tempDir.resolve("cache"),
      "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
      out
    );
    assertFalse(restored);
    assertFalse(Files.exists(out));
  }

  @Test
  void differentKeyDoesNotRestoreStaleOutput() throws Exception {
    final Path cacheDir = tempDir.resolve("cache");
    final Path out = tempDir.resolve("topic.html");
    Files.writeString(out, "v1", UTF_8);
    cache.store(cacheDir, "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd", out);

    Files.writeString(out, "v2", UTF_8);
    final boolean restored = cache.restore(
      cacheDir,
      "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
      out
    );
    assertFalse(restored);
    assertEquals("v2", Files.readString(out, UTF_8));
  }

  @Test
  void storeMissingOutputNoop() throws Exception {
    final Path out = tempDir.resolve("missing.html");
    cache.store(tempDir.resolve("cache"), "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc", out);
    assertFalse(Files.exists(tempDir.resolve("cache")));
  }
}
