package com.nmmedit.apkprotect.dex2c.converter.structs;

import com.android.tools.smali.dexlib2.AccessFlags;
import com.android.tools.smali.dexlib2.HiddenApiRestriction;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.base.reference.BaseMethodReference;
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x;
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c;
import com.android.tools.smali.dexlib2.iface.Annotation;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.MethodParameter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

//默认无参数构造方法

/**
 * .method public constructor <init>()V
 * .locals 0
 * invoke-direct {p0}, Landroid/app/Application;-><init>()V
 * return-void
 * .end method
 */

public class EmptyConstructorMethod extends BaseMethodReference implements Method {

    @NotNull
    private final String definingClass;

    @NotNull
    private final String superClass;

    public EmptyConstructorMethod(@NotNull String definingClass, @NotNull String superClass) {
        this.definingClass = definingClass;
        this.superClass = superClass;
    }

    @NotNull
    @Override
    public List<? extends MethodParameter> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public int getAccessFlags() {
        return AccessFlags.CONSTRUCTOR.getValue()
                | AccessFlags.PUBLIC.getValue();
    }

    @NotNull
    @Override
    public Set<? extends Annotation> getAnnotations() {
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public Set<HiddenApiRestriction> getHiddenApiRestrictions() {
        return Collections.emptySet();
    }

    @Override
    public MethodImplementation getImplementation() {
        final MutableMethodImplementation implementation = new MutableMethodImplementation(1);
        implementation.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_DIRECT, 1,
                0, 0, 0, 0, 0, new BaseMethodReference() {
            @NotNull
            @Override
            public String getDefiningClass() {
                return superClass;
            }

            @NotNull
            @Override
            public String getName() {
                return "<init>";
            }

            @NotNull
            @Override
            public List<? extends CharSequence> getParameterTypes() {
                return Collections.emptyList();
            }

            @NotNull
            @Override
            public String getReturnType() {
                return "V";
            }
        }
        ));
        implementation.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
        return implementation;
    }

    @NotNull
    @Override
    public String getDefiningClass() {
        return definingClass;
    }

    @NotNull
    @Override
    public String getName() {
        return "<init>";
    }

    @NotNull
    @Override
    public List<? extends CharSequence> getParameterTypes() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public String getReturnType() {
        return "V";
    }
}