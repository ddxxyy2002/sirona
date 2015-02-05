/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sirona.javaagent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static java.lang.Integer.MIN_VALUE;

public class SironaClassVisitor extends ClassVisitor implements Opcodes {
    private static final String STATIC_INIT = "<clinit>";
    private static final String CONSTRUCTOR = "<init>";

    private static final Type AGENT_CONTEXT = Type.getType(AgentContext.class);

    private static final Type STRING_TYPE = Type.getType(String.class);
    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final Type ARRAY_TYPE = Type.getType( Object[].class );
    private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);
    private static final Type[] STOP_WITH_OBJECT_ARGS_TYPES = new Type[]{OBJECT_TYPE};
    private static final Type[] STOP_WITH_THROWABLE_ARGS_TYPES = new Type[]{THROWABLE_TYPE};
    private static final Type[] START_ARGS_TYPES = new Type[]{OBJECT_TYPE, STRING_TYPE,ARRAY_TYPE};

    // methods
    public static final Method START_METHOD = new Method("startOn", AGENT_CONTEXT, START_ARGS_TYPES);
    private static final Method STOP_METHOD = new Method("stop", Type.VOID_TYPE, STOP_WITH_OBJECT_ARGS_TYPES);
    private static final Method STOP_WITH_EXCEPTION_METHOD = new Method("stopWithException", Type.VOID_TYPE, STOP_WITH_THROWABLE_ARGS_TYPES);

    private final String javaName;
    private final byte[] classfileBuffer;
    private int count = 0;

    /**
     *
     * @param writer
     * @param javaName
     * @param buffer original class byte
     */
    public SironaClassVisitor(final ClassWriter writer, final String javaName, final byte[] buffer) {
        super(ASM5, writer);
        this.javaName = javaName;
        this.classfileBuffer = buffer;
    }

    @Override
    public void visitSource(final String source, final String debug) {
        super.visitSource(source, debug);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        // final MethodVisitor visitor = new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
        final MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);
        if (!isSironable(access, name)) {
            return visitor;
        }

        final String label = javaName.replace("/", ".") + "." + name + "(" + typesToString(Type.getArgumentTypes(desc)) + ")";
        if (AgentContext.listeners(label, classfileBuffer) != null) {
            count++;
            return new SironaAdviceAdapter(visitor, access, name, desc, label);
        }
        return visitor;
    }

    private String typesToString(final Type[] argumentTypes) {
        final StringBuilder b = new StringBuilder();
        for (final Type t : argumentTypes) {
            b.append(t.getClassName()).append(",");
        }
        if (b.length() > 0) {
            b.setLength(b.length() - 1);
        }
        return b.toString();
    }

    private static boolean isSironable(final int access, final String name) {
        return !name.equals(STATIC_INIT) && !name.equals(CONSTRUCTOR)
                && !Modifier.isAbstract(access) && !Modifier.isNative(access);
    }

    public boolean wasAdviced() {
        return count > 0;
    }

    private class SironaAdviceAdapter extends AdviceAdapter {
        private final boolean isStatic;
        private final String label;
        private final String desc;
        private final int access;

        public SironaAdviceAdapter(final MethodVisitor visitor, final int access, final String name, final String desc, final String label) {
            super(ASM5, visitor, access, name, desc);
            this.isStatic = Modifier.isStatic(access);
            this.label = label;
            this.desc = desc;
            this.access = access;
        }

        private int ctxLocal;
        private final Label tryStart = new Label();
        private final Label endLabel = new Label();



        @Override
        public void onMethodEnter() {

            // we need to call static method onStart from AgentContext
            // with parameters final String key, final Object[] methodParameters,final Object that

            if ( isStatic )
            {
                visitInsn( ACONST_NULL );
            }
            else
            {
                loadThis();
            }

            push( label );

            int length = Type.getArgumentTypes( desc ).length;

            // push count of arguments to the stack
            super.visitIntInsn( BIPUSH, length );
            // creates an object array
            super.visitTypeInsn( ANEWARRAY, "java/lang/Object" );

            // stores the arguments in the array
            for ( int i = 0; i < length; i++ )
            {
                Type tp = Type.getArgumentTypes( desc )[i];

                // duplicates the reference to the array. AASTORE consumes the stack element with the reference to the array.
                super.visitInsn( DUP );
                super.visitIntInsn( BIPUSH, i );
                // puts the value of the current argument on the stack
                // arguments can be primitive so we must box up to the corresponding Object
                if ( tp.equals( Type.BOOLEAN_TYPE ) )
                {
                    super.visitVarInsn( Opcodes.ILOAD, i + ( isStatic ? 0 : 1 ) );
                    super.visitMethodInsn( Opcodes.INVOKESTATIC, //
                                           "java/lang/Boolean", //
                                           "valueOf", //
                                           "(Z)Ljava/lang/Boolean;", //
                                           false );
                }
                else if ( tp.equals( Type.BYTE_TYPE ) )
                {
                    super.visitVarInsn( Opcodes.ILOAD, i + ( isStatic ? 0 : 1 ) );
                    super.visitMethodInsn( Opcodes.INVOKESTATIC, //
                                           "java/lang/Byte", //
                                           "valueOf", //
                                           "(B)Ljava/lang/Byte;", //
                                           false );
                }
                else if ( tp.equals( Type.CHAR_TYPE ) )
                {
                    super.visitVarInsn( Opcodes.ILOAD, i + ( isStatic ? 0 : 1 ) );
                    super.visitMethodInsn( Opcodes.INVOKESTATIC, //
                                           "java/lang/Character", //
                                           "valueOf", //
                                           "(C)Ljava/lang/Character;", //
                                           false );
                }
                else if ( tp.equals( Type.SHORT_TYPE ) )
                {
                    super.visitVarInsn( Opcodes.ILOAD, i + ( isStatic ? 0 : 1 ) );
                    super.visitMethodInsn( Opcodes.INVOKESTATIC, //
                                           "java/lang/Short", //
                                           "valueOf", //
                                           "(S)Ljava/lang/Short;", //
                                           false );
                }
                else if ( tp.equals( Type.INT_TYPE ) )
                {
                    super.visitVarInsn( Opcodes.ILOAD, i + ( isStatic ? 0 : 1 ) );
                    super.visitMethodInsn( Opcodes.INVOKESTATIC, //
                                           "java/lang/Integer", //
                                           "valueOf", //
                                           "(I)Ljava/lang/Integer;", //
                                           false );
                }
                else if ( tp.equals( Type.LONG_TYPE ) )
                {
                    super.visitVarInsn( Opcodes.LLOAD, i + ( isStatic ? 0 : 1 ) );
                    super.visitMethodInsn( Opcodes.INVOKESTATIC, //
                                           "java/lang/Long", //
                                           "valueOf", //
                                           "(J)Ljava/lang/Long;", //
                                           false );
                }
                else if ( tp.equals( Type.FLOAT_TYPE ) )
                {
                    super.visitVarInsn( Opcodes.FLOAD, i + ( isStatic ? 0 : 1 ) );
                    super.visitMethodInsn( Opcodes.INVOKESTATIC, //
                                           "java/lang/Float", //
                                           "valueOf", //
                                           "(F)Ljava/lang/Float;", //
                                           false );
                }
                else if ( tp.equals( Type.DOUBLE_TYPE ) )
                {
                    super.visitVarInsn( Opcodes.DLOAD, i + ( isStatic ? 0 : 1 ) );
                    super.visitMethodInsn( Opcodes.INVOKESTATIC, //
                                           "java/lang/Double", //
                                           "valueOf", //
                                           "(D)Ljava/lang/Double;", //
                                           false );
                }
                else
                {
                    super.visitVarInsn( Opcodes.ALOAD, i + ( isStatic ? 0 : 1 ) );
                }

                // stores the value of the current argument in the array
                super.visitInsn( AASTORE );
            }

            ctxLocal = newLocal( AGENT_CONTEXT );

            invokeStatic( AGENT_CONTEXT, START_METHOD );

            storeLocal( ctxLocal );

            visitLabel( tryStart );
        }


        @Override
        public void onMethodExit(final int opCode) {
            if (opCode == ATHROW) {
                return;
            }

            int stateLocal = -1;
            if (opCode != MIN_VALUE) {
                final Type returnType = Type.getReturnType(desc);
                final boolean isVoid = Type.VOID_TYPE.equals(returnType);
                if (!isVoid) {
                    stateLocal = newLocal(returnType);
                    storeLocal(stateLocal);
                }
            } else {
                stateLocal = newLocal(THROWABLE_TYPE);
                storeLocal(stateLocal);
            }

            loadLocal(ctxLocal);
            if (stateLocal != -1) {
                loadLocal(stateLocal);
                if (opCode != MIN_VALUE) {
                    valueOf(Type.getReturnType(desc));
                }
            } else {
                visitInsn(ACONST_NULL);
            }
            if (opCode != MIN_VALUE) {
                invokeVirtual(AGENT_CONTEXT, STOP_METHOD);
            } else {
                invokeVirtual(AGENT_CONTEXT, STOP_WITH_EXCEPTION_METHOD);
            }

            if (stateLocal != -1) {
                loadLocal(stateLocal);
            }
        }

        @Override
        public void visitMaxs(final int maxStack, final int maxLocals) {
            visitLabel(endLabel);
            catchException(tryStart, endLabel, THROWABLE_TYPE);
            onMethodExit(MIN_VALUE);
            throwException();
            super.visitMaxs(0, 0);
        }
    }
}
