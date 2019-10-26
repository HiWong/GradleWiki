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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

abstract class ClassesProcessor implements Processor {
    protected final Function<byte[], byte[]> classTransform;
    protected final Path src;
    protected final Path dst;

    /**
     * @param src Source path, can be resolved as a directory or a jar file
     * @param dst Destination path
     */
    ClassesProcessor(Function<byte[], byte[]> classTransform,
                     Path src, Path dst) {
        this.classTransform = classTransform;
        this.src = src;
        this.dst = dst;
        if (Files.notExists(src)) {
            throw new IllegalArgumentException("No such file " + src);
        }
    }
}
