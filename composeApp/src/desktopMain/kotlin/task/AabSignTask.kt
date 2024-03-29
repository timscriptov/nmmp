package task

import com.nmmedit.apkprotect.log.VmpLogger
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths

class AabSignTask(
    private val output: String,
    private val keystorePath: String,
    private val keystorePassword: String,
    private val keystoreAlias: String,
    private val keystoreAliasPassword: String,
    private val logs: MutableList<String>,
) : VmpLogger {
    fun start() {
        if (keystorePath.isEmpty()) {
            info("No keystore, skip sign.")
            return
        }

        if (!Files.exists(Paths.get(keystorePath))) {
            info("Keystore $keystorePath does not exist, skip sign.")
            return
        }


        try {
            execCmd(
                listOf(
                    "jarsigner",
                    "-sigalg",
                    "SHA256withRSA",
                    "-digestalg",
                    "SHA-256",
                    "-keystore",
                    keystorePath,
                    "-storepass",
                    keystorePassword,
                    "-keypass",
                    keystoreAliasPassword,
                    output,
                    keystoreAlias,
                    "-tsa",
                    "http://sha256timestamp.ws.symantec.com/sha256/timestamp"
                ), this
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun execCmd(cmds: List<String>, logger: VmpLogger?) {
        if (logger != null) {
            logger.info(cmds.toString())
        } else {
            println(cmds)
        }
        val builder = ProcessBuilder()
            .command(cmds)

        val process = builder.start()

        printOutput(process.inputStream, logger)
        printOutput(process.errorStream, logger)

        try {
            val exitStatus = process.waitFor()
            if (exitStatus != 0) {
                if (logger != null) {
                    logger.error(String.format("Cmd '%s' exec failed", cmds))
                } else {
                    throw IOException(String.format("Cmd '%s' exec failed", cmds))
                }
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun printOutput(inputStream: InputStream, logger: VmpLogger?) {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        while ((reader.readLine().also { line = it }) != null) {
            if (logger != null) {
                logger.info(line)
            } else {
                println(line)
            }
        }
    }

    override fun info(msg: String?) {
        if (!msg.isNullOrEmpty()) {
            logs.add("I: $msg")
        }
    }

    override fun error(msg: String?) {
        if (!msg.isNullOrEmpty()) {
            logs.add("E: $msg")
        }
    }

    override fun warning(msg: String?) {
        if (!msg.isNullOrEmpty()) {
            logs.add("W: $msg")
        }
    }
}