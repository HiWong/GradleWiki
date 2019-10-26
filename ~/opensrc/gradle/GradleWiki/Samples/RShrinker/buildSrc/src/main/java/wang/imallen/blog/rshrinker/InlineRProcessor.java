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

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.TransformInput;

import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


final class InlineRProcessor implements Processor {
    private Collection<TransformInput> inputs;
    private Function<QualifiedContent, Path> getTargetPath;
    private Function<byte[], byte[]> transform;

    InlineRProcessor(Collection<TransformInput> inputs,
                     Function<byte[], byte[]> transform,
                     Function<QualifiedContent, Path> getTargetPath) {
        this.inputs = inputs;
        this.getTargetPath = getTargetPath;
        this.transform = transform;
    }

    @Override
    public void proceed() {
        Stream.concat(
                streamOf(inputs, TransformInput::getDirectoryInputs).map(input -> {
                    Path src = input.getFile().toPath();
                    Path dst = getTargetPath.apply(input);
                    return new DirProcessor(transform, src, dst);
                }),
                streamOf(inputs, TransformInput::getJarInputs).map(input -> {
                    Path src = input.getFile().toPath();
                    Path dst = getTargetPath.apply(input);
                    return new JarProcessor(transform, src, dst);
                })
        ).forEach(Processor::proceed);
    }

    private static <T extends QualifiedContent> Stream<T> streamOf(
            Collection<TransformInput> inputs,
            Function<TransformInput, Collection<T>> mapping) {
        Collection<T> list = inputs.stream()
                .map(mapping)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        if (list.size() >= Runtime.getRuntime().availableProcessors())
            return list.parallelStream();
        else
            return list.stream();
    }

}
