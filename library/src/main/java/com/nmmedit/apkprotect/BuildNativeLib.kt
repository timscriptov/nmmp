package com.nmmedit.apkprotect

import com.nmmedit.apkprotect.data.Prefs
import com.nmmedit.apkprotect.data.Prefs.getNdkAbi
import com.nmmedit.apkprotect.data.Prefs.getNdkStrip
import com.nmmedit.apkprotect.data.Prefs.getNdkToolchains
import com.nmmedit.apkprotect.data.Prefs.getNmmpName
import com.nmmedit.apkprotect.data.Prefs.getVmName
import com.nmmedit.apkprotect.log.ApkLogger
import java.io.*
import java.util.*

class BuildNativeLib(
    private val apkLogger: ApkLogger?
) {
    //编译出native lib，同时返回最后的so文件
    @Throws(IOException::class)
    fun build(options: CMakeOptions): List<File> {
        val cmakeArguments = options.getCmakeArguments()
        //cmake
        execCmd(cmakeArguments)

        //cmake --build <dir>
        execCmd(
            listOf(
                options.getCmakeBinaryPath(),
                "--build",
                options.getBuildPath()
            )
        )
        //strip
        val sharedObjectPath = options.getSharedObjectFile()
        for (file in sharedObjectPath) {
            execCmd(
                listOf(
                    options.getStripBinaryPath(),
                    "--strip-unneeded",
                    file.absolutePath
                )
            )
        }

        //编译成功的so
        return sharedObjectPath
    }

    @Throws(IOException::class)
    private fun execCmd(cmds: List<String>) {
        println(cmds)
        val builder = ProcessBuilder().command(cmds)
        val process = builder.start()
        printOutput(process.inputStream)
        printOutput(process.errorStream)
        try {
            val exitStatus = process.waitFor()
            if (exitStatus != 0) {
                throw IOException(String.format("Cmd '%s' exec failed", cmds.toString()))
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun printOutput(inputStream: InputStream) {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            apkLogger?.info(line)
            println(line)
        }
    }

    /**
     * cmake相关配置
     */
    class CMakeOptions(
        private val apiLevel: Int,
        private val projectHome: String,
        private val buildType: BuildType,
        private val abi: String
    ) {
        fun getCmakePath(): String {
            return Prefs.getCmakePath()
        }

        fun getSdkHome(): String {
            return Prefs.getSdkPath()
        }

        fun getNdkHome(): String {
            return Prefs.getNdkPath()
        }

        fun getLibOutputDir(): String {
            return File(File(projectHome, "obj"), abi).absolutePath
        }

        //cmake --build <BuildPath>
        fun getBuildPath(): String {
            return File(projectHome, String.format(".cxx/cmake/%s/%s", buildType.buildTypeName, abi)).absolutePath
        }

        fun getStripBinaryPath(): String {
            return File(getNdkHome(), getNdkToolchains() + "/" + getNdkAbi() + "/" + getNdkStrip()).absolutePath
        }

        fun getCmakeBinaryPath(): String {
            return File(getCmakePath(), "/bin/cmake").absolutePath
        }

        fun getNinjaBinaryPath(): String {
            return File(getCmakePath(), "/bin/ninja").absolutePath
        }

        fun getCmakeArguments(): List<String> {
            return listOf(
                getCmakeBinaryPath(),
                String.format("-H%s", File(projectHome, "dex2c").absoluteFile),
                String.format(
                    "-DCMAKE_TOOLCHAIN_FILE=%s",
                    File(getNdkHome(), "/build/cmake/android.toolchain.cmake").absoluteFile
                ),
                String.format("-DCMAKE_BUILD_TYPE=%s", buildType.buildTypeName),
                String.format("-DANDROID_ABI=%s", abi),
                String.format("-DANDROID_NDK=%s", getNdkHome()),
                String.format("-DANDROID_PLATFORM=android-%d", apiLevel),
                String.format("-DCMAKE_ANDROID_ARCH_ABI=%s", abi),
                String.format("-DCMAKE_ANDROID_NDK=%s", getNdkHome()),
                "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON",
                String.format("-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=%s", getLibOutputDir()),
                String.format("-DCMAKE_MAKE_PROGRAM=%s", getNinjaBinaryPath()),
                "-DCMAKE_SYSTEM_NAME=Android",
                String.format("-DCMAKE_SYSTEM_VERSION=%d", apiLevel),
                String.format("-B%s", getBuildPath()),
                "-GNinja"
            )
        }

        //最后输出的so文件
        fun getSharedObjectFile(): List<File> {
            //linux,etc.
            val vmLibName = "lib" + getVmName() + ".so"
            val nmmpLibName = "lib" + getNmmpName() + ".so"
            var vmSo = File(getLibOutputDir(), vmLibName)
            var mpSo = File(getLibOutputDir(), nmmpLibName)
            if (!vmSo.exists()) {
                //windows
                vmSo = File(getBuildPath(), "vm/$vmLibName")
            }
            if (!vmSo.exists()) {
                throw RuntimeException("Not Found so: " + vmSo.absolutePath)
            }
            if (!mpSo.exists()) {
                mpSo = File(getBuildPath(), nmmpLibName)
            }
            if (!mpSo.exists()) {
                throw RuntimeException("Not Found so: " + mpSo.absolutePath)
            }
            return listOf(vmSo, mpSo)
        }

        enum class BuildType(
            val buildTypeName: String
        ) {
            DEBUG("Debug"),
            RELEASE("Release")
        }
    }
}
