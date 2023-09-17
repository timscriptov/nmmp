package com.nmmedit.apkprotect.dex2c.converter.instructionrewriter;

import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.ReferenceType;
import com.android.tools.smali.dexlib2.iface.ExceptionHandler;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.TryBlock;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.instruction.SwitchElement;
import com.android.tools.smali.dexlib2.iface.instruction.formats.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.writer.DexDataWriter;
import com.android.tools.smali.util.ExceptionWithContext;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.nmmedit.apkprotect.dex2c.converter.ClassAnalyzer;
import com.nmmedit.apkprotect.dex2c.converter.References;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 继承它,实现replaceOpcode(int opcode)方法, 进行opcode替换
 * 保留替换信息,通过这些信息生成c语言的opcode头文件,以保证虚拟机正常运行
 */
public abstract class InstructionRewriter {


    final Opcodes opcodes;
    private final Comparator<SwitchElement> switchElementComparator = new Comparator<SwitchElement>() {
        @Override
        public int compare(@NotNull SwitchElement element1, @NotNull SwitchElement element2) {
            return Ints.compare(element1.getKey(), element2.getKey());
        }
    };
    // 指令重写需要的引用信息
    private References references;
    private ClassAnalyzer classAnalyzer;

    public InstructionRewriter(@NotNull Opcodes opcodes) {
        this.opcodes = opcodes;
    }

    private static int packNibbles(int a, int b) {
        return (b << 4) | a;
    }

    public void loadReferences(
            @NotNull References references,
            @NotNull ClassAnalyzer classAnalyzer) {
        this.references = references;
        this.classAnalyzer = classAnalyzer;
    }

    /**
     * 对指令里的opcode进行替换
     *
     * @param opcode
     * @return
     */
    public abstract int replaceOpcode(Opcode opcode);

    //得到处理后的opcode列表
    @NotNull
    protected abstract List<Opcode> getOpcodeList();

    //opcode替换之后,需要修改c头文件之类
    public final void generateConfig(Writer opcodeWriter, Writer gotoTableWriter) throws IOException {
        final List<Opcode> opcodeList = getOpcodeList();
        for (int i = 0; i < opcodeList.size(); i++) {
            final Opcode opcode = opcodeList.get(i);
            if (opcode != null) {
                final String opName = opcode.name.replace('-', '_').replace('/', '_').toUpperCase();
                opcodeWriter.write(String.format("    OP_%s     = 0x%x,\n", opName, i));
                gotoTableWriter.write(String.format(
                        "    H(OP_%s),                                                            \\\\\n", opName));
            } else {
                final String opName = String.format("OP_UNUSED_%02x", i).toUpperCase();
                opcodeWriter.write(String.format("    %s     = 0x%02x,\n", opName, i));

                gotoTableWriter.write(String.format(
                        "    H(%s),                                                            \\\\\n", opName));

            }
        }
    }

    @Contract("null -> fail")
    public final byte @NotNull [] rewriteInstructions(MethodImplementation methodImp) {
        if (methodImp == null) {
            throw new RuntimeException("No methodImp");
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final DexDataWriter writer = new DexDataWriter(out, 0);
        for (Instruction instruction : methodImp.getInstructions()) {
            switch (instruction.getOpcode().format) {
                case Format10t -> write(writer, (Instruction10t) instruction);
                case Format10x -> write(writer, (Instruction10x) instruction);
                case Format11n -> write(writer, (Instruction11n) instruction);
                case Format11x -> write(writer, (Instruction11x) instruction);
                case Format12x -> write(writer, (Instruction12x) instruction);
                case Format20bc -> write(writer, (Instruction20bc) instruction);
                case Format20t -> write(writer, (Instruction20t) instruction);
                case Format21c -> write(writer, (Instruction21c) instruction);
                case Format21ih -> write(writer, (Instruction21ih) instruction);
                case Format21lh -> write(writer, (Instruction21lh) instruction);
                case Format21s -> write(writer, (Instruction21s) instruction);
                case Format21t -> write(writer, (Instruction21t) instruction);
                case Format22b -> write(writer, (Instruction22b) instruction);
                case Format22c -> write(writer, (Instruction22c) instruction);
                case Format22cs -> write(writer, (Instruction22cs) instruction);
                case Format22s -> write(writer, (Instruction22s) instruction);
                case Format22t -> write(writer, (Instruction22t) instruction);
                case Format22x -> write(writer, (Instruction22x) instruction);
                case Format23x -> write(writer, (Instruction23x) instruction);
                case Format30t -> write(writer, (Instruction30t) instruction);
                case Format31c -> write(writer, (Instruction31c) instruction);
                case Format31i -> write(writer, (Instruction31i) instruction);
                case Format31t -> write(writer, (Instruction31t) instruction);
                case Format32x -> write(writer, (Instruction32x) instruction);
                case Format35c -> write(writer, (Instruction35c) instruction);
                case Format35mi -> {
                }
                case Format35ms -> {
                }
                case Format3rc -> write(writer, (Instruction3rc) instruction);
                case Format3rmi -> {
                }
                case Format3rms -> {
                }
                case Format45cc -> {
                }
                case Format4rcc -> {
                }
                case Format51l -> write(writer, (Instruction51l) instruction);
                case ArrayPayload -> write(writer, (ArrayPayload) instruction);
                case PackedSwitchPayload -> write(writer, (PackedSwitchPayload) instruction);
                case SparseSwitchPayload -> write(writer, (SparseSwitchPayload) instruction);
                case UnresolvedOdexInstruction -> throw new RuntimeException("Don't support odex");
            }
        }
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return out.toByteArray();
    }

    /**
     * 复制出异常表数据,同时把异常数量写在头两个字节里
     * typedef struct {
     * u2 triesSize;
     * u2 unused;
     * followed by try_item[triesSize]
     * TryItem tryItems[1];
     * // followed by uleb128 handlersSize
     * // followed by catch_handler_item[handlersSize]
     * } TryCatchHandler;
     *
     * @param methodImp 方法实现
     * @return 异常表数据
     * @throws IOException
     */
    @Contract("null -> fail")
    public final byte @NotNull [] handleTries(MethodImplementation methodImp) throws IOException {
        if (methodImp == null) {
            throw new RuntimeException("No methodImp");
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final DexDataWriter writer = new DexDataWriter(out, 0);

        final List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = methodImp.getTryBlocks();
        if (!tryBlocks.isEmpty()) {
            writer.writeUshort(tryBlocks.size());
            writer.writeUshort(0);
            ByteArrayOutputStream ehBuf = new ByteArrayOutputStream();

            // filter out unique lists of exception handlers
            Map<List<? extends ExceptionHandler>, Integer> exceptionHandlerOffsetMap = Maps.newHashMap();
            for (TryBlock<? extends ExceptionHandler> tryBlock : tryBlocks) {
                exceptionHandlerOffsetMap.put(tryBlock.getExceptionHandlers(), 0);
            }
            DexDataWriter.writeUleb128(ehBuf, exceptionHandlerOffsetMap.size());

            for (TryBlock<? extends ExceptionHandler> tryBlock : tryBlocks) {
                int startAddress = tryBlock.getStartCodeAddress();
                int endAddress = startAddress + tryBlock.getCodeUnitCount();

                int tbCodeUnitCount = endAddress - startAddress;

                writer.writeInt(startAddress);
                writer.writeUshort(tbCodeUnitCount);

                if (tryBlock.getExceptionHandlers().isEmpty()) {
                    throw new ExceptionWithContext("No exception handlers for the try block!");
                }

                Integer offset = exceptionHandlerOffsetMap.get(tryBlock.getExceptionHandlers());
                if (offset != 0) {
                    // exception handler has already been written out, just use it
                    writer.writeUshort(offset);
                } else {
                    // if offset has not been set yet, we are about to write out a new exception handler
                    offset = ehBuf.size();
                    writer.writeUshort(offset);
                    exceptionHandlerOffsetMap.put(tryBlock.getExceptionHandlers(), offset);

                    // check if the last exception handler is a catch-all and adjust the size accordingly
                    int ehSize = tryBlock.getExceptionHandlers().size();
                    ExceptionHandler ehLast = tryBlock.getExceptionHandlers().get(ehSize - 1);
                    if (ehLast.getExceptionType() == null) {
                        ehSize = ehSize * (-1) + 1;
                    }

                    // now let's layout the exception handlers, assuming that catch-all is always last
                    DexDataWriter.writeSleb128(ehBuf, ehSize);
                    for (ExceptionHandler eh : tryBlock.getExceptionHandlers()) {
                        final TypeReference exceptionTypeReference = eh.getExceptionTypeReference();

                        int codeAddress = eh.getHandlerCodeAddress();

                        if (exceptionTypeReference != null) {
                            //regular exception handling
                            DexDataWriter.writeUleb128(ehBuf, getReferenceIndex(ReferenceType.TYPE, exceptionTypeReference));
                            DexDataWriter.writeUleb128(ehBuf, codeAddress);
                        } else {
                            //catch-all
                            DexDataWriter.writeUleb128(ehBuf, codeAddress);
                        }
                    }
                }
            }

            if (ehBuf.size() > 0) {
                ehBuf.writeTo(writer);
                ehBuf.reset();
            }
        }

        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    private short getOpcodeValue(Opcode opcode) {
        Short value = opcodes.getOpcodeValue(opcode);
        if (value == null) {
            throw new ExceptionWithContext("Instruction %s is invalid for api %d", opcode.name, opcodes.api);
        }
        if (value > 0xff) {
            return value;
        }
        return (short) replaceOpcode(opcode);
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction10t instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction10x instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(0);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction11n instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getNarrowLiteral()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction11x instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction12x instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction20bc instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getVerificationError());
            writer.writeUshort(getReferenceIndex(instruction));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction20t instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(0);
            writer.writeShort(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction21c instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeUshort(getReferenceIndex(instruction));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction21ih instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeShort(instruction.getHatLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction21lh instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeShort(instruction.getHatLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction21s instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeShort(instruction.getNarrowLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction21t instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeShort(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction22b instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.write(instruction.getRegisterB());
            writer.write(instruction.getNarrowLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction22c instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
            writer.writeUshort(getReferenceIndex(instruction));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction22cs instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
            writer.writeUshort(instruction.getFieldOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction22s instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
            writer.writeShort(instruction.getNarrowLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction22t instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
            writer.writeShort(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction22x instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeUshort(instruction.getRegisterB());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction23x instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.write(instruction.getRegisterB());
            writer.write(instruction.getRegisterC());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction30t instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(0);
            writer.writeInt(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction31c instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeInt(getReferenceIndex(instruction));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction31i instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeInt(instruction.getNarrowLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction31t instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeInt(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction32x instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(0);
            writer.writeUshort(instruction.getRegisterA());
            writer.writeUshort(instruction.getRegisterB());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction35c instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterG(), instruction.getRegisterCount()));
            writer.writeUshort(getReferenceIndex(instruction));
            writer.write(packNibbles(instruction.getRegisterC(), instruction.getRegisterD()));
            writer.write(packNibbles(instruction.getRegisterE(), instruction.getRegisterF()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction3rc instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterCount());
            writer.writeUshort(getReferenceIndex(instruction));
            writer.writeUshort(instruction.getStartRegister());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull Instruction51l instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeLong(instruction.getWideLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull ArrayPayload instruction) {
        try {
            writer.writeUshort(getOpcodeValue(instruction.getOpcode()));
            writer.writeUshort(instruction.getElementWidth());
            List<Number> elements = instruction.getArrayElements();
            writer.writeInt(elements.size());
            switch (instruction.getElementWidth()) {
                case 1 -> {
                    for (Number element : elements) {
                        writer.write(element.byteValue());
                    }
                }
                case 2 -> {
                    for (Number element : elements) {
                        writer.writeShort(element.shortValue());
                    }
                }
                case 4 -> {
                    for (Number element : elements) {
                        writer.writeInt(element.intValue());
                    }
                }
                case 8 -> {
                    for (Number element : elements) {
                        writer.writeLong(element.longValue());
                    }
                }
            }
            if ((writer.getPosition() & 1) != 0) {
                writer.write(0);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull SparseSwitchPayload instruction) {
        try {
            writer.writeUbyte(0);
            writer.writeUbyte(getOpcodeValue(instruction.getOpcode()) >> 8);
            List<? extends SwitchElement> elements = Ordering.from(switchElementComparator).immutableSortedCopy(
                    instruction.getSwitchElements());
            writer.writeUshort(elements.size());
            for (SwitchElement element : elements) {
                writer.writeInt(element.getKey());
            }
            for (SwitchElement element : elements) {
                writer.writeInt(element.getOffset());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NotNull DexDataWriter writer,
                      @NotNull PackedSwitchPayload instruction) {
        try {
            writer.writeUbyte(0);
            writer.writeUbyte(getOpcodeValue(instruction.getOpcode()) >> 8);
            List<? extends SwitchElement> elements = instruction.getSwitchElements();
            writer.writeUshort(elements.size());
            if (elements.isEmpty()) {
                writer.writeInt(0);
            } else {
                writer.writeInt(elements.get(0).getKey());
                for (SwitchElement element : elements) {
                    writer.writeInt(element.getOffset());
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private int getReferenceIndex(@NotNull ReferenceInstruction referenceInstruction) {
        switch (referenceInstruction.getOpcode()) {
            case SGET, SGET_BOOLEAN, SGET_BYTE, SGET_CHAR, SGET_SHORT, SGET_WIDE, SGET_OBJECT -> {
                //修复接口中静态域访问问题
                final FieldReference reference = (FieldReference) referenceInstruction.getReference();
                final FieldReference newFieldRef = classAnalyzer.getDirectFieldRef(reference);
                if (newFieldRef != null) {
                    return getReferenceIndex(referenceInstruction.getReferenceType(), newFieldRef);
                }
            }
            case CONST_STRING, CONST_STRING_JUMBO -> {
                //重写const-string idx
                return references.getConstStringItemIndex(((StringReference) referenceInstruction.getReference()).getString());
            }
        }
        return getReferenceIndex(referenceInstruction.getReferenceType(),
                referenceInstruction.getReference());
    }

    private int getReferenceIndex(int referenceType, Reference reference) {
        return switch (referenceType) {
            case ReferenceType.FIELD -> references.getFieldItemIndex((FieldReference) reference);
            case ReferenceType.METHOD -> references.getMethodItemIndex((MethodReference) reference);
            case ReferenceType.STRING -> references.getStringItemIndex(((StringReference) reference).getString());
            case ReferenceType.TYPE -> references.getTypeItemIndex(((TypeReference) reference).getType());
            default -> throw new ExceptionWithContext("Unknown reference type: %d", referenceType);
        };
    }
}
