/*
 * This file is part of the DITA Open Toolkit project.
 *
 * Copyright 2026
 *
 * See the accompanying LICENSE file for applicable license.
 */
package org.dita.dost.module;

import java.io.*;
import java.nio.file.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class TopicTransformCache {

  private static final String SUFFIX = ".html.gz";

  boolean restore(final Path cacheDir, final String key, final Path out) throws IOException {
    final Path entry = cacheEntry(cacheDir, key);
    if (!Files.exists(entry)) {
      return false;
    }
    Files.createDirectories(out.getParent());
    try (InputStream in = new GZIPInputStream(Files.newInputStream(entry));
      OutputStream os = Files.newOutputStream(out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      in.transferTo(os);
    }
    return true;
  }

  void store(final Path cacheDir, final String key, final Path out) throws IOException {
    if (!Files.exists(out)) {
      return;
    }
    final Path entry = cacheEntry(cacheDir, key);
    Files.createDirectories(entry.getParent());
    final Path tmp = Files.createTempFile(entry.getParent(), key.substring(0, Math.min(8, key.length())), ".tmp");
    try (
      InputStream in = Files.newInputStream(out);
      OutputStream os = new GZIPOutputStream(Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING))
    ) {
      in.transferTo(os);
    }
    Files.move(tmp, entry, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  private Path cacheEntry(final Path cacheDir, final String key) {
    return cacheDir.resolve(key.substring(0, 2)).resolve(key + SUFFIX);
  }
}

