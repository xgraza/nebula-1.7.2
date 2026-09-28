package ez.nebula.client.forge.patch;

import com.google.common.collect.Lists;
import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.*;

import java.util.List;

@SuppressWarnings("unused")
public final class ASMParserPatcher implements IClassTransformer
{
    private static final Logger LOGGER = LogManager.getLogger("Nebula ASM Patcher");
    private static final List<String> PATCH_CLASSES = Lists.newArrayList(
            "cpw.mods.fml.common.discovery.asm.ModClassVisitor",
            "cpw.mods.fml.common.discovery.asm.ModMethodVisitor",
            "cpw.mods.fml.common.discovery.asm.ModAnnotationVisitor",
            "cpw.mods.fml.common.discovery.asm.ModFieldVisitor");

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass)
    {
        if (PATCH_CLASSES.contains(transformedName))
        {
            LOGGER.info("Attempting to patch {}", transformedName);

            final ClassReader cr = new ClassReader(basicClass);
            final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            cr.accept(new ClassVisitor(Opcodes.ASM5, cw)
            {
                @Override
                public MethodVisitor visitMethod(int access, String mName, String desc, String signature, String[] exceptions) {
                    final MethodVisitor mv = super.visitMethod(access, mName, desc, signature, exceptions);
                    if (!mName.equals("<init>"))
                    {
                        return mv;
                    }

                    return new MethodVisitor(Opcodes.ASM5, mv)
                    {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)
                        {
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }

                        @Override
                        public void visitInsn(int opcode)
                        {
                            super.visitInsn(opcode);
                        }

                        @Override
                        public void visitIntInsn(int opcode, int operand)
                        {
                            super.visitIntInsn(opcode, operand);
                        }

                        @Override
                        public void visitLdcInsn(Object cst) {
                            if (cst instanceof Integer && ((Integer) cst) == Opcodes.ASM4)
                            {
                                LOGGER.info("Patched ASM4 to ASM5!");
                                super.visitLdcInsn(Opcodes.ASM5);
                                return;
                            }
                            super.visitLdcInsn(cst);
                        }
                    };
                }
            }, 0);
            LOGGER.info("Rewriting class bytes...");
            return cw.toByteArray();
        }
        return basicClass;
    }
}
