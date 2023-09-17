package com.nmmedit.apkprotect.dex2c.converter.structs;

import com.android.tools.smali.dexlib2.AccessFlags;
import com.android.tools.smali.dexlib2.HiddenApiRestriction;
import com.android.tools.smali.dexlib2.base.reference.BaseMethodReference;
import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference;
import com.android.tools.smali.dexlib2.iface.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 每个类调用的静态初始化方法,一般情况一个classes.dex对应一个注册方法
 * 需要把它放在主classes.dex里
 * <p>
 * 静态初始化方法里增加加载本地库代码
 */
public class RegisterNativesUtilClassDef extends BaseTypeReference implements ClassDef {
    @NotNull
    private final String type;
    @NotNull
    private final List<String> nativeMethodNames;

    private final String libName;

    public RegisterNativesUtilClassDef(@NotNull String type,
                                       @NotNull List<String> nativeMethodNames,
                                       @NotNull String libName) {
        this.type = type;
        this.nativeMethodNames = nativeMethodNames;
        this.libName = libName;
    }

    @NotNull
    @Override
    public String getType() {
        return type;
    }


    @Override
    public int getAccessFlags() {
        return AccessFlags.PUBLIC.getValue();
    }

    @Nullable
    @Override
    public String getSuperclass() {
        return "Ljava/lang/Object;";
    }

    @NotNull
    @Override
    public List<String> getInterfaces() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public String getSourceFile() {
        return null;
    }

    @NotNull
    @Override
    public Set<? extends Annotation> getAnnotations() {
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public Iterable<? extends Field> getStaticFields() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Iterable<? extends Field> getInstanceFields() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Iterable<? extends Field> getFields() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Iterable<? extends Method> getDirectMethods() {
        final ArrayList<Method> methods = new ArrayList<>();
        //静态初始化方法
        methods.add(new LoadLibStaticBlockMethod(null, type, libName));
        //空构造函数
        methods.add(new EmptyConstructorMethod(type, "Ljava/lang/Object;"));
        for (String methodName : nativeMethodNames) {
            methods.add(new NativeMethod(type, methodName));
        }
        return methods;
    }

    @NotNull
    @Override
    public Iterable<? extends Method> getVirtualMethods() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Iterable<? extends Method> getMethods() {
        //virtualMethods为空,总方法只需要返回directMethods就行
        return getDirectMethods();
    }

    private static class NativeMethod extends BaseMethodReference implements Method {

        @NotNull
        private final String type;

        private final String methodName;

        public NativeMethod(@NotNull String type, String methodName) {
            this.type = type;
            this.methodName = methodName;
        }

        @Contract(pure = true)
        @NotNull
        @Override
        public @Unmodifiable List<? extends MethodParameter> getParameters() {
            return Collections.emptyList();
        }

        @Override
        public int getAccessFlags() {
            return AccessFlags.NATIVE.getValue()
                    | AccessFlags.STATIC.getValue()
                    | AccessFlags.PUBLIC.getValue();
        }

        @Contract(pure = true)
        @NotNull
        @Override
        public @Unmodifiable Set<? extends Annotation> getAnnotations() {
            return Collections.emptySet();
        }

        @Contract(pure = true)
        @NotNull
        @Override
        public @Unmodifiable Set<HiddenApiRestriction> getHiddenApiRestrictions() {
            return Collections.emptySet();
        }

        @Contract(pure = true)
        @Override
        public @Nullable MethodImplementation getImplementation() {
            return null;
        }

        @NotNull
        @Override
        public String getDefiningClass() {
            return type;
        }

        @NotNull
        @Override
        public String getName() {
            return methodName;
        }

        @Contract(value = " -> new", pure = true)
        @NotNull
        @Override
        public @Unmodifiable List<? extends CharSequence> getParameterTypes() {
            return Collections.singletonList("I");
        }

        @NotNull
        @Override
        public String getReturnType() {
            return "V";
        }
    }
}
