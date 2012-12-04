package pardalote

import groovy.beans.Bindable
import ca.odell.glazedlists.*

class PardaloteModel {
    final String appName = "pardalote"

    @Bindable String appId = ""

    @Bindable String accessToken = ""

    @Bindable String expiresIn = ""

    @Bindable String fullName = ""

    @Bindable String email = ""

    @Bindable String statusMessageId = ""

    @Bindable String statusMessageSubject = ""

    @Bindable String statusMessageBody = ""

    @Bindable String statusMessageImageFilePath = ""

    @Bindable Integer statusMessageSendPredeterminedTime = 0

    @Bindable Integer statusMessageSendWaitingHours = 0

    @Bindable Integer statusMessageNotYetSend = 0

    @Bindable String messageId = ""

    @Bindable String messageToFriends = ""

    @Bindable String messageToFriendsName = ""

    @Bindable String messageSubject = ""

    @Bindable String messageBody = ""

    @Bindable String messageImageFilePath = ""

    @Bindable Integer messageSendPredeterminedTime = 0

    @Bindable Integer messageSendWaitingHours = 0

    @Bindable String birthdayMessageBody1 = ""

    @Bindable String birthdayMessageBody2 = ""

    @Bindable String birthdayMessageBody3 = ""

    @Bindable String birthdayMessageBody4 = ""

    @Bindable String birthdayMessageBody5 = ""

    @Bindable String birthdayMessageId = ""

    @Bindable Integer birthdayMessageSendPredeterminedTime = 0

    @Bindable String birthdayMessageToFriends = ""

    @Bindable String birthdayMessageToFriendsName = ""

    @Bindable Integer autoClickLikeHoursOfStartFromNow = 0

    @Bindable Integer autoClickLikeHoursOfEndFromNow = 99

    @Bindable Integer autoClickLikeToMe = 0

    @Bindable String statusbarText = ""

    @Bindable String dialogConditionOfUserName = ""

    @Bindable String dialogStatusbarText = ""

    @Bindable boolean doAutoSendStatusMessage = false

    @Bindable boolean doAutoSendMessage = false

    @Bindable boolean doAutoSendBirthdayMessage = false

    @Bindable boolean doAutoClickLike = false

    @Bindable boolean doAutoClickLikeNow = false

    @Bindable boolean doEraseSendStatusMessage = false

    @Bindable boolean doEraseSendMessage = false

    @Bindable boolean countOfNotYetSendStatusMessageIsNotOverMax = false

    @Bindable boolean countOfNotYetSendMessageIsNotOverMax = false

    @Bindable boolean editable = false

    EventList statusMessageEventList = new SortedList(
        new BasicEventList(),
        { a, b -> b.updatedTime <=> a.updatedTime } as Comparator
    )

    EventList messageEventList = new SortedList(
        new BasicEventList(),
        { a, b -> b.updatedTime <=> a.updatedTime } as Comparator
    )

    EventList birthdayMessageEventList = new SortedList(
        new BasicEventList(),
        { a, b -> b.updatedTime <=> a.updatedTime } as Comparator
    )

    EventList friendsEventList = new SortedList(
        new BasicEventList(),
        { a, b -> b.updatedTime <=> a.updatedTime } as Comparator
    )

    @Bindable String dialogCaller = ""

    Map snapshot = [:]

    @Bindable String oldStatusMessageSubject = ""

    @Bindable String oldStatusMessageBody = ""

    @Bindable String oldStatusMessageImageFilePath = ""

    @Bindable Integer oldStatusMessageSendPredeterminedTime = 0

    @Bindable Integer oldStatusMessageSendWaitingHours = 0

}
