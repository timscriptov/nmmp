package com.mcal.nmmp

import com.nmmedit.apkprotect.ApkFolders
import com.nmmedit.apkprotect.ApkProtect
import com.nmmedit.apkprotect.deobfus.MappingReader
import com.nmmedit.apkprotect.dex2c.converter.ClassAnalyzer
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.RandomInstructionRewriter
import com.nmmedit.apkprotect.dex2c.filters.BasicKeepConfig
import com.nmmedit.apkprotect.dex2c.filters.ProguardMappingConfig
import com.nmmedit.apkprotect.dex2c.filters.SimpleConvertConfig
import com.nmmedit.apkprotect.dex2c.filters.SimpleRules
import com.nmmedit.apkprotect.log.ApkLogger
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.nio.charset.StandardCharsets

class Nmmp(
    private val inputApkFile: File,
    private val outputApkFile: File,
    private val rules: File? = null,
    private val mapping: File? = null,
    private val logger: ApkLogger? = null
) {
    fun obfuscate() {
        val simpleRules = SimpleRules()
        if (rules != null) {
            simpleRules.parse(InputStreamReader(FileInputStream(rules), StandardCharsets.UTF_8))
        } else {
            //all classes
            simpleRules.parse(StringReader("class *"))
        }
        val filterConfig = if (mapping != null) {
            ProguardMappingConfig(BasicKeepConfig(), MappingReader(mapping), simpleRules)
        } else {
            SimpleConvertConfig(BasicKeepConfig(), simpleRules)
        }
        ApkProtect.Builder(ApkFolders(inputApkFile, outputApkFile)).apply {
            setInstructionRewriter(RandomInstructionRewriter())
            setApkVerifyCodeGenerator(null)
            setFilter(filterConfig)
            setLogger(logger)
            setClassAnalyzer(ClassAnalyzer())
        }.build().run()
    }
}