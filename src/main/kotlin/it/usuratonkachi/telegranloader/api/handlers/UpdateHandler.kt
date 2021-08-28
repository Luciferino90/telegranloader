package it.usuratonkachi.telegranloader.api.handlers

import it.tdlight.common.ResultHandler
import it.tdlight.jni.TdApi
import it.usuratonkachi.telegranloader.Example
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Component
class UpdateHandler(
    private val authorizationRequestHandler: AuthorizationRequestHandler
) : ResultHandler {

    private val users: ConcurrentMap<Int, TdApi.User> = ConcurrentHashMap()
    private val basicGroups: ConcurrentMap<Int, TdApi.BasicGroup> = ConcurrentHashMap()
    private val supergroups: ConcurrentMap<Int, TdApi.Supergroup> = ConcurrentHashMap()
    private val secretChats: ConcurrentMap<Int, TdApi.SecretChat> = ConcurrentHashMap()
    private val chats: ConcurrentMap<Long, TdApi.Chat> = ConcurrentHashMap()
    private val mainChatList: NavigableSet<Example.OrderedChat> = TreeSet()
    private val usersFullInfo: ConcurrentMap<Int, TdApi.UserFullInfo> = ConcurrentHashMap()
    private val basicGroupsFullInfo: ConcurrentMap<Int, TdApi.BasicGroupFullInfo> = ConcurrentHashMap()
    private val supergroupsFullInfo: ConcurrentMap<Int, TdApi.SupergroupFullInfo> = ConcurrentHashMap()

    override fun onResult(`object`: TdApi.Object) {
        println(`object`.constructor)
        when (`object`.constructor) {
            TdApi.UpdateAuthorizationState.CONSTRUCTOR -> authorizationRequestHandler.onAuthorizationStateUpdated((`object` as TdApi.UpdateAuthorizationState).authorizationState)
            TdApi.UpdateUser.CONSTRUCTOR -> {
                val updateUser = `object` as TdApi.UpdateUser
                users[updateUser.user.id] = updateUser.user
            }
            TdApi.UpdateUserStatus.CONSTRUCTOR -> {
                val updateUserStatus = `object` as TdApi.UpdateUserStatus
                val user = users[updateUserStatus.userId]
                synchronized(user!!) { user.status = updateUserStatus.status }
            }
            TdApi.UpdateBasicGroup.CONSTRUCTOR -> {
                val updateBasicGroup = `object` as TdApi.UpdateBasicGroup
                basicGroups[updateBasicGroup.basicGroup.id] = updateBasicGroup.basicGroup
            }
            TdApi.UpdateSupergroup.CONSTRUCTOR -> {
                val updateSupergroup = `object` as TdApi.UpdateSupergroup
                supergroups[updateSupergroup.supergroup.id] = updateSupergroup.supergroup
            }
            TdApi.UpdateSecretChat.CONSTRUCTOR -> {
                val updateSecretChat = `object` as TdApi.UpdateSecretChat
                secretChats[updateSecretChat.secretChat.id] = updateSecretChat.secretChat
            }
            TdApi.UpdateNewChat.CONSTRUCTOR -> {
                val updateNewChat = `object` as TdApi.UpdateNewChat
                val chat = updateNewChat.chat
                synchronized(chat) {
                    chats[chat.id] = chat
                    val positions = chat.positions
                    chat.positions = arrayOfNulls(0)
                    setChatPositions(chat, positions)
                }
            }
            TdApi.UpdateChatTitle.CONSTRUCTOR -> {
                val updateChat = `object` as TdApi.UpdateChatTitle
                val chat = chats[updateChat.chatId]
                synchronized(chat!!) { chat.title = updateChat.title }
            }
            TdApi.UpdateChatPhoto.CONSTRUCTOR -> {
                val updateChat = `object` as TdApi.UpdateChatPhoto
                val chat = chats[updateChat.chatId]
                synchronized(chat!!) { chat.photo = updateChat.photo }
            }
            TdApi.UpdateChatLastMessage.CONSTRUCTOR -> {
                val updateChat = `object` as TdApi.UpdateChatLastMessage
                val chat = chats[updateChat.chatId]
                synchronized(chat!!) {
                    chat.lastMessage = updateChat.lastMessage
                    setChatPositions(chat, updateChat.positions)
                }
            }
            TdApi.UpdateChatPosition.CONSTRUCTOR -> {
                val updateChat = `object` as TdApi.UpdateChatPosition
                if (updateChat.position.list.constructor != TdApi.ChatListMain.CONSTRUCTOR) {
                    return
                }
                val chat = chats[updateChat.chatId]
                synchronized(chat!!) {
                    var i: Int
                    i = 0
                    while (i < chat.positions.size) {
                        if (chat.positions[i].list.constructor == TdApi.ChatListMain.CONSTRUCTOR) {
                            break
                        }
                        i++
                    }
                    val new_positions =
                        arrayOfNulls<TdApi.ChatPosition>(chat.positions.size + (if (updateChat.position.order == 0L) 0 else 1) - if (i < chat.positions.size) 1 else 0)
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
            TdApi.UpdateChatReadInbox.CONSTRUCTOR -> {
                val updateChat = `object` as TdApi.UpdateChatReadInbox
                val chat = chats[updateChat.chatId]
                synchronized(chat!!) {
                    chat.lastReadInboxMessageId = updateChat.lastReadInboxMessageId
                    chat.unreadCount = updateChat.unreadCount
                }
            }
            TdApi.UpdateChatReadOutbox.CONSTRUCTOR -> {
                val updateChat = `object` as TdApi.UpdateChatReadOutbox
                val chat = chats[updateChat.chatId]
                synchronized(chat!!) { chat.lastReadOutboxMessageId = updateChat.lastReadOutboxMessageId }
            }
            TdApi.UpdateChatUnreadMentionCount.CONSTRUCTOR -> {
                val updateChat = `object` as TdApi.UpdateChatUnreadMentionCount
                val chat = chats[updateChat.chatId]
                synchronized(chat!!) { chat.unreadMentionCount = updateChat.unreadMentionCount }
            }
            TdApi.UpdateMessageMentionRead.CONSTRUCTOR -> {
                val updateChat = `object` as TdApi.UpdateMessageMentionRead
                val chat = chats[updateChat.chatId]
                synchronized(chat!!) { chat.unreadMentionCount = updateChat.unreadMentionCount }
            }
            TdApi.UpdateChatReplyMarkup.CONSTRUCTOR -> {
                val updateChat = `object` as TdApi.UpdateChatReplyMarkup
                val chat = chats[updateChat.chatId]
                synchronized(chat!!) { chat.replyMarkupMessageId = updateChat.replyMarkupMessageId }
            }
            TdApi.UpdateChatDraftMessage.CONSTRUCTOR -> {
                val updateChat = `object` as TdApi.UpdateChatDraftMessage
                val chat = chats[updateChat.chatId]
                synchronized(chat!!) {
                    chat.draftMessage = updateChat.draftMessage
                    setChatPositions(chat, updateChat.positions)
                }
            }
            TdApi.UpdateChatPermissions.CONSTRUCTOR -> {
                val update = `object` as TdApi.UpdateChatPermissions
                val chat = chats[update.chatId]
                synchronized(chat!!) { chat.permissions = update.permissions }
            }
            TdApi.UpdateChatNotificationSettings.CONSTRUCTOR -> {
                val update = `object` as TdApi.UpdateChatNotificationSettings
                val chat = chats[update.chatId]
                synchronized(chat!!) { chat.notificationSettings = update.notificationSettings }
            }
            TdApi.UpdateChatDefaultDisableNotification.CONSTRUCTOR -> {
                val update = `object` as TdApi.UpdateChatDefaultDisableNotification
                val chat = chats[update.chatId]
                synchronized(chat!!) { chat.defaultDisableNotification = update.defaultDisableNotification }
            }
            TdApi.UpdateChatIsMarkedAsUnread.CONSTRUCTOR -> {
                val update = `object` as TdApi.UpdateChatIsMarkedAsUnread
                val chat = chats[update.chatId]
                synchronized(chat!!) { chat.isMarkedAsUnread = update.isMarkedAsUnread }
            }
            TdApi.UpdateChatIsBlocked.CONSTRUCTOR -> {
                val update = `object` as TdApi.UpdateChatIsBlocked
                val chat = chats[update.chatId]
                synchronized(chat!!) { chat.isBlocked = update.isBlocked }
            }
            TdApi.UpdateChatHasScheduledMessages.CONSTRUCTOR -> {
                val update = `object` as TdApi.UpdateChatHasScheduledMessages
                val chat = chats[update.chatId]
                synchronized(chat!!) { chat.hasScheduledMessages = update.hasScheduledMessages }
            }
            TdApi.UpdateUserFullInfo.CONSTRUCTOR -> {
                val updateUserFullInfo = `object` as TdApi.UpdateUserFullInfo
                usersFullInfo[updateUserFullInfo.userId] = updateUserFullInfo.userFullInfo
            }
            TdApi.UpdateBasicGroupFullInfo.CONSTRUCTOR -> {
                val updateBasicGroupFullInfo = `object` as TdApi.UpdateBasicGroupFullInfo
                basicGroupsFullInfo[updateBasicGroupFullInfo.basicGroupId] =
                    updateBasicGroupFullInfo.basicGroupFullInfo
            }
            TdApi.UpdateSupergroupFullInfo.CONSTRUCTOR -> {
                val updateSupergroupFullInfo = `object` as TdApi.UpdateSupergroupFullInfo
                supergroupsFullInfo[updateSupergroupFullInfo.supergroupId] =
                    updateSupergroupFullInfo.supergroupFullInfo
            }
            else -> {
            }
        }
    }

    private fun setChatPositions(chat: TdApi.Chat?, positions: Array<TdApi.ChatPosition?>) {
        synchronized(mainChatList) {
            synchronized(chat!!) {
                for (position in chat.positions) {
                    if (position.list.constructor == TdApi.ChatListMain.CONSTRUCTOR) {
                        val isRemoved = mainChatList.remove(Example.OrderedChat(chat.id, position))
                        assert(isRemoved)
                    }
                }
                chat.positions = positions
                for (position in chat.positions) {
                    if (position.list.constructor == TdApi.ChatListMain.CONSTRUCTOR) {
                        val isAdded = mainChatList.add(Example.OrderedChat(chat.id, position))
                        assert(isAdded)
                    }
                }
            }
        }
    }

}
