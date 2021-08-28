package it.usuratonkachi.telegranloader.api

/*

class ApiStorage : TelegramApiStorage {

    override fun saveAuthKey(authKey: AuthKey) {
        try {
            FileUtils.writeByteArrayToFile(AUTH_KEY_FILE, authKey.key)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun loadAuthKey(): AuthKey? {
        try {
            return AuthKey(FileUtils.readFileToByteArray(AUTH_KEY_FILE))
        } catch (e: IOException) {
            if (e !is FileNotFoundException) e.printStackTrace()
        }
        return null
    }

    override fun saveDc(@NotNull dataCenter: DataCenter) {
        try {
            FileUtils.write(NEAREST_DC_FILE, dataCenter.toString(), Charset.defaultCharset())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun loadDc(): DataCenter? {
        try {
            val infos: List<String> = FileUtils.readFileToString(NEAREST_DC_FILE, Charset.defaultCharset()).split(":")
            return DataCenter(infos[0], infos[1].toInt())
        } catch (e: IOException) {
            if (e !is FileNotFoundException) e.printStackTrace()
        }
        return null
    }

    override fun deleteAuthKey() {
        try {
            FileUtils.forceDelete(AUTH_KEY_FILE)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun deleteDc() {
        try {
            FileUtils.forceDelete(NEAREST_DC_FILE)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun saveSession(@Nullable session: MTSession?) {}

    override fun loadSession(): MTSession? {
        return null
    }

    companion object {
        //Create File variable for auth.key and dc.save
        val AUTH_KEY_FILE = File("Properties/auth.key")
        val NEAREST_DC_FILE = File("Properties/dc.save")
    }
}
*/
