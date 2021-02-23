package com.alienhe.art.vproxy.dex;

import com.alienhe.art.vproxy.instruction.DexInstruction;

import java.util.ArrayList;
import java.util.List;

/**
 * @author alienhe
 */
public class DexCode {

    /**
     * 本段代码使用到的寄存器数目
     */
    public final int registersSize;

    /**
     * method传入参数的数量
     */
    public final int insSize;

    /**
     * 本段代码调用其他方法时需要的参数个数
     */
    public final int outsSize;

    public final List<DexInstruction> instructions;

    private DexCode(final Builder builder) {
        registersSize = builder.registersSize;
        insSize = builder.insSize;
        outsSize = builder.outsSize;
        instructions = builder.instructions;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private int registersSize;
        private int insSize;
        private int outsSize;
        private final List<DexInstruction> instructions = new ArrayList<>();

        private Builder() {
        }

        public Builder registersSize(final int registersSize) {
            this.registersSize = registersSize;
            return this;
        }

        public Builder insSize(final int insSize) {
            this.insSize = insSize;
            return this;
        }

        public Builder outsSize(final int outsSize) {
            this.outsSize = outsSize;
            return this;
        }

        public Builder instruction(final DexInstruction instruction) {
            this.instructions.add(instruction);
            return this;
        }

        public DexCode build() {
            return new DexCode(this);
        }
    }
}
