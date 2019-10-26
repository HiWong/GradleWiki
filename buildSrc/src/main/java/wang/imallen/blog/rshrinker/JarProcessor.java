/*
 * Copyright (c) 2017 Yrom Wang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wang.imallen.blog.rshrinker;

import com.google.common.collect.ImmutableList;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


class JarProcessor extends ClassesProcessor {

    JarProcessor(Function<byte[], byte[]> classTransform, Path src, Path dst) {
        super(classTransform, src, dst);
    }

    @Override
    public void proceed() {
        try {
            List<Pair<String, byte[]>> entryList = readZipEntries(src)
                    .parallelStream()
                    .map(this::transformClassBlob)
                    .collect(Collectors.toList());
            if (entryList.isEmpty()) return;
            try (OutputStream fileOut = Files.newOutputStream(dst)) {
                ByteArrayOutputStream buffer = zipEntries(entryList);
                buffer.writeTo(fileOut);
            }
        } catch (IOException e) {
            throw new RuntimeException("Reading jar entries failure", e);
        }
    }

    private ByteArrayOutputStream zipEntries(List<Pair<String, byte[]>> entryList) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(8192);
        try (ZipOutputStream jar = new ZipOutputStream(buffer)) {
            jar.setMethod(ZipOutputStream.STORED);
            final CRC32 crc = new CRC32();
            for (Pair<String, byte[]> entry : entryList) {
                byte[] bytes = entry.second;
                final ZipEntry newEntry = new ZipEntry(entry.first);
                newEntry.setMethod(ZipEntry.STORED); // chose STORED method
                crc.reset();
                crc.update(entry.second);
                newEntry.setCrc(crc.getValue());
                newEntry.setSize(bytes.length);
                writeEntryToJar(newEntry, bytes, jar);
            }
            jar.flush();
        }
        return buffer;
    }

    private Pair<String, byte[]> transformClassBlob(Pair<String, byte[]> entry) {
        byte[] bytes = entry.second;
        entry.second = classTransform.apply(bytes);
        return entry;
    }

    private List<Pair<String, byte[]>> readZipEntries(Path src) throws IOException {
        ImmutableList.Builder<Pair<String, byte[]>> list = ImmutableList.builder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(Files.readAllBytes(src)))) {
            for (ZipEntry entry = zip.getNextEntry();
                 entry != null;
                 entry = zip.getNextEntry()) {
                String name = entry.getName();
                if (!name.endsWith(".class")) {
                    // skip
                    continue;
                }
                long entrySize = entry.getSize();
                if (entrySize >= Integer.MAX_VALUE) {
                    throw new OutOfMemoryError("Too large class file " + name + ", size is " + entrySize);
                }
                byte[] bytes = readByteArray(zip, (int) entrySize);
                list.add(Pair.of(name, bytes));
            }
        }
        return list.build();
    }

    private byte[] readByteArray(ZipInputStream zip, int expected) throws IOException {
        if (expected == -1) { // unknown size
            return IOUtils.toByteArray(zip);
        }
        final byte[] bytes = new byte[expected];
        int read = 0;
        do {
            int n = zip.read(bytes, read, expected - read);
            if (n <= 0) {
                break;
            }
            read += n;
        } while (read < expected);

        if (expected != bytes.length) {
            throw new EOFException("unexpected EOF");
        }
        return bytes;
    }


    private static void writeEntryToJar(ZipEntry entry, byte[] bytes, ZipOutputStream jar) {
        try {
            jar.putNextEntry(entry);
            jar.write(bytes);
            jar.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // mutable pair
    static class Pair<F, S> {
        F first;
        S second;

        Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        static <F, S> Pair<F, S> of(F first, S second) {
            return new Pair<>(first, second);
        }
    }
}
