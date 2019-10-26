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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


class PredicateClassVisitor extends ClassVisitor {
    private boolean attemptToVisitR;

    PredicateClassVisitor() {
        super(Opcodes.ASM5);
    }

    boolean isAttemptToVisitR() {
        return attemptToVisitR;
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (!attemptToVisitR
                && access == 0x19 /*ACC_PUBLIC | ACC_STATIC | ACC_FINAL*/
                && ShrinkRClassVisitor.isRClass(name) && !ShrinkRClassVisitor.shouldSkip(name)) {
            attemptToVisitR = true;
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        if (attemptToVisitR) return null;
        return new MethodVisitor(Opcodes.ASM5, null) {

            @Override
            public void visitFieldInsn(int opcode, String owner, String fieldName,
                                       String fieldDesc) {

                if (attemptToVisitR
                        || opcode != Opcodes.GETSTATIC
                        || owner.startsWith("java/lang/")) {
                    return;
                }

                attemptToVisitR = ShrinkRClassVisitor.isRClass(owner) && !ShrinkRClassVisitor.shouldSkip(owner);
            }
        };
    }
}
