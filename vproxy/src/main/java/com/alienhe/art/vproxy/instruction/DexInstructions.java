package com.alienhe.art.vproxy.instruction;

import com.alienhe.art.vproxy.dex.DexField;
import com.alienhe.art.vproxy.dex.DexItem;
import com.alienhe.art.vproxy.dex.DexMethod;
import com.alienhe.art.vproxy.dex.DexType;

/**
 * 字节码格式：https://source.android.google.cn/devices/tech/dalvik/dalvik-bytecode?hl=zh-cn
 * 可执行指令格式：https://source.android.google.cn/devices/tech/dalvik/instruction-formats?hl=zh-cn
 *
 * @author alienhe
 */
public class DexInstructions {
    private DexInstructions() {
    }

    public static DexInstruction returnObject(int a) {
        return instruction11x(a, 0x11);
    }

    public static DexInstruction returnPrimitive(int a) {
        return instruction11x(a, 0x0f);
    }

    public static DexInstruction returnVoid() {
        return instruction10x(0x0e);
    }

    /**
     * @param a destination register (4 bits)
     * @param b signed int (4 bits)
     */
    public static DexInstruction const4(final int a, final int b) {
        return instruction11n(a, b, 0x12);
    }

    /**
     * @param a destination register (8 bits)
     * @param b signed int (16 bits)
     */
    public static DexInstruction const16(final int a, final int b) {
        return instruction21s(a, b, 0x13);
    }

    /**
     * @param a destination register (8 bits)
     */
    public static DexInstruction checkCast(final int a, final DexType dexType) {
        return instruction21c(a, 0x1f, dexType);
    }

    /**
     * @param a destination register (8 bits)
     */
    public static DexInstruction moveResultObject(final int a) {
        return instruction11x(a, 0x0c);
    }

    /**
     * @param a destination register (8 bits)
     */
    public static DexInstruction moveResult(final int a) {
        return instruction11x(a, 0x0a);
    }

    /**
     * @param a     value register or pair; may be source or dest (4 bits)
     * @param b     object register (4 bits)
     * @param field instance field reference index (16 bits)
     */
    public static DexInstruction igetObject(final int a, final int b, final DexField field) {
        return instruction22c(a, b, 0x54, field);
    }

    /**
     * @param a     value register or pair; may be source or dest (4 bits)
     * @param b     object register (4 bits)
     * @param field instance field reference index (16 bits)
     */
    public static DexInstruction iputObject(final int a, final int b, final DexField field) {
        return instruction22c(a, b, 0x5b, field);
    }

    /**
     * @param a value register or pair; may be source or dest (8 bits)
     * @param b array register (8 bits)
     * @param c index register (8 bits)
     */
    public static DexInstruction agetObject(final int a, final int b, final int c) {
        return instruction23x(a, b, c, 0x46);
    }

    /**
     * @param a value register or pair; may be source or dest (8 bits)
     * @param b array register (8 bits)
     * @param c index register (8 bits)
     */
    public static DexInstruction aputObject(final int a, final int b, final int c) {
        return instruction23x(a, b, c, 0x4d);
    }

    /**
     * @param a        destination register (4 bits)
     * @param b        size register
     * @param itemType type index
     */
    public static DexInstruction newArray(final int a, final int b, final DexType itemType) {
        return instruction22c(a, b, 0x23, itemType);
    }

    /**
     * @param c      argument register (4 bits)
     * @param method method reference index (16 bits)
     */
    public static DexInstruction invokeDirect(final int c, final DexMethod method) {
        return instruction35c(c, 0x70, method);
    }

    /**
     * @param c      argument register (4 bits)
     * @param method method reference index (16 bits)
     */
    public static DexInstruction invokeVirtual(final int c, final DexMethod method) {
        return instruction35c(c, 0x6e, method);
    }

    /**
     * @param c      argument register (4 bits)
     * @param d      argument register (4 bits)
     * @param e      argument register (4 bits)
     * @param f      argument register (4 bits)
     * @param method method reference index (16 bits)
     */
    public static DexInstruction invokeInterface(final int c, final int d, final int e, final int f, final DexMethod method) {
        return instruction35c(c, d, e, f, 0x72, method);
    }

    /**
     * @param c      argument register (4 bits)
     * @param method method reference index (16 bits)
     */
    public static DexInstruction invokeStatic(final int c, final DexMethod method) {
        return instruction35c(c, 0x71, method);
    }

    /**
     * @param c      argument register (4 bits)
     * @param d      argument register (4 bits)
     * @param method method reference index (16 bits)
     */
    public static DexInstruction invokeStatic(final int c, final int d, final DexMethod method) {
        return instruction35c(c, d, 0x71, method);
    }

    private static DexInstruction instruction10x(final int op) {
        return new DexInstruction() {
            @Override
            public int[] getByteCode() {
                // ØØ|op
                return new int[]{word(0x00, op)};
            }
        };
    }

    private static DexInstruction instruction11n(final int a, final int b, final int op) {
        return new DexInstruction() {
            @Override
            public int[] getByteCode() {
                // B|A|op
                return new int[]{word(b, a, op)};
            }
        };
    }

    private static DexInstruction instruction11x(final int a, final int op) {
        return new DexInstruction() {
            @Override
            public int[] getByteCode() {
                // AA|op
                return new int[]{word(a, op)};
            }
        };
    }

    private static DexInstruction instruction21c(final int a, final int op, final DexItem item) {
        return new DexInstruction() {
            @Override
            public int[] getByteCode() {
                // B|A|op CCCC
                return new int[]{word(a, op), item.index};
            }
        };
    }

    private static DexInstruction instruction21s(final int a, final int b, final int op) {
        return new DexInstruction() {
            @Override
            public int[] getByteCode() {
                // AA|op BBBB
                return new int[]{word(a, op), b};
            }
        };
    }

    private static DexInstruction instruction22c(final int a, final int b, final int op, final DexItem item) {
        return new DexInstruction() {
            @Override
            public int[] getByteCode() {
                // B|A|op CCCC
                return new int[]{word(b, a, op), item.index};
            }
        };
    }

    private static DexInstruction instruction23x(final int aa, final int bb, final int cc, final int op) {
        return new DexInstruction() {
            @Override
            public int[] getByteCode() {
                // AA|op CC|BB
                return new int[]{word(aa, op), word(cc, bb)};
            }
        };
    }

    private static DexInstruction instruction35c(final int c, final int op, final DexItem item) {
        return new DexInstruction() {
            @Override
            public int[] getByteCode() {
                // A|G|op BBBB F|E|D|C
                return new int[]{word(0x1, 0x0, op), item.index, word(0x0, 0x0, 0x0, c)};
            }
        };
    }

    private static DexInstruction instruction35c(final int c, final int d, final int op, final DexItem item) {
        return new DexInstruction() {
            @Override
            public int[] getByteCode() {
                // A|G|op BBBB F|E|D|C
                return new int[]{word(0x2, 0x0, op), item.index, word(0x0,0x0, d, c)};
            }
        };
    }

    private static DexInstruction instruction35c(final int c, final int d, final int e, final int f, final int op, final DexItem item) {
        return new DexInstruction() {
            @Override
            public int[] getByteCode() {
                // A|G|op BBBB F|E|D|C
                return new int[]{word(0x4, 0x0, op), item.index, word(f, e, d, c)};
            }
        };
    }


    private static int word(final int high, final int low) {
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }

    private static int word(final int highHighHalf, final int highLowHalf, final int low) {
        return DexInstructions.word(((highHighHalf & 0x0F) << 4) | (highLowHalf & 0x0F), low);
    }

    private static int word(final int highHighHalf, final int highLowHalf, final int lowHighHalf, final int lowLowHalf) {
        return DexInstructions.word(((highHighHalf & 0x0F) << 4) | (highLowHalf & 0x0F),
                ((lowHighHalf & 0x0F) << 4) | (lowLowHalf & 0x0F));
    }

}
