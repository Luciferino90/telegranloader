package it.usuratonkachi.telegranloader


import it.tdlight.common.ExceptionHandler
import it.tdlight.common.Init
import it.tdlight.common.ResultHandler
import it.tdlight.common.TelegramClient
import it.tdlight.common.utils.CantLoadLibrary
import it.tdlight.jni.TdApi.*
import it.tdlight.tdlib.ClientManager
import java.io.BufferedReader
import java.io.IOError
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


/**
 * Example class for TDLib usage from Java.
 * (Based on the official TDLib java example)
 */
object Example {
    private var client: TelegramClient? = null
    private var authorizationState: AuthorizationState? = null

    @Volatile
    private var haveAuthorization = false

    @Volatile
    private var needQuit = false

    @Volatile
    private var canQuit = false
    private val defaultHandler: ResultHandler = DefaultHandler()
    private val authorizationLock: Lock = ReentrantLock()
    private val gotAuthorization = authorizationLock.newCondition()
    private val users: ConcurrentMap<Int, User> = ConcurrentHashMap()
    private val basicGroups: ConcurrentMap<Int, BasicGroup> = ConcurrentHashMap()
    private val supergroups: ConcurrentMap<Int, Supergroup> = ConcurrentHashMap()
    private val secretChats: ConcurrentMap<Int, SecretChat> = ConcurrentHashMap()
    private val chats: ConcurrentMap<Long, Chat> = ConcurrentHashMap()
    private val mainChatList: NavigableSet<OrderedChat> = TreeSet()
    private var haveFullMainChatList = false
    private val usersFullInfo: ConcurrentMap<Int, UserFullInfo> = ConcurrentHashMap()
    private val basicGroupsFullInfo: ConcurrentMap<Int, BasicGroupFullInfo> = ConcurrentHashMap()
    private val supergroupsFullInfo: ConcurrentMap<Int, SupergroupFullInfo> = ConcurrentHashMap()
    private val newLine = System.getProperty("line.separator")
    private const val commandsLine =
        "Enter command (gcs - GetChats, gc <chatId> - GetChat, me - GetMe, sm <chatId> <message> - SendMessage, lo - LogOut, q - Quit): "

    @Volatile
    private var currentPrompt: String? = null
    private fun printo(str: String?) {
        if (currentPrompt != null) {
            println("")
        }
        println(str)
        if (currentPrompt != null) {
            print(currentPrompt)
        }
    }

    private fun setChatPositions(chat: Chat?, positions: Array<ChatPosition?>) {
        synchronized(mainChatList) {
            synchronized(chat!!) {
                for (position in chat.positions) {
                    if (position.list.constructor == ChatListMain.CONSTRUCTOR) {
                        val isRemoved = mainChatList.remove(OrderedChat(chat.id, position))
                        assert(isRemoved)
                    }
                }
                chat.positions = positions
                for (position in chat.positions) {
                    if (position.list.constructor == ChatListMain.CONSTRUCTOR) {
                        val isAdded = mainChatList.add(OrderedChat(chat.id, position))
                        assert(isAdded)
                    }
                }
            }
        }
    }

    private fun onAuthorizationStateUpdated(authorizationState: AuthorizationState?) {
        if (authorizationState != null) {
            Example.authorizationState = authorizationState
        }
        when (Example.authorizationState!!.constructor) {
            AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                val parameters = TdlibParameters()
                parameters.databaseDirectory = "tdlib"
                parameters.useMessageDatabase = true
                parameters.useSecretChats = true
                parameters.apiId = 94575
                parameters.apiHash = "a3406de8d171bb422bb6ddf3bbd800e2"
                parameters.systemLanguageCode = "en"
                parameters.deviceModel = "Desktop"
                parameters.applicationVersion = "1.0"
                parameters.enableStorageOptimizer = true
                client!!.send(SetTdlibParameters(parameters), AuthorizationRequestHandler())
            }
            AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> client!!.send(
                CheckDatabaseEncryptionKey(),
                AuthorizationRequestHandler()
            )
            AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                val phoneNumber = promptString("Please enter phone number: ")
                client!!.send(SetAuthenticationPhoneNumber(phoneNumber, null), AuthorizationRequestHandler())
            }
            AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR -> {
                val link = (Example.authorizationState as AuthorizationStateWaitOtherDeviceConfirmation?)!!.link
                println("Please confirm this login link on another device: $link")
            }
            AuthorizationStateWaitCode.CONSTRUCTOR -> {
                val code = promptString("Please enter authentication code: ")
                client!!.send(CheckAuthenticationCode(code), AuthorizationRequestHandler())
            }
            AuthorizationStateWaitRegistration.CONSTRUCTOR -> {
                val firstName = promptString("Please enter your first name: ")
                val lastName = promptString("Please enter your last name: ")
                client!!.send(RegisterUser(firstName, lastName), AuthorizationRequestHandler())
            }
            AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                val password = promptString("Please enter password: ")
                client!!.send(CheckAuthenticationPassword(password), AuthorizationRequestHandler())
            }
            AuthorizationStateReady.CONSTRUCTOR -> {
                haveAuthorization = true
                authorizationLock.lock()
                try {
                    gotAuthorization.signal()
                } finally {
                    authorizationLock.unlock()
                }
            }
            AuthorizationStateLoggingOut.CONSTRUCTOR -> {
                haveAuthorization = false
                printo("Logging out")
            }
            AuthorizationStateClosing.CONSTRUCTOR -> {
                haveAuthorization = false
                printo("Closing")
            }
            AuthorizationStateClosed.CONSTRUCTOR -> {
                printo("Closed")
                if (!needQuit) {
                    client = ClientManager.create() // recreate client after previous has closed
                    client!!.initialize(UpdateHandler(), ErrorHandler(), ErrorHandler())
                } else {
                    canQuit = true
                }
            }
            else -> System.err.println("Unsupported authorization state:" + newLine + Example.authorizationState)
        }
    }

    private fun toInt(arg: String): Int {
        var result = 0
        try {
            result = arg.toInt()
        } catch (ignored: NumberFormatException) {
        }
        return result
    }

    private fun getChatId(arg: String): Long {
        var chatId: Long = 0
        try {
            chatId = arg.toLong()
        } catch (ignored: NumberFormatException) {
        }
        return chatId
    }

    private fun promptString(prompt: String): String {
        printo(prompt)
        currentPrompt = prompt
        val reader = BufferedReader(InputStreamReader(System.`in`))
        var str = ""
        try {
            str = reader.readLine()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        currentPrompt = null
        return str
    }

    private val command: Unit
        private get() {
            val command = promptString(commandsLine)
            val commands = command.split(" ", "2")
            try {
                when (commands[0]) {
                    "gcs" -> {
                        var limit = 20
                        if (commands.size > 1) {
                            limit = toInt(commands[1])
                        }
                        getMainChatList(limit)
                    }
                    "gc" -> client!!.send(GetChat(getChatId(commands[1])), defaultHandler)
                    "me" -> client!!.send(GetMe(), defaultHandler)
                    "sm" -> {
                        val args = commands[1].split(" ", "2")
                        sendMessage(getChatId(args[0]), args[1])
                    }
                    "lo" -> {
                        haveAuthorization = false
                        client!!.send(LogOut(), defaultHandler)
                    }
                    "q" -> {
                        needQuit = true
                        haveAuthorization = false
                        client!!.send(Close(), defaultHandler)
                    }
                    else -> System.err.println("Unsupported command: $command")
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                printo("Not enough arguments")
            }
        }

    private fun getMainChatList(limit: Int) {
        synchronized(mainChatList) {
            if (!haveFullMainChatList && limit > mainChatList.size) {
                // have enough chats in the chat list or chat list is too small
                var offsetOrder = Long.MAX_VALUE
                var offsetChatId: Long = 0
                if (!mainChatList.isEmpty()) {
                    val last = mainChatList.last()
                    offsetOrder = last.position.order
                    offsetChatId = last.chatId
                }
                client!!.send(
                    GetChats(
                        ChatListMain(),
                        offsetOrder,
                        offsetChatId,
                        limit - mainChatList.size
                    )
                ) { `object` ->
                    when (`object`.constructor) {
                        Error.CONSTRUCTOR -> System.err.println("Receive an error for GetChats:" + newLine + `object`)
                        Chats.CONSTRUCTOR -> {
                            val chatIds = (`object` as Chats).chatIds
                            if (chatIds.size == 0) {
                                synchronized(mainChatList) { haveFullMainChatList = true }
                            }
                            // chats had already been received through updates, let's retry request
                            getMainChatList(limit)
                        }
                        else -> System.err.println("Receive wrong response from TDLib:" + newLine + `object`)
                    }
                }
                return
            }

            // have enough chats in the chat list to answer request
            val iter: Iterator<OrderedChat> = mainChatList.iterator()
            println()
            println("First " + limit + " chat(s) out of " + mainChatList.size + " known chat(s):")
            var i = 0
            while (i < limit && iter.hasNext()) {
                val chatId = iter.next().chatId
                val chat = chats[chatId]
                synchronized(chat!!) { println(chatId.toString() + ": " + chat.title) }
                i++
            }
            printo("")
        }
    }

    private fun sendMessage(chatId: Long, message: String) {
        // initialize reply markup just for testing
        val row = arrayOf(
            InlineKeyboardButton("https://telegram.org?1", InlineKeyboardButtonTypeUrl()),
            InlineKeyboardButton("https://telegram.org?2", InlineKeyboardButtonTypeUrl()),
            InlineKeyboardButton("https://telegram.org?3", InlineKeyboardButtonTypeUrl())
        )
        val replyMarkup: ReplyMarkup = ReplyMarkupInlineKeyboard(arrayOf(row, row, row))
        val content: InputMessageContent = InputMessageText(FormattedText(message, null), false, true)
        client!!.send(SendMessage(chatId, 0, 0, null, replyMarkup, content), defaultHandler)
    }

    @Throws(InterruptedException::class, CantLoadLibrary::class)
    @JvmStatic
    fun __main(args: Array<String>) {

        // create client
        Init.start()
        client = ClientManager.create()
        client!!.initialize(UpdateHandler(), ErrorHandler(), ErrorHandler())
        client!!.execute(SetLogVerbosityLevel(0))
        // disable TDLib log
        if (client!!.execute(SetLogStream(LogStreamFile("tdlib.log", 1 shl 27, false))) is Error) {
            throw IOError(IOException("Write access to the current directory is required"))
        }

        // test Client.execute
        defaultHandler.onResult(client!!.execute(GetTextEntities("@telegram /test_command https://telegram.org telegram.me @gif @test")))

        // main loop
        while (!needQuit) {
            // await authorization
            authorizationLock.lock()
            try {
                while (!haveAuthorization) {
                    gotAuthorization.await()
                }
            } finally {
                authorizationLock.unlock()
            }
            while (haveAuthorization) {
                command
            }
        }
        while (!canQuit) {
            Thread.sleep(1)
        }
    }

    class OrderedChat internal constructor(val chatId: Long, val position: ChatPosition) : Comparable<OrderedChat?> {
        override fun equals(obj: Any?): Boolean {
            val o = obj as OrderedChat?
            return chatId == o!!.chatId && position.order == o.position.order
        }

        override fun compareTo(other: OrderedChat?): Int {
            if (position.order != other!!.position.order) {
                return if (other!!.position.order < position.order) -1 else 1
            }
            return if (chatId != other!!.chatId) {
                if (other!!.chatId < chatId) -1 else 1
            } else 0
        }
    }

    private class DefaultHandler : ResultHandler {
        override fun onResult(`object`: Object) {
            printo(`object`.toString())
        }
    }

    private class UpdateHandler : ResultHandler {
        override fun onResult(`object`: Object) {
            println(`object`.constructor)
            when (`object`.constructor) {
                UpdateAuthorizationState.CONSTRUCTOR -> onAuthorizationStateUpdated((`object` as UpdateAuthorizationState).authorizationState)
                UpdateUser.CONSTRUCTOR -> {
                    val updateUser = `object` as UpdateUser
                    users[updateUser.user.id] = updateUser.user
                }
                UpdateUserStatus.CONSTRUCTOR -> {
                    val updateUserStatus = `object` as UpdateUserStatus
                    val user = users[updateUserStatus.userId]
                    synchronized(user!!) { user.status = updateUserStatus.status }
                }
                UpdateBasicGroup.CONSTRUCTOR -> {
                    val updateBasicGroup = `object` as UpdateBasicGroup
                    basicGroups[updateBasicGroup.basicGroup.id] = updateBasicGroup.basicGroup
                }
                UpdateSupergroup.CONSTRUCTOR -> {
                    val updateSupergroup = `object` as UpdateSupergroup
                    supergroups[updateSupergroup.supergroup.id] = updateSupergroup.supergroup
                }
                UpdateSecretChat.CONSTRUCTOR -> {
                    val updateSecretChat = `object` as UpdateSecretChat
                    secretChats[updateSecretChat.secretChat.id] = updateSecretChat.secretChat
                }
                UpdateNewChat.CONSTRUCTOR -> {
                    val updateNewChat = `object` as UpdateNewChat
                    val chat = updateNewChat.chat
                    synchronized(chat) {
                        chats[chat.id] = chat
                        val positions = chat.positions
                        chat.positions = arrayOfNulls(0)
                        setChatPositions(chat, positions)
                    }
                }
                UpdateChatTitle.CONSTRUCTOR -> {
                    val updateChat = `object` as UpdateChatTitle
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) { chat.title = updateChat.title }
                }
                UpdateChatPhoto.CONSTRUCTOR -> {
                    val updateChat = `object` as UpdateChatPhoto
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) { chat.photo = updateChat.photo }
                }
                UpdateChatLastMessage.CONSTRUCTOR -> {
                    val updateChat = `object` as UpdateChatLastMessage
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        chat.lastMessage = updateChat.lastMessage
                        setChatPositions(chat, updateChat.positions)
                    }
                }
                UpdateChatPosition.CONSTRUCTOR -> {
                    val updateChat = `object` as UpdateChatPosition
                    if (updateChat.position.list.constructor != ChatListMain.CONSTRUCTOR) {
                        return
                    }
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        var i: Int
                        i = 0
                        while (i < chat.positions.size) {
                            if (chat.positions[i].list.constructor == ChatListMain.CONSTRUCTOR) {
                                break
                            }
                            i++
                        }
                        val new_positions =
                            arrayOfNulls<ChatPosition>(chat.positions.size + (if (updateChat.position.order == 0L) 0 else 1) - if (i < chat.positions.size) 1 else 0)
                        var pos = 0
                        if (updateChat.position.order != 0L) {
                            new_positions[pos++] = updateChat.position
                        }
                        var j = 0
                        while (j < chat.positions.size) {
                            if (j != i) {
                                new_positions[pos++] = chat.positions[j]
                            }
                            j++
                        }
                        assert(pos == new_positions.size)
                        setChatPositions(chat, new_positions)
                    }
                }
                UpdateChatReadInbox.CONSTRUCTOR -> {
                    val updateChat = `object` as UpdateChatReadInbox
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        chat.lastReadInboxMessageId = updateChat.lastReadInboxMessageId
                        chat.unreadCount = updateChat.unreadCount
                    }
                }
                UpdateChatReadOutbox.CONSTRUCTOR -> {
                    val updateChat = `object` as UpdateChatReadOutbox
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) { chat.lastReadOutboxMessageId = updateChat.lastReadOutboxMessageId }
                }
                UpdateChatUnreadMentionCount.CONSTRUCTOR -> {
                    val updateChat = `object` as UpdateChatUnreadMentionCount
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) { chat.unreadMentionCount = updateChat.unreadMentionCount }
                }
                UpdateMessageMentionRead.CONSTRUCTOR -> {
                    val updateChat = `object` as UpdateMessageMentionRead
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) { chat.unreadMentionCount = updateChat.unreadMentionCount }
                }
                UpdateChatReplyMarkup.CONSTRUCTOR -> {
                    val updateChat = `object` as UpdateChatReplyMarkup
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) { chat.replyMarkupMessageId = updateChat.replyMarkupMessageId }
                }
                UpdateChatDraftMessage.CONSTRUCTOR -> {
                    val updateChat = `object` as UpdateChatDraftMessage
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        chat.draftMessage = updateChat.draftMessage
                        setChatPositions(chat, updateChat.positions)
                    }
                }
                UpdateChatPermissions.CONSTRUCTOR -> {
                    val update = `object` as UpdateChatPermissions
                    val chat = chats[update.chatId]
                    synchronized(chat!!) { chat.permissions = update.permissions }
                }
                UpdateChatNotificationSettings.CONSTRUCTOR -> {
                    val update = `object` as UpdateChatNotificationSettings
                    val chat = chats[update.chatId]
                    synchronized(chat!!) { chat.notificationSettings = update.notificationSettings }
                }
                UpdateChatDefaultDisableNotification.CONSTRUCTOR -> {
                    val update = `object` as UpdateChatDefaultDisableNotification
                    val chat = chats[update.chatId]
                    synchronized(chat!!) { chat.defaultDisableNotification = update.defaultDisableNotification }
                }
                UpdateChatIsMarkedAsUnread.CONSTRUCTOR -> {
                    val update = `object` as UpdateChatIsMarkedAsUnread
                    val chat = chats[update.chatId]
                    synchronized(chat!!) { chat.isMarkedAsUnread = update.isMarkedAsUnread }
                }
                UpdateChatIsBlocked.CONSTRUCTOR -> {
                    val update = `object` as UpdateChatIsBlocked
                    val chat = chats[update.chatId]
                    synchronized(chat!!) { chat.isBlocked = update.isBlocked }
                }
                UpdateChatHasScheduledMessages.CONSTRUCTOR -> {
                    val update = `object` as UpdateChatHasScheduledMessages
                    val chat = chats[update.chatId]
                    synchronized(chat!!) { chat.hasScheduledMessages = update.hasScheduledMessages }
                }
                UpdateUserFullInfo.CONSTRUCTOR -> {
                    val updateUserFullInfo = `object` as UpdateUserFullInfo
                    usersFullInfo[updateUserFullInfo.userId] = updateUserFullInfo.userFullInfo
                }
                UpdateBasicGroupFullInfo.CONSTRUCTOR -> {
                    val updateBasicGroupFullInfo = `object` as UpdateBasicGroupFullInfo
                    basicGroupsFullInfo[updateBasicGroupFullInfo.basicGroupId] =
                        updateBasicGroupFullInfo.basicGroupFullInfo
                }
                UpdateSupergroupFullInfo.CONSTRUCTOR -> {
                    val updateSupergroupFullInfo = `object` as UpdateSupergroupFullInfo
                    supergroupsFullInfo[updateSupergroupFullInfo.supergroupId] =
                        updateSupergroupFullInfo.supergroupFullInfo
                }
                else -> {
                }
            }
        }
    }

    private class ErrorHandler : ExceptionHandler {
        override fun onException(e: Throwable) {
            e.printStackTrace()
        }
    }

    private class AuthorizationRequestHandler : ResultHandler {
        override fun onResult(`object`: Object) {
            when (`object`.constructor) {
                Error.CONSTRUCTOR -> {
                    System.err.println("Receive an error:" + newLine + `object`)
                    onAuthorizationStateUpdated(null) // repeat last action
                }
                Ok.CONSTRUCTOR -> {
                }
                else -> System.err.println("Receive wrong response from TDLib:" + newLine + `object`)
            }
        }
    }
}
