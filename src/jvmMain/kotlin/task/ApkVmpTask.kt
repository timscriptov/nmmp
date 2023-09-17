package task

import com.nmmedit.apkprotect.ApkFolders
import com.nmmedit.apkprotect.ApkProtect
import com.nmmedit.apkprotect.deobfus.MappingReader
import com.nmmedit.apkprotect.dex2c.converter.ClassAnalyzer
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.RandomInstructionRewriter
import com.nmmedit.apkprotect.dex2c.filters.BasicKeepConfig
import com.nmmedit.apkprotect.dex2c.filters.ProguardMappingConfig
import com.nmmedit.apkprotect.dex2c.filters.SimpleConvertConfig
import com.nmmedit.apkprotect.dex2c.filters.SimpleRules
import com.nmmedit.apkprotect.log.VmpLogger
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class ApkVmpTask(
    private val input: String,
    private val output: String,
    private val rules: String,
    private val mapping: String,
    private val logs: MutableList<String>,
) : VmpLogger {
    fun start() {
        val simpleRules = SimpleRules().apply {
            parse(InputStreamReader(FileInputStream(rules), StandardCharsets.UTF_8))
        }
        val filterConfig = if (mapping.isNotEmpty()) {
            ProguardMappingConfig(BasicKeepConfig(), MappingReader(File(mapping)), simpleRules)
        } else {
            SimpleConvertConfig(BasicKeepConfig(), simpleRules)
        }
        ApkProtect.Builder(ApkFolders(File(input), File(output))).apply {
            setInstructionRewriter(RandomInstructionRewriter())
            setFilter(filterConfig)
            setLogger(this@ApkVmpTask)
            setClassAnalyzer(ClassAnalyzer())
        }.build().run()
    }

    override fun info(msg: String?) {
        if(!msg.isNullOrEmpty()) {
            logs.add("I: $msg")
        }
    }

    override fun error(msg: String?) {
        if(!msg.isNullOrEmpty()) {
            logs.add("E: $msg")
        }
    }

    override fun warning(msg: String?) {
        if(!msg.isNullOrEmpty()) {
            logs.add("W: $msg")
        }
    }
}