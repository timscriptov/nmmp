package com.nmmedit.apkprotect;

import com.nmmedit.apkprotect.data.Prefs;
import com.nmmedit.apkprotect.log.ApkLogger;
import com.nmmedit.apkprotect.util.FileHelper;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nmmedit.apkprotect.ApkProtect.apkLogger;

public class BuildNativeLib {
    public static @NotNull Map<String, Map<File, File>> generateNativeLibs(@NotNull File outDir,
                                                                           @NotNull final List<String> abis) throws IOException {
        String cmakePath = Prefs.getCmakePath();
        String sdkHome = Prefs.getSdkPath();
        String ndkHome = Prefs.getNdkPath();

        final Map<String, Map<File, File>> allLibs = new HashMap<>();

        for (String abi : abis) {
            final BuildNativeLib.CMakeOptions cmakeOptions = new BuildNativeLib.CMakeOptions(cmakePath,
                    sdkHome,
                    ndkHome, 21,
                    outDir.getAbsolutePath(),
                    BuildNativeLib.CMakeOptions.BuildType.RELEASE,
                    abi);

            //删除上次创建的目录
            FileHelper.deleteFile(new File(cmakeOptions.getBuildPath()));

            final Map<File, File> files = BuildNativeLib.build(cmakeOptions);
            allLibs.put(abi, files);
        }
        return allLibs;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }


    //编译出native lib，同时返回最后的so文件
    public static @NotNull Map<File, File> build(@NotNull CMakeOptions options) throws IOException {

        final List<String> cmakeArguments = options.getCmakeArguments();
        //cmake
        execCmd(cmakeArguments);

        //cmake --build <dir>
        execCmd(Arrays.asList(
                options.getCmakeBinaryPath(),
                "--build",
                options.getBuildPath()
        ));
        //strip
        final Map<File, File> soMaps = options.getSharedObjectFileMap();
        for (Map.Entry<File, File> entry : soMaps.entrySet()) {
            execCmd(Arrays.asList(
                            options.getStripBinaryPath(),
                            "--strip-unneeded",
                            "-o",
                            entry.getValue().getAbsolutePath(),
                            entry.getKey().getAbsolutePath()
                    )
            );
        }

        //编译成功的so
        return soMaps;
    }

    private static void execCmd(List<String> cmds) throws IOException {
        final ApkLogger logger = apkLogger;
        if (logger != null) {
            logger.info(String.valueOf(cmds));
        } else {
            System.out.println(cmds);
        }
        final ProcessBuilder builder = new ProcessBuilder()
                .command(cmds);

        final Process process = builder.start();

        printOutput(process.getInputStream());
        printOutput(process.getErrorStream());

        try {
            final int exitStatus = process.waitFor();
            if (exitStatus != 0) {
                if (logger != null) {
                    logger.error(String.format("Cmd '%s' exec failed", cmds.toString()));
                } else {
                    throw new IOException(String.format("Cmd '%s' exec failed", cmds.toString()));
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void printOutput(InputStream inputStream) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        final ApkLogger logger = apkLogger;
        while ((line = reader.readLine()) != null) {
            if (logger != null) {
                logger.info(line);
            } else {
                System.out.println(line);
            }
        }
    }

    /**
     * cmake相关配置
     */
    public static class CMakeOptions {
        private final String cmakePath;

        private final String sdkHome;

        private final String ndkHome;
        private final int apiLevel;
        private final String projectHome;

        private final BuildType buildType;

        private final String abi;

        public CMakeOptions(String cmakePath,
                            String sdkHome,
                            String ndkHome,
                            int apiLevel,
                            String projectHome,
                            BuildType buildType,
                            String abi) {
            this.cmakePath = cmakePath;
            this.sdkHome = sdkHome;
            this.ndkHome = ndkHome;
            this.apiLevel = apiLevel;
            this.projectHome = projectHome;
            this.buildType = buildType;
            this.abi = abi;
        }

        public String getCmakePath() {
            return cmakePath;
        }

        public String getSdkHome() {
            return sdkHome;
        }

        public String getNdkHome() {
            return ndkHome;
        }

        public int getApiLevel() {
            return apiLevel;
        }

        public String getProjectHome() {
            return projectHome;
        }

        public BuildType getBuildType() {
            return buildType;
        }

        public String getAbi() {
            return abi;
        }

        @NotNull
        public String getLibStripOutputDir() {
            return new File(new File(getProjectHome(), "obj/strip"), abi).getAbsolutePath();
        }

        @NotNull
        public String getLibSymOutputDir() {
            return new File(new File(getProjectHome(), "obj/sym"), abi).getAbsolutePath();
        }

        //camke --build <BuildPath>
        public String getBuildPath() {
            return new File(getProjectHome(),
                    String.format(".cxx/cmake/%s/%s", getBuildType().getBuildTypeName(), getAbi())).getAbsolutePath();
        }

        public String getStripBinaryPath() {
            return new File(getNdkHome(), Prefs.getNdkToolchains() + "/" +
                    Prefs.getNdkAbi() + "/" + Prefs.getNdkStrip()).getAbsolutePath();
        }

        public String getCmakeBinaryPath() {
            return new File(getCmakePath(), "/bin/cmake").getAbsolutePath();
        }

        public String getNinjaBinaryPath() {
            return new File(getCmakePath(), "/bin/ninja").getAbsolutePath();
        }

        public List<String> getCmakeArguments() {
            return Arrays.asList(
                    getCmakeBinaryPath(),
                    String.format("-H%s", new File(getProjectHome(), "dex2c").getAbsoluteFile()),
                    String.format("-DCMAKE_TOOLCHAIN_FILE=%s", new File(getNdkHome(), "/build/cmake/android.toolchain.cmake").getAbsoluteFile()),
                    String.format("-DCMAKE_BUILD_TYPE=%s", getBuildType().getBuildTypeName()),
                    String.format("-DANDROID_ABI=%s", getAbi()),
                    String.format("-DANDROID_NDK=%s", getNdkHome()),
                    String.format("-DANDROID_PLATFORM=android-%d", getApiLevel()),
                    String.format("-DCMAKE_ANDROID_ARCH_ABI=%s", getAbi()),
                    String.format("-DCMAKE_ANDROID_NDK=%s", getNdkHome()),
                    "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON",
                    String.format("-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=%s", getLibSymOutputDir()),
                    String.format("-DCMAKE_MAKE_PROGRAM=%s", getNinjaBinaryPath()),
                    "-DCMAKE_SYSTEM_NAME=Android",
                    String.format("-DCMAKE_SYSTEM_VERSION=%d", getApiLevel()),
                    String.format("-B%s", getBuildPath()),
                    "-GNinja");
        }

        //未strip的so跟strip之后的so，
        @NotNull
        public Map<File, File> getSharedObjectFileMap() {
            final HashMap<File, File> map = new HashMap<>();
            final String libSymOutputDir = getLibSymOutputDir();

            final File stripOutputDir = new File(getLibStripOutputDir());
            if (!stripOutputDir.exists()) stripOutputDir.mkdirs();

            final String vm = "lib" + Prefs.getVmName() + ".so";

            File vmFile = new File(libSymOutputDir, vm);
            if (!vmFile.exists()) {
                //windows
                vmFile = new File(getBuildPath(), "vm/" + vm);
            }
            final ApkLogger logger = apkLogger;
            if (!vmFile.exists()) {
                if (logger != null) {
                    logger.warning("Not Found so: " + vmFile.getAbsolutePath());
                } else {
                    throw new RuntimeException("Not Found so: " + vmFile.getAbsolutePath());
                }
            }

            map.put(vmFile, new File(stripOutputDir, vm));

            final String vmp = "lib" + Prefs.getNmmpName() + ".so";
            File vmpFile = new File(libSymOutputDir, vmp);
            if (!vmpFile.exists()) {
                //windows
                vmpFile = new File(getBuildPath(), vmp);
            }
            if (!vmpFile.exists()) {
                if (logger != null) {
                    logger.warning("Not Found so: " + vmpFile.getAbsolutePath());
                } else {
                    throw new RuntimeException("Not Found so: " + vmpFile.getAbsolutePath());
                }
            }
            map.put(vmpFile, new File(stripOutputDir, vmp));

            return map;
        }


        public enum BuildType {
            DEBUG("Debug"),
            RELEASE("Release");

            private final String buildTypeName;

            BuildType(String buildTypeName) {
                this.buildTypeName = buildTypeName;
            }

            public String getBuildTypeName() {
                return buildTypeName;
            }
        }
    }
}
