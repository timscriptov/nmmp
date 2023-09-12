import com.nmmedit.apkprotect.ApkFolders
import com.nmmedit.apkprotect.ApkProtect
import com.nmmedit.apkprotect.dex2c.converter.ClassAnalyzer
import com.nmmedit.apkprotect.dex2c.converter.instructionrewriter.RandomInstructionRewriter
import com.nmmedit.apkprotect.dex2c.filters.BasicKeepConfig
import com.nmmedit.apkprotect.dex2c.filters.SimpleConvertConfig
import com.nmmedit.apkprotect.dex2c.filters.SimpleRules
import com.nmmedit.apkprotect.log.ApkLogger
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class VmpTask(
    private val input: File,
    private val output: File,
    private val rules: File,
    private val logs: MutableList<String>,
) : ApkLogger {
    fun start() {
        val simpleRules = SimpleRules().apply {
            parse(InputStreamReader(FileInputStream(rules), StandardCharsets.UTF_8))
        }
        val filterConfig = SimpleConvertConfig(BasicKeepConfig(), simpleRules)
        ApkProtect.Builder(ApkFolders(input, output)).apply {
            setInstructionRewriter(RandomInstructionRewriter())
            setApkVerifyCodeGenerator(null)
            setFilter(filterConfig)
            setLogger(this@VmpTask)
            setClassAnalyzer(ClassAnalyzer())
        }.build().run()
    }

    override fun info(msg: String?) {
        msg?.let { logs.add("I: $it") }
    }

    override fun error(msg: String?) {
        msg?.let { logs.add("E: $it") }
    }

    override fun warning(msg: String?) {
        msg?.let { logs.add("W: $it") }
    }
}