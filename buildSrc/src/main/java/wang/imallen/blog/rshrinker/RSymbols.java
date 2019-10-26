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

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.TransformInput;
import com.google.common.collect.Maps;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;

class RSymbols {
    /**
     * default package!
     */
    static final String R_STYLEABLES_CLASS_NAME = "R$styleable";

    private Map<String, Integer> symbols = Collections.emptyMap();
    private Map<String, int[]> styleables = Maps.newHashMap();

    public Integer get(String key) {
        return symbols.get(key);
    }

    public boolean containsKey(String key) {
        return symbols.containsKey(key);
    }

    public boolean isEmpty() {
        return symbols.isEmpty() && styleables.isEmpty();
    }

    public Map<String, int[]> getStyleables() {
        return Collections.unmodifiableMap(styleables);
    }

    public RSymbols from(Collection<TransformInput> inputs) {
        final PathMatcher rClassMatcher = FileSystems.getDefault().getPathMatcher("glob:R$*.class");
        final List<Path> paths = inputs.stream()
                .map(TransformInput::getDirectoryInputs)
                .flatMap(Collection::stream)
                .map(this::toStream)
                .reduce(Stream.empty(), Stream::concat)
                .collect(Collectors.toList());
        Stream<Path> stream;
        if (paths.size() >= Runtime.getRuntime().availableProcessors() * 3) {
            // use parallel here!
            stream = paths.parallelStream();
            symbols = Maps.newConcurrentMap();
        } else {
            stream = paths.stream();
            symbols = Maps.newHashMap();
        }
        stream.filter(path -> rClassMatcher.matches(path.getFileName()))
                .forEach(this::drainSymbols);
        return this;
    }

    private void drainSymbols(Path file) {

        final String filename = file.getFileName().toString();
        String typeName = filename.substring(0, filename.length() - ".class".length());
        //typeName是类似R$bool, R$id这样的

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5) {
            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                // read constant value
                if (value instanceof Integer) {
                    String key = typeName + '.' + name;
                    Integer old = symbols.get(key);
                    if (old != null && !old.equals(value)) {
                        throw new IllegalStateException("Value of " + key + " mismatched! "
                                + "Excepted 0x" + Integer.toHexString(old)
                                + " but was 0x" + Integer.toHexString((Integer) value));
                    } else {
                        symbols.put(key, (Integer) value);
                    }
                }
                return null; //TODO 这里返回null是为了将R文件去掉吗？
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (access == Opcodes.ACC_STATIC && "<clinit>".equals(name)) {

                    return new MethodVisitor(Opcodes.ASM5) {
                        int[] current = null;
                        LinkedList<Integer> intStack = new LinkedList<>();

                        @Override
                        public void visitIntInsn(int opcode, int operand) {
                            if (opcode == Opcodes.NEWARRAY && operand == Opcodes.T_INT) {
                                current = new int[intStack.pop()];
                            } else if (opcode == Opcodes.BIPUSH) {
                                intStack.push(operand);
                            }
                        }

                        @Override
                        public void visitLdcInsn(Object cst) {
                            if (cst instanceof Integer) {
                                intStack.push((Integer) cst);
                            }
                        }

                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode >= Opcodes.ICONST_0 && opcode <= Opcodes.ICONST_5) {
                                intStack.push(opcode - Opcodes.ICONST_0);
                            } else if (opcode == Opcodes.IASTORE) {
                                int value = intStack.pop();
                                int index = intStack.pop();
                                current[index] = value;
                            }
                        }

                        @Override
                        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                            if (opcode == Opcodes.PUTSTATIC) {
                                int[] old = styleables.get(name);
                                if (old != null && old.length != current.length && !Arrays.equals(old, current)) {
                                    throw new IllegalStateException("Value of styleable." + name + " mismatched! "
                                            + "Excepted " + Arrays.toString(old)
                                            + " but was " + Arrays.toString(current));
                                } else {
                                    styleables.put(name, current);
                                }
                                current = null;
                                intStack.clear();
                            }
                        }
                    };
                }
                return null; //TODO 这里返回null是为了将R文件去掉吗？
            }
        };

        new ClassReader(bytes).accept(visitor, SKIP_DEBUG | SKIP_FRAMES);
    }

    private Stream<Path> toStream(DirectoryInput dir) {
        try {
            return Files.walk(dir.getFile().toPath()).filter(Files::isRegularFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
