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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.util.function.Function;

import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;

class ClassTransform implements Function<byte[], byte[]> {
    private RSymbols rSymbols;

    ClassTransform(RSymbols rSymbols) {
        this.rSymbols = rSymbols;
    }

    @Override
    public byte[] apply(byte[] origin) {
        ClassReader reader = new ClassReader(origin);
        PredicateClassVisitor precondition = new PredicateClassVisitor();
        reader.accept(precondition, SKIP_DEBUG | SKIP_FRAMES);
        if (!precondition.isAttemptToVisitR()) {
            return origin;
        }
        // don't pass reader to the writer.
        // or it will copy 'CONSTANT POOL' that contains no used entries to lead proguard running failed!
        ClassWriter writer = new ClassWriter(0);
        ClassVisitor visitor = new ShrinkRClassVisitor(writer, rSymbols);
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }
}