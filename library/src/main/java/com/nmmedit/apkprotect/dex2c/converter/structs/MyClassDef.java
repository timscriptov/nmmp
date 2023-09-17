package com.nmmedit.apkprotect.dex2c.converter.structs;

import com.android.tools.smali.dexlib2.base.reference.BaseTypeReference;
import com.android.tools.smali.dexlib2.iface.Annotation;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Field;
import com.android.tools.smali.dexlib2.iface.Method;
import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class MyClassDef extends BaseTypeReference implements ClassDef {
    @NotNull
    private final ClassDef classDef;
    @NotNull
    private final List<? extends Method> directMethods;
    @NotNull
    private final List<? extends Method> virtualMethods;


    public MyClassDef(@NotNull ClassDef classDef,
                      @NotNull List<? extends Method> directMethods,
                      @NotNull List<? extends Method> virtualMethods) {
        this.classDef = classDef;
        this.directMethods = directMethods;
        this.virtualMethods = virtualMethods;
    }

    @NotNull
    @Override
    public String getType() {
        return classDef.getType();
    }

    @Override
    public int getAccessFlags() {
        return classDef.getAccessFlags();
    }

    @Nullable
    @Override
    public String getSuperclass() {
        return classDef.getSuperclass();
    }

    @NotNull
    @Override
    public List<String> getInterfaces() {
        return classDef.getInterfaces();
    }

    @Nullable
    @Override
    public String getSourceFile() {
        return null;
    }

    @NotNull
    @Override
    public Set<? extends Annotation> getAnnotations() {
        return classDef.getAnnotations();
    }

    @NotNull
    @Override
    public Iterable<? extends Field> getStaticFields() {
        return classDef.getStaticFields();
    }

    @NotNull
    @Override
    public Iterable<? extends Field> getInstanceFields() {
        return classDef.getInstanceFields();
    }

    @NotNull
    @Override
    public Iterable<? extends Field> getFields() {
        return classDef.getFields();
    }

    @NotNull
    @Override
    public Iterable<? extends Method> getDirectMethods() {
        return directMethods;
    }

    @NotNull
    @Override
    public Iterable<? extends Method> getVirtualMethods() {
        return virtualMethods;
    }

    @NotNull
    @Override
    public Iterable<? extends Method> getMethods() {
//        return Iterables.concat(directMethods, virtualMethods);
        return new AbstractCollection<Method>() {
            @NotNull
            @Override
            public Iterator<Method> iterator() {
                return Iterators.concat(directMethods.iterator(), virtualMethods.iterator());
            }

            @Override
            public int size() {
                return directMethods.size() + virtualMethods.size();
            }
        };
    }
}