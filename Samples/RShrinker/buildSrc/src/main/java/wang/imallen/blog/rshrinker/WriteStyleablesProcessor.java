/*
 * Copyright (c) 2018 Yrom Wang
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

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;

final class WriteStyleablesProcessor implements Processor {
    private RSymbols symbols;
    private File dir;

    WriteStyleablesProcessor(RSymbols symbols, File dir) {
        this.symbols = symbols;
        this.dir = dir;
    }

    @Override
    public void proceed() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_SUPER,
                RSymbols.R_STYLEABLES_CLASS_NAME,
                null,
                "java/lang/Object",
                null);
        for (String name : symbols.getStyleables().keySet()) {
            writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, name, "[I", null, null);
        }

        writeClinit(writer);
        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        try {
            if (!dir.isDirectory() && !dir.mkdirs()) {
                throw new RuntimeException("Cannot mkdir " + dir);
            }
            Files.write(dir.toPath().resolve(RSymbols.R_STYLEABLES_CLASS_NAME + ".class"), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeClinit(ClassWriter writer) {
        Map<String, int[]> styleables = symbols.getStyleables();
        MethodVisitor clinit = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.visitCode();

        for (Map.Entry<String, int[]> entry : styleables.entrySet()) {
            final String field = entry.getKey();
            final int[] value = entry.getValue();
            final int length = value.length;
            pushInt(clinit, length);
            clinit.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);
            for (int i = 0; i < length; i++) {
                clinit.visitInsn(Opcodes.DUP);                  // dup
                pushInt(clinit, i);
                pushInt(clinit, value[i]);
                clinit.visitInsn(Opcodes.IASTORE);              // iastore
            }
            clinit.visitFieldInsn(Opcodes.PUTSTATIC, RSymbols.R_STYLEABLES_CLASS_NAME, field, "[I");
        }
        clinit.visitInsn(Opcodes.RETURN);
        clinit.visitMaxs(0, 0); // auto compute
        clinit.visitEnd();
    }

    private static void pushInt(MethodVisitor mv, int i) {
        if (0 <= i && i <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + i); //  ICONST_0 ~ ICONST_5
        } else if (i <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, i);
        } else if (i <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, i);
        } else {
            mv.visitLdcInsn(i);
        }
    }
}
