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

import wang.imallen.blog.rshrinker.log.Logger;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Function;

class DirProcessor extends ClassesProcessor {
    private static PathMatcher CASE_R_FILE =
            FileSystems.getDefault().getPathMatcher("regex:^R\\.class|R\\$[a-z]+\\.class$");

    private static DirectoryStream.Filter<Path> CLASS_TRANSFORM_FILTER =
            path -> Files.isDirectory(path)
                    || (Files.isRegularFile(path)
                    && !CASE_R_FILE.matches(path.getFileName()));

    DirProcessor(Function<byte[], byte[]> classTransform, Path src, Path dst) {
        super(classTransform, src, dst);
    }

    @Override
    public void proceed() {
        List<Path> files = resolveSources();
        if (files.size() >= Runtime.getRuntime().availableProcessors()) {
            files.parallelStream().forEach(this::proceedFile);
        } else {
            files.forEach(this::proceedFile);
        }
    }

    private void proceedFile(Path source) {
        String name = source.getFileName().toString();
        Path target = dst.resolve(name);
        if (Files.isDirectory(source)) {
            new DirProcessor(classTransform, source, target).proceed();
        } else if (Files.isRegularFile(source)) {
            Logger.d("transform file {}... ", source);
            try {
                if (Files.notExists(dst)) {
                    Files.createDirectories(dst);
                }
                if (source.getFileName().toString().endsWith(".class")) {
                    byte[] bytes = classTransform.apply(Files.readAllBytes(source));
                    Files.write(target, bytes);
                } else {
                    // copy non-class file!
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (Exception e) {
                Logger.d("error occurred on " + source, e);
                throw e;
            }
        }
    }

    private List<Path> resolveSources() {
        ImmutableList.Builder<Path> list = ImmutableList.builder();
        try (DirectoryStream<Path> dir = Files.newDirectoryStream(src, CLASS_TRANSFORM_FILTER)) {
            for (Path file : dir) {
                list.add(file);
            }
        } catch (DirectoryIteratorException e) {
            throw new UncheckedIOException(e.getCause());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return list.build();
    }
}
