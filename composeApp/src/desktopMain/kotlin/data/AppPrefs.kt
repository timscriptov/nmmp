package data

import com.mcal.preferences.PreferencesManager
import com.nmmedit.apkprotect.data.Prefs
import com.nmmedit.apkprotect.data.Storage

object AppPrefs : PreferencesManager(Storage.binDir, "nmmp_preferences.json") {
    /**
     * ApkSigner
     */
    @JvmStatic
    fun keystorePath(): String {
        return Prefs.getString("keystore_path", "")
    }

    @JvmStatic
    fun setKeystorePath(name: String) {
        Prefs.putString("keystore_path", name)
    }

    @JvmStatic
    fun keystorePass(): String {
        return Prefs.getString("keystore_pass", "")
    }

    @JvmStatic
    fun setKeystorePass(name: String) {
        Prefs.putString("keystore_pass", name)
    }

    @JvmStatic
    fun keystoreAlias(): String {
        return Prefs.getString("keystore_alias", "")
    }

    @JvmStatic
    fun setKeystoreAlias(name: String) {
        Prefs.putString("keystore_alias", name)
    }

    @JvmStatic
    fun keystoreAliasPass(): String {
        return Prefs.getString("keystore_alias_pass", "")
    }

    @JvmStatic
    fun setKeystoreAliasPass(name: String) {
        Prefs.putString("keystore_alias_pass", name)
    }

    /**
     * Config
     */
    @JvmStatic
    fun rulesPath(): String {
        return Prefs.getString("rules_path", "")
    }

    @JvmStatic
    fun setRulesPath(name: String) {
        Prefs.putString("rules_path", name)
    }

    @JvmStatic
    fun mappingPath(): String {
        return Prefs.getString("mapping_path", "")
    }

    @JvmStatic
    fun setMappingPath(name: String) {
        Prefs.putString("mapping_path", name)
    }
}
