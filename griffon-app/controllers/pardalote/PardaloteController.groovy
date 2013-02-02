package pardalote

import ca.odell.glazedlists.*
import ca.odell.glazedlists.gui.*
import ca.odell.glazedlists.swing.*
import com.restfb.*
import com.restfb.types.*
import groovy.sql.*
import groovy.util.logging.*
import javax.swing.*
import static javax.swing.JOptionPane.QUESTION_MESSAGE
import static javax.swing.JOptionPane.YES_NO_OPTION
import static javax.swing.JOptionPane.YES_OPTION
import static javax.swing.JOptionPane.showMessageDialog
import static javax.swing.JOptionPane.showOptionDialog

@Log
class PardaloteController {
    def MAX_COUNT_OF_NOT_YET_SEND = 30
//    def MAX_COUNT_OF_NOT_YET_SEND = 3

    def MAX_COUNT_OF_HAS_SEND = 200
//    def MAX_COUNT_OF_HAS_SEND = 2

    def RANDOM_WAIT_TIME_FOR_LIKE_PUBLISHER = 290
//    def RANDOM_WAIT_TIME_FOR_LIKE_PUBLISHER = 110

    def model
    def view

    def gsql

    FacebookApi facebookApi = null

    FacebookClient client = null

    def limitTimeChecker

    def statusMessagePublisher

    def statusMessagePublisher2

    def statusMessageLockObject = new Object()

    def messagePublisher

    def messagePublisher2

    def messageLockObject = new Object()

    def birthdayMessagePublisher

    def likePublisher

    /*
        Remember that actions will be called outside of the UI thread
        by default. You can change this setting of course.
        Please read chapter 9 of the Griffon Guide to know more.

    def action = { evt = null ->
    }
    */
    def onStartupEnd = { app ->
        log.info "onStartupEnd - start."

        model.statusbarText = "画面を表示します..."

        view.messageToFriendsSelectedDialog.hide()

        doOutside{
            if( !Lock.getLock() ) {
                edt{
                    JOptionPane.showMessageDialog( view.mainFrame, "${model.appName}が既に起動しています。" )
                }
                System.exit(1)
            }

            def dbfile = new File("./${model.appName}.db")

            def url = "jdbc:sqlite:${dbfile.name}"
            def driver = "org.sqlite.JDBC"
            this.gsql = Sql.newInstance(
                url
                ,driver
            )

            if( dbfile.size() == 0 ) {
                this.gsql.execute("""
                    create table status_message (
                        id integer primary key,
                        subject text,
                        body text,
                        image_file_path text,
                        send_predetermined_time integer,
                        send_waiting_hours integer,
                        send_time integer,
                        updated_time integer
                    )
                """)
                this.gsql.execute("""
                    create table message (
                        id integer primary key,
                        to_friends text,
                        to_friends_name text,
                        subject text,
                        body text,
                        image_file_path text,
                        send_predetermined_time integer,
                        send_waiting_hours integer,
                        send_time integer,
                        updated_time integer
                    )
                """)
                this.gsql.execute("""
                    create table birthday_message (
                        id integer primary key,
                        to_friends text,
                        to_friends_name text,
                        send_predetermined_time integer,
                        send_friends text,
                        updated_time integer
                    )
                """)
                this.gsql.execute("""
                    create table settings (
                        app_id text,
                        access_token text,
                        expires_in text,
                        do_auto_send_status_message integer,
                        do_auto_send_message integer,
                        do_auto_send_birthday_message integer,
                        do_auto_click_like integer,
                        do_erase_send_status_message integer,
                        do_erase_send_message integer,
                        birthday_message_body1 text,
                        birthday_message_body2 text,
                        birthday_message_body3 text,
                        birthday_message_body4 text,
                        birthday_message_body5 text,
                        auto_click_like_hours_of_start_from_now integer,
                        auto_click_like_hours_of_end_from_now integer,
                        auto_click_like_to_me integer,
                        limit_time text
                    )
                """)
                this.gsql.execute("""
                    insert into settings (
                        app_id,
                        access_token,
                        expires_in,
                        do_auto_send_status_message,
                        do_auto_send_message,
                        do_auto_send_birthday_message,
                        do_auto_click_like,
                        do_erase_send_status_message,
                        do_erase_send_message,
                        birthday_message_body1,
                        birthday_message_body2,
                        birthday_message_body3,
                        birthday_message_body4,
                        birthday_message_body5,
                        auto_click_like_hours_of_start_from_now,
                        auto_click_like_hours_of_end_from_now,
                        auto_click_like_to_me,
                        limit_time
                    )
                    values (
                        '',
                        '',
                        '',
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        '',
                        '',
                        '',
                        '',
                        '',
                        0,
                        99,
                        0,
                        ''
                    )
                """)

            }

            def settings = this.gsql.firstRow("""
                SELECT
                    app_id AS appId,
                    access_token AS accessToken,
                    expires_in AS expiresIn,
                    do_auto_send_status_message AS doAutoSendStatusMessage,
                    do_auto_send_message AS doAutoSendMessage,
                    do_auto_send_birthday_message AS doAutoSendBirthdayMessage,
                    do_auto_click_like AS doAutoClickLike,
                    do_erase_send_status_message AS doEraseSendStatusMessage,
                    do_erase_send_message AS doEraseSendMessage,
                    birthday_message_body1 AS birthdayMessageBody1,
                    birthday_message_body2 AS birthdayMessageBody2,
                    birthday_message_body3 AS birthdayMessageBody3,
                    birthday_message_body4 AS birthdayMessageBody4,
                    birthday_message_body5 AS birthdayMessageBody5,
                    auto_click_like_hours_of_start_from_now AS autoClickLikeHoursOfStartFromNow,
                    auto_click_like_hours_of_end_from_now AS autoClickLikeHoursOfEndFromNow,
                    auto_click_like_to_me AS autoClickLikeToMe,
                    limit_time AS limitTime
                FROM
                    settings
            """)

            model.with{
                appId = settings.appId
                accessToken = settings.accessToken
                doAutoSendStatusMessage = settings.doAutoSendStatusMessage
                doAutoSendMessage = settings.doAutoSendMessage
                doAutoSendBirthdayMessage = settings.doAutoSendBirthdayMessage
                doAutoClickLike = settings.doAutoClickLike
                doEraseSendStatusMessage = settings.doEraseSendStatusMessage
                doEraseSendMessage = settings.doEraseSendMessage
                birthdayMessageBody1 = settings.birthdayMessageBody1
                birthdayMessageBody2 = settings.birthdayMessageBody2
                birthdayMessageBody3 = settings.birthdayMessageBody3
                birthdayMessageBody4 = settings.birthdayMessageBody4
                birthdayMessageBody5 = settings.birthdayMessageBody5
                autoClickLikeHoursOfStartFromNow = settings.autoClickLikeHoursOfStartFromNow
                autoClickLikeHoursOfEndFromNow = settings.autoClickLikeHoursOfEndFromNow
                autoClickLikeToMe = settings.autoClickLikeToMe
                limitTime = settings.limitTime
            }

            if( !model.appId ) {
                model.appId = JOptionPane.showInputDialog(view.mainFrame, "はじめにアプリケーションIDを設定してください。")?.trim() ?: ""

                if( model.appId.empty ) {
                    edt{
                        JOptionPane.showMessageDialog( view.mainFrame, "アプリケーションIDは必ず入力してください。" )
                    }
                    System.exit(1)
                }

                this.gsql.executeUpdate("""
                    UPDATE
                        settings
                    SET
                        app_id = ${model.appId}
                """)
            }


            facebookApi = new FacebookApi(
                appId:model.appId,
                scopeList:[
                    "create_event",
                    "email",
                    "friends_about_me",
                    "friends_likes",
                    "offline_access",
                    "publish_actions",
                    "publish_stream",
                    "read_friendlists",
                    "read_mailbox",
                    "read_stream",
                    "user_about_me",
                    "user_likes",
                    "user_birthday",
                    "friends_birthday",
                ],
            )

            facebookApi.accessToken = settings.accessToken
            facebookApi.expiresIn = settings.expiresIn
            facebookApi.limitTime = settings.limitTime

            try {
                try {
                    facebookApiLoad()
                } catch ( com.restfb.exception.FacebookOAuthException e ) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "facebook認証の有効期限が切れた可能性があります。\n再度認証を行います。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )

                    facebookApi.accessToken = ""
                    facebookApi.expiresIn = ""
                    facebookApi.limitTime = ""
                    facebookApiLoad()
                }
            } catch ( IOException e ) {
                if( !( e instanceof UnknownHostException || e instanceof NoRouteToHostException ) )
                    throw e

                edt{
                    JOptionPane.showMessageDialog( view.mainFrame, "インターネットに接続できません。\n【${model.appName}】を終了します。" )
                }
                System.exit(1)
            }

            this.reflushStatusMessage()
            this.reflushBirthdayMessage()
            this.reflushMessage()


            this.limitTimeChecker = Thread.start{
                while ( true ) {
                    try {
                        def now = Calendar.instance
                        if (facebookApi.limitTime) {
                            if (now.timeInMillis >= facebookApi.limitTime.toLong() - 60*1000) {
                                facebookApi.reset()

                                edt {
                                    this.gsql.executeUpdate("""
                                        UPDATE
                                            settings
                                        SET
                                            access_token = ${facebookApi.accessToken}
                                            ,expires_in = ${facebookApi.expiresIn}
                                            ,limit_time = ${facebookApi.limitTime}
                                    """)
                    
                                    model.accessToken = facebookApi.accessToken
                                    model.fullName = ""
                                    model.email = ""

                                    showMessageDialog(
                                        app.windowManager.windows[0],
                                        "API利用期限となりました。\nメニューより再度認証手続きを実施してください。"
                                    )
                                }
                            }
                            
                        }
                    } catch( Exception e ) {
                        log.info "", e
                    } finally {
                        sleep 10 * 1000
                    }
                }
            }

            this.statusMessagePublisher = Thread.start{
                while(true) {
                    try {
                        def accessToken = false
                        def check = false
                        edt {
                            check = model.doAutoSendStatusMessage
                            accessToken = model.accessToken
                        }

                        if ( (accessToken?:"").size() == 0 || !check ) {
                            Thread.sleep(10 * 1000)
                            continue
                        }

                        def currentTime = Calendar.instance

                        def statusMessageList = this.gsql.rows("""
                            SELECT
                                id
                                ,subject
                                ,body
                                ,image_file_path AS imageFilePath
                                ,updated_time AS updatedTime
                                ,send_predetermined_time AS sendPredeterminedTime
                            FROM
                                status_message
                            WHERE
                                send_time IS NULL
                                AND send_predetermined_time != 0
                            ORDER BY
                                updated_time
                        """).findAll{ statusMessage ->
                             ( (statusMessage.sendPredeterminedTime - 1) * 3 <= currentTime.format("HH").toInteger() &&
                             (statusMessage.sendPredeterminedTime) * 3 > currentTime.format("HH").toInteger() )
                        } as Queue

                        if ( statusMessageList.empty ) {
                            Thread.sleep(10 * 1000)
                            continue
                        }

                        def statusMessage = statusMessageList.poll()

                        postStatusMessage( statusMessage, client, currentTime.timeInMillis )

                        def endHours = ( (statusMessage.sendPredeterminedTime) * 3 ).toString().padLeft(2, "0")
                        def endTime = Date.parse(
                            "yyyy/MM/dd HH:mm"
                            ,"${currentTime.format('yyyy/MM/dd')} ${endHours}:00"
                        ).toCalendar()

                        if( !statusMessageList.empty ) {
                            def waitForTime = ( endTime.timeInMillis - currentTime.timeInMillis ) / (statusMessageList.size() + 1)
                            log.info "statusMessagePublisher - waitForTime = ${waitForTime}"
                            Thread.sleep(waitForTime.toInteger())
                        }
                    } catch ( IOException e ) {
                        if( !( e instanceof UnknownHostException || e instanceof NoRouteToHostException ) ) {
                            log.info "", e
                            continue
                        }

                        edt{
                            JOptionPane.showMessageDialog( view.mainFrame, "インターネットに接続できません。\n【${model.appName}】を終了します。" )
                        }
                        System.exit(1)
                    }
                }
            }

            this.statusMessagePublisher2 = Thread.start{
                while(true) {
                    try {
                        def accessToken = false
                        def check = false
                        edt {
                            check = model.doAutoSendStatusMessage
                            accessToken = model.accessToken
                        }

                        if ( (accessToken?:"").size() == 0 || !check ) {
                            Thread.sleep(10 * 1000)
                            continue
                        }

                        def currentTime = Calendar.instance

                        def statusMessageList = this.gsql.rows("""
                            SELECT
                                id
                                ,subject
                                ,body
                                ,image_file_path AS imageFilePath
                                ,updated_time AS updatedTime
                                ,send_waiting_hours AS sendWaitingHours
                            FROM
                                status_message
                            WHERE
                                send_time IS NULL
                                AND send_waiting_hours != 0
                            ORDER BY
                                updated_time
                        """
                        ).findAll{
                            def t = Calendar.instance
                            t.timeInMillis = it.updatedTime
                            t.add(Calendar.HOUR, it.sendWaitingHours)

                            t.timeInMillis <= currentTime.timeInMillis
                        } as Queue

                        if ( statusMessageList.empty ) {
                            Thread.sleep(10 * 1000)
                            continue
                        }

                        def statusMessage = statusMessageList.poll()

                        postStatusMessage( statusMessage, client, currentTime.timeInMillis )

                        Thread.sleep((10 + (Math.random() * 290).toInteger()) * 1000)
                    } catch ( IOException e ) {
                        if( !( e instanceof UnknownHostException || e instanceof NoRouteToHostException ) ) {
                            log.info "", e
                            continue
                        }

                        edt{
                            JOptionPane.showMessageDialog( view.mainFrame, "インターネットに接続できません。\n【${model.appName}】を終了します。" )
                        }
                        System.exit(1)
                    }
                }
            }

            this.messagePublisher = Thread.start{
                while(true) {
                    try{
                        def accessToken = false
                        def check = false
                        edt {
                            check = model.doAutoSendMessage
                            accessToken = model.accessToken
                        }

                        if ( (accessToken?:"").size() == 0 || !check ) {
                            Thread.sleep(10 * 1000)
                            continue
                        }

                        def currentTime = Calendar.instance

                        def messageList = this.gsql.rows("""
                            SELECT
                                id
                                ,to_friends AS toFriends
                                ,to_friends_name AS toFriendsName
                                ,subject
                                ,body
                                ,image_file_path AS imageFilePath
                                ,updated_time AS updatedTime
                                ,send_predetermined_time AS sendPredeterminedTime
                            FROM
                                message
                            WHERE
                                send_time IS NULL
                                AND send_predetermined_time != 0
                            ORDER BY
                                updated_time
                        """
                        ) .findAll{ message ->
                            ( (message.sendPredeterminedTime - 1) * 3 <= currentTime.format("HH").toInteger() &&
                            (message.sendPredeterminedTime) * 3 > currentTime.format("HH").toInteger() )
                        } as Queue

                        if ( messageList.empty) {
                            Thread.sleep(10 * 1000)
                            continue
                        }

                        def message = messageList.poll()

                        postMessage( message, client, currentTime.timeInMillis )

                        def endHours = ( (message.sendPredeterminedTime) * 3 ).toString().padLeft(2, "0")
                        def endTime = Date.parse(
                            "yyyy/MM/dd HH:mm"
                            ,"${currentTime.format('yyyy/MM/dd')} ${endHours}:00"
                        ).toCalendar()

                        if( !messageList.empty ) {
                            def waitTimeMills = ( endTime.timeInMillis - currentTime.timeInMillis ) / (messageList.size() + 1)
                            log.info "messagePublisher - waitTimeMills = ${waitTimeMills}"
                            Thread.sleep(waitTimeMills.toInteger())
                        }
                    } catch ( IOException e ) {
                        if( !( e instanceof UnknownHostException || e instanceof NoRouteToHostException ) ) {
                            log.info "", e
                            continue
                        }

                        edt{
                            JOptionPane.showMessageDialog( view.mainFrame, "インターネットに接続できません。\n【${model.appName}】を終了します。" )
                        }
                        System.exit(1)
                    }

                }
            }

            this.messagePublisher2 = Thread.start{
                while(true) {
                    try{
                        def accessToken = false
                        def check = false
                        edt {
                            check = model.doAutoSendMessage
                            accessToken = model.accessToken
                        }

                        if ( (accessToken?:"").size() == 0 || !check ) {
                            Thread.sleep(10 * 1000)
                            continue
                        }

                        def currentTime = Calendar.instance

                        def messageList = this.gsql.rows("""
                            SELECT
                                id
                                ,to_friends AS toFriends
                                ,to_friends_name AS toFriendsName
                                ,subject
                                ,body
                                ,image_file_path AS imageFilePath
                                ,updated_time AS updatedTime
                                ,send_waiting_hours AS sendWaitingHours
                            FROM
                                message
                            WHERE
                                send_time IS NULL
                                AND send_waiting_hours != 0
                            ORDER BY
                                updated_time
                        """
                        ).findAll{
                            def t = Calendar.instance
                            t.timeInMillis = it.updatedTime
                            t.add(Calendar.HOUR, it.sendWaitingHours)

                            t.timeInMillis <= currentTime.timeInMillis
                        } as Queue

                        if ( messageList.empty ) {
                            Thread.sleep(10 * 1000)
                            continue
                        }

                        def message = messageList.poll()

                        postMessage( message, client, currentTime.timeInMillis )

                        Thread.sleep((10 + (Math.random() * 290).toInteger()) * 1000)
                    } catch ( IOException e ) {
                        if( !( e instanceof UnknownHostException || e instanceof NoRouteToHostException ) ) {
                            log.info "", e
                            continue
                        }

                        edt{
                            JOptionPane.showMessageDialog( view.mainFrame, "インターネットに接続できません。\n【${model.appName}】を終了します。" )
                        }
                        System.exit(1)
                    }

                }
            }

            this.birthdayMessagePublisher = Thread.start{
                while(true) {
                    try{
                        def accessToken = ""
                        def check = false
                        edt {
                            check = model.doAutoSendBirthdayMessage
                            accessToken = model.accessToken
                        }

                        if ( (accessToken?:"").size() == 0 || !check ) {
                            Thread.sleep(10 * 1000)
                            continue
                        }

                        def birthdayMessageList = this.gsql.rows("""
                            SELECT
                                id
                                ,to_friends AS toFriends
                                ,to_friends_name AS toFriendsName
                                ,send_friends AS sendFriends
                                ,send_predetermined_time AS sendPredeterminedTime
                                ,updated_time as updatedTime
                            FROM
                                birthday_message
                            ORDER BY
                                updated_time
                        """)

                        birthdayMessageList.findAll{ birthdayMessage ->
                            def now = Calendar.instance.time
                            (birthdayMessage.sendPredeterminedTime) <= now.format("HH").toInteger() &&
                            now.format("HH").toInteger() < (birthdayMessage.sendPredeterminedTime + 1)
                        }.each{ birthdayMessage ->
                            def toFriends = birthdayMessage.toFriends.split("\n") ?: []
                            def sendFriends = birthdayMessage.sendFriends?.split("\n").findAll {
                                !!it
                            } ?: []

                            log.info "birthdayMessagePublisher - toFriends:${toFriends}"
                            log.info "birthdayMessagePublisher - sendFriends:${sendFriends}"

                            def now = Calendar.instance.time
                            def t = Calendar.instance
                            t.timeInMillis = birthdayMessage.updatedTime
                            if ( t.time.format("yyyyMMdd") < now.format("yyyyMMdd") ) sendFriends.clearAll()


                            ( toFriends - sendFriends ).each{ id ->
                                def friend = facebookApi.get("${id}", ["locale":"ja_JP"])
                                log.info "birthdayMessagePublisher - ${friend.birthday}"
                                log.info "birthdayMessagePublisher - ${new Date().format("MM/dd")}"
                                if( friend.birthday &&
                                    friend.birthday[0..4] == new Date().format("MM/dd")
                                ) {
                                    log.info "birthdayMessagePublisher - post"
                                    def bodies = [
                                        model.birthdayMessageBody1,
                                        model.birthdayMessageBody2,
                                        model.birthdayMessageBody3,
                                        model.birthdayMessageBody4,
                                        model.birthdayMessageBody3,
                                    ].findAll{
                                        it?.trim()
                                    }

                                    if( !bodies.empty ) {
                                        def content = Parameter.with("message", bodies[ (int)(bodies.size() * Math.random()) ].replaceAll(
                                            /\[\[%myid%\]\]/, model.fullName
                                        ).replaceAll(
                                            /\[\[%name%\]\]/, friend.name
                                        ))
                                        FacebookType publishMessageResponse = client.publish(
                                            "${id}/feed",
                                            FacebookType.class,
                                            content
                                        )

                                        sendFriends << id

                                        this.gsql.executeUpdate(
                                            "update birthday_message set updated_time = ${Calendar.instance.timeInMillis}, send_friends = ${sendFriends.sort().join('\n')} " +
                                            "where id = ${birthdayMessage.id}"
                                        )
                                        edt {
                                            this.reflushBirthdayMessage()
                                        }
                                    }
                                    Thread.sleep((10 + (Math.random() * 290).toInteger()) * 1000)
                                }
                            }
                        }

                        Thread.sleep(10 * 1000)

                    } catch ( IOException e ) {
                        if( !( e instanceof UnknownHostException || e instanceof NoRouteToHostException ) ) {
                            log.info "", e
                            continue
                        }

                        edt{
                            JOptionPane.showOptionDialog(
                                view.mainFrame,
                                "インターネットに接続できません。\n【${model.appName}】を終了します。",
                                "エラー",
                                JOptionPane.OK_OPTION,
                                JOptionPane.ERROR_MESSAGE,
                                null,
                                [ "閉じる" ] as String[], "閉じる"
                            )
                        }
                        System.exit(1)
                    }
                }
            }

            this.likePublisher = Thread.start{

                def blacklist = []
                while(true) {

                    if( !model.doAutoClickLike ) {
                        model.doAutoClickLikeNow = false
                        sleep( 5 * 1000 )
                        continue
                    }

                    ( 10 * 60 ).times {
                        if( !model.doAutoClickLike )
                            return
                        sleep( 1000 )
                    }

                    if( !model.doAutoClickLike )
                        continue

                    model.doAutoClickLikeNow = true

                    try {
                        def me = facebookApi.get("me", ["locale":"ja_JP"])

                        def accessToken = false
                        def startTime = Calendar.instance
                        def autoClickLikeHoursOfStartFromNow
                        def autoClickLikeHoursOfEndFromNow
                        def autoClickLikeToMe

                        edt {
                            accessToken = model.accessToken
                            autoClickLikeHoursOfStartFromNow = model.autoClickLikeHoursOfStartFromNow
                            autoClickLikeHoursOfEndFromNow = model.autoClickLikeHoursOfEndFromNow
                            autoClickLikeToMe = model.autoClickLikeToMe
                        }

                        def feedList = []

                        def searchFromCreateTime = startTime.timeInMillis - autoClickLikeHoursOfEndFromNow * 60 * 60 * 1000
                        def searchToCreateTime = startTime.timeInMillis - autoClickLikeHoursOfStartFromNow * 60 * 60 * 1000

                        def condition = { feed ->
                            return (
                                (accessToken?:"").size() != 0 &&
                                feed?.status_type &&
//                                !(feed?.status_type in ["app_created_story", "approved_friend"]) &&
                                feed.created_time &&
                                Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", feed.created_time).time >= searchFromCreateTime &&
                                Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", feed.created_time).time <= searchToCreateTime
                            )
                        }

                        feedList.addAll facebookApi.get("me/friends", ["locale":"ja_JP", "fields":"feed.fields(id,status_type,created_time)"]).data.feed*.data*.findAll(
                            condition
                        ).flatten().findAll {
                            !!it
                        }

                        if (autoClickLikeToMe) {
                            feedList.addAll facebookApi.get("me/feed", ["locale":"ja_JP"]).data.findAll( condition )
                        }

                        log.info "likePublisher - find feed list size is [ ${feedList.size()} ]."
                        log.info "likePublisher - ${feedList}."
                        feedList.findAll { feed ->
                            !blacklist.contains(feed.id)
                        }.sort{ feed ->
                            feed.created_time
                        }.each{ feed ->
                            log.info "likePublisher - ${feed}."

                            if( !model.doAutoClickLike )
                                return

                            try {
                                def likes = facebookApi.get("${feed.id}/likes", ["locale":"ja_JP"])

                                if( likes.data.id == null || !likes.data.id.contains( me.id )) {
                                    facebookApi.post("${feed.id}/likes")
                                    log.info "likePublisher - post like( like_id=${feed.id} )"
                                }

                                ((10 + (Math.random() * RANDOM_WAIT_TIME_FOR_LIKE_PUBLISHER).toInteger())).times {
                                    if( !model.doAutoClickLike )
                                        return
                                    sleep( 1000 )
                                }

                            } catch ( IOException e ) {
                                if( !(e instanceof UnknownHostException || e instanceof NoRouteToHostException) ) {
                                    log.info "likePublisher - add blacklist like( like_id=${feed.id} )"
                                    blacklist << feed.id
                                    Thread.sleep(5 * 1000)
                                } else {
                                    throw e
                                }
                            }
                        }
                    } catch ( IOException e ) {
                        if( e instanceof UnknownHostException || e instanceof NoRouteToHostException ) {
                            edt{
                                JOptionPane.showOptionDialog(
                                    view.mainFrame,
                                    "インターネットに接続できません。\n【${model.appName}】を終了します。",
                                    "エラー",
                                    JOptionPane.OK_OPTION,
                                    JOptionPane.ERROR_MESSAGE,
                                    null,
                                    [ "閉じる" ] as String[], "閉じる"
                                )
                            }
                            System.exit(1)
                        } else {
                            log.error "likePublisher - ERROR:", e
                        }

                    }

                }
            }

            edt{
                model.with{
                    statusbarText = "画面を表示しました。"
                    editable = true

                }
                model.snapshot = model.properties.clone()
            }
        }
    }

    def updateSettings = { evt ->
        log.info "updateSettings - start."
        model.with{
            this.gsql.executeUpdate("""
                update
                    settings
                set
                    app_id = ${appId},
                    do_auto_send_status_message = ${doAutoSendStatusMessage},
                    do_auto_send_message = ${doAutoSendMessage},
                    do_auto_send_birthday_message = ${doAutoSendBirthdayMessage},
                    do_auto_click_like = ${doAutoClickLike},
                    do_erase_send_status_message = ${doEraseSendStatusMessage},
                    do_erase_send_message = ${doEraseSendMessage}
            """)
        }

        log.info "updateSettings - end."
    }

    def deleteMessage = { evt ->
        log.info "deleteMessage - start."
        model.with{
            def count = new MessageDao("gsql":this.gsql).delete(
                messageId,
            )

            this.reflushMessage(evt)

            statusbarText = "削除が完了しました。"
            messageId = ""
            messageToFriends = ""
            messageToFriendsName = ""
            messageSubject = ""
            messageBody = ""
            messageImageFilePath = ""
        }
        log.info "deleteMessage - end."
    }

    def deleteBirthdayMessage = { evt ->
        log.info "deleteBirthdayMessage - start."
        model.with{
            this.gsql.executeUpdate("""
                delete from
                    birthday_message
                where
                    id = ${birthdayMessageId}
            """)

            this.reflushBirthdayMessage(evt)

            statusbarText = "削除が完了しました。"
            birthdayMessageId = ""
            birthdayMessageToFriends = ""
            birthdayMessageToFriendsName = ""
            birthdayMessageSendPredeterminedTime = 0
        }
        log.info "deleteBirthdayMessage - end."
    }

    def deleteStatusMessage = { evt ->
        log.info "deleteStatusMessage - start."
        model.with{

            def count = new StatusMessageDao("gsql":this.gsql).delete(
                statusMessageId,
            )

            this.reflushStatusMessage(evt)

            statusbarText = "削除が完了しました。"
            statusMessageId = ""
            statusMessageSubject = ""
            statusMessageBody = ""
            statusMessageImageFilePath = ""
        }
        log.info "deleteStatusMessage - end."
    }

    def updateMessage = { evt ->
        log.info "updateMessage - start."
        model.with{
            if( messageImageFilePath?.trim()?.size() && !new File(messageImageFilePath).exists() ) {
                JOptionPane.showMessageDialog( view.mainFrame, "指定されたファイルは存在しません。" )
                return
            }

            def count = new MessageDao("gsql":this.gsql).update(
                messageId,
                messageSubject,
                messageBody,
                messageImageFilePath,
                messageSendPredeterminedTime,
                messageSendWaitingHours,
                messageToFriends,
                messageToFriendsName
            )

            if( count < 1 ) {
                JOptionPane.showMessageDialog( view.mainFrame, "変更対象のメッセージはすでに送信済みです。" )
                return
            }

            this.reflushMessage(evt)

            statusbarText = "書き込みが完了しました。"
            messageId = ""
            messageToFriends = ""
            messageToFriendsName = ""
            messageSubject = ""
            messageBody = ""
            messageImageFilePath = ""
            messageSendPredeterminedTime = 0
            messageSendWaitingHours = 0
        }
        log.info "updateMessage - end."
    }

    def updateBirthdayMessage = { evt ->
        log.info "updateBirthdayMessage - start."
        model.with{
            this.gsql.executeUpdate("""
                update
                    birthday_message
                set
                    to_friends = ${birthdayMessageToFriends},
                    to_friends_name = ${birthdayMessageToFriendsName},
                    updated_time = ${Calendar.instance.timeInMillis},
                    send_predetermined_time = ${birthdayMessageSendPredeterminedTime}
                where
                    id = ${birthdayMessageId}
            """)

            this.reflushBirthdayMessage(evt)

            statusbarText = "書き込みが完了しました。"
            birthdayMessageId = ""
            birthdayMessageToFriends = ""
            birthdayMessageToFriendsName = ""
            birthdayMessageSendPredeterminedTime = 0
        }
        log.info "updateBirthdayMessage - end."
    }

    def updateBirthdayMessageTemplate = { evt ->
        log.info "updateBirthdayMessageTemplate - start."
        model.with{
            this.gsql.executeUpdate("""
                update
                    settings
                set
                    birthday_message_body1 = ${birthdayMessageBody1},
                    birthday_message_body2 = ${birthdayMessageBody2},
                    birthday_message_body3 = ${birthdayMessageBody3},
                    birthday_message_body4 = ${birthdayMessageBody4},
                    birthday_message_body5 = ${birthdayMessageBody5}
            """)

            statusbarText = "書き込みが完了しました。"
        }
        log.info "updateBirthdayMessageTemplate - end."
    }

    def updateStatusMessage = { evt ->
        log.info "updateStatusMessage - start."

        model.with{
            if( statusMessageImageFilePath?.trim()?.size() && !new File(statusMessageImageFilePath).exists() ) {
                JOptionPane.showMessageDialog( view.mainFrame, "指定されたファイルは存在しません。" )
                return
            }

            def count = new StatusMessageDao("gsql":this.gsql).update(
                statusMessageId,
                statusMessageSubject,
                statusMessageBody,
                statusMessageImageFilePath,
                statusMessageSendPredeterminedTime,
                statusMessageSendWaitingHours,
            )

            if( count < 1 ) {
                JOptionPane.showMessageDialog( view.mainFrame, "変更対象のメッセージはすでに送信済みです。" )
                return
            }

            this.reflushStatusMessage(evt)

            statusbarText = "書き込みが完了しました。"
            statusMessageId = ""
            statusMessageSubject = ""
            statusMessageBody = ""
            statusMessageImageFilePath = ""
            statusMessageSendPredeterminedTime = 0
            statusMessageSendWaitingHours = 0
        }

        log.info "updateStatusMessage - end."
    }

    def insertMessage = { evt ->
        log.info "insertMessage - start."

        model.with{
            if( messageImageFilePath?.trim()?.size() && !new File(messageImageFilePath).exists() ) {
                JOptionPane.showMessageDialog( view.mainFrame, "指定されたファイルは存在しません。" )
                return
            }

            new MessageDao("gsql":this.gsql).insert(
                messageSubject,
                messageBody,
                messageImageFilePath,
                messageSendPredeterminedTime,
                messageSendWaitingHours,
                messageToFriends,
                messageToFriendsName
            )

            this.reflushMessage(evt)

            statusbarText = "書き込みが完了しました。"
            messageId = ""
            messageToFriends = ""
            messageToFriendsName = ""
            messageSubject = ""
            messageBody = ""
            messageImageFilePath = ""
            messageSendPredeterminedTime = 0
            messageSendWaitingHours = 0

            model.snapshot = model.properties.clone()
        }

        log.info "insertMessage - end."
    }

    def insertBirthdayMessage = { evt ->
        log.info "insertBirthdayMessage - start."

        model.with{
            this.gsql.executeInsert("""
                insert into birthday_message (
                    id,
                    to_friends,
                    to_friends_name,
                    send_friends,
                    send_predetermined_time,
                    updated_time
                )
                values (
                    (select coalesce(max(id), -1) + 1 from birthday_message),
                    ${birthdayMessageToFriends},
                    ${birthdayMessageToFriendsName},
                    "",
                    ${birthdayMessageSendPredeterminedTime},
                    ${Calendar.instance.timeInMillis}
                )
            """)

            this.reflushBirthdayMessage(evt)

            statusbarText = "書き込みが完了しました。"
            birthdayMessageId = ""
            birthdayMessageToFriends = ""
            birthdayMessageToFriendsName = ""
            birthdayMessageSendPredeterminedTime = 0

            model.snapshot = model.properties.clone()
        }
        log.info "insertBirthdayMessage - end."
    }

    def insertStatusMessage = { evt ->
        log.info "insertStatusMessage - start."
        model.with{
            if( statusMessageImageFilePath?.trim()?.size() && !new File(statusMessageImageFilePath).exists() ) {
                JOptionPane.showMessageDialog( view.mainFrame, "指定されたファイルは存在しません。" )
                return
            }

            new StatusMessageDao("gsql":this.gsql).insert(
                statusMessageSubject,
                statusMessageBody,
                statusMessageImageFilePath,
                statusMessageSendPredeterminedTime,
                statusMessageSendWaitingHours
            )

            this.reflushStatusMessage(evt)

            statusbarText = "書き込みが完了しました。"
            statusMessageId = ""
            statusMessageSubject = ""
            statusMessageBody = ""
            statusMessageImageFilePath = ""
            statusMessageSendPredeterminedTime = 0
            statusMessageSendWaitingHours = 0
        }
        model.snapshot = model.properties.clone()
        log.info "insertStatusMessage - end."
    }

    def clearMessage = { evt ->
        log.info "clearMessage - start."
        model.with{
            statusbarText = "クリアしました。"
            messageId = ""
            messageToFriends = ""
            messageToFriendsName = ""
            messageSubject = ""
            messageBody = ""
            messageImageFilePath = ""
            messageSendPredeterminedTime = 0
            messageSendWaitingHours = 0
        }
        model.snapshot = model.properties.clone()
        log.info "clearMessage - end."
    }

    def clearBirthdayMessage = { evt ->
        log.info "clearBirthdayMessage - start."
        model.with{
            statusbarText = "クリアしました。"
            birthdayMessageId = ""
            birthdayMessageToFriends = ""
            birthdayMessageToFriendsName = ""
            birthdayMessageSendPredeterminedTime = 0
        }
        log.info "clearBirthdayMessage - end."
    }

    def clearStatusMessage = { evt ->
        log.info "clearStatusMessage - start."
        model.with{
            statusbarText = "クリアしました。"
            statusMessageId = ""
            statusMessageSubject = ""
            statusMessageBody = ""
            statusMessageImageFilePath = ""
            statusMessageSendPredeterminedTime = 0
            statusMessageSendWaitingHours = 0
        }
        model.snapshot = model.properties.clone()
        log.info "clearStatusMessage - end."
    }

    def unauth = { evt = null ->
        log.info "unauth - start."

        try {
            def params = model.properties.clone()
    
            JOptionPane.showMessageDialog( view.mainFrame, "認証を解除します。\n別アカウントで【facebook認証】を行う場合は、facebookサイトからログアウトしていることを確認してから実施してください。" )
            doOutside {
                facebookApi.reset()

                this.gsql.executeUpdate("""
                    UPDATE
                        settings
                    SET
                        access_token = ${facebookApi.accessToken}
                        ,expires_in = ${facebookApi.expiresIn}
                        ,limit_time = ${facebookApi.limitTime}
                """)
    
                doLater {
                    model.accessToken = facebookApi.accessToken
                    model.fullName = ""
                    model.email = ""
                }
            }
        } catch ( e ) {
            log.error "unauth - exception.", e
        } finally {
            log.info "unauth - end."
        }
    }
    

    def auth = { evt ->
        log.info "auth - start."
        model.accessToken = ""

        facebookApiLoad()
        log.info "auth - end."
    }

    def facebookApiLoad() {
        model.accessToken = !facebookApi.accessToken.empty ? facebookApi.accessToken: facebookApi.auth()


        this.gsql.executeUpdate("""
            UPDATE
                settings
            SET
                access_token = ${facebookApi.accessToken}
                ,expires_in = ${facebookApi.expiresIn}
                ,limit_time = ${facebookApi.limitTime}
        """)

        this.client = new DefaultFacebookClient(facebookApi.accessToken )

        def me = facebookApi.get("me", ["locale":"ja_JP"])

        model.fullName = "${me.last_name} ${me.first_name}"
        model.email = me.email
    }

    def selectMessage = { evt ->
        log.info "selectMessage - start."
        if( evt.clickCount > 1 && evt.source instanceof JTable ) {
            def table = evt.source

            def list = model.messageEventList
            def row = null;
            list.readWriteLock.readLock().lock()
            try {
                row = list[table.convertRowIndexToModel(table.selectedRow)]
            } finally {
                list.readWriteLock.readLock().unlock()
            }

            if ( row.sendTime ) {
                JOptionPane.showMessageDialog( view.mainFrame, "選択されたデータが既に送信済みのため開けません。" )
                return
            }

            model.with{
                messageToFriends = row.toFriends
                messageToFriendsName = row.toFriendsName
                messageSubject = row.subject
                messageBody = row.body
                messageImageFilePath = row.imageFilePath
                messageSendPredeterminedTime = row.sendPredeterminedTime
                messageSendWaitingHours = row.sendWaitingHours
            }
            model.snapshot = model.properties.clone()
            model.messageId = row.id
        }
        log.info "selectMessage - end."
    }

    def selectBirthdayMessage = { evt ->
        log.info "selectBirthdayMessage - start."
        if( evt.clickCount > 1 && evt.source instanceof JTable ) {
            def table = evt.source

            def list = model.birthdayMessageEventList
            def row = null;
            list.readWriteLock.readLock().lock()
            try {
                row = list[table.convertRowIndexToModel(table.selectedRow)]
            } finally {
                list.readWriteLock.readLock().unlock()
            }

            model.with{
                birthdayMessageId = row.id
                birthdayMessageToFriends = row.toFriends
                birthdayMessageToFriendsName = row.toFriendsName
                birthdayMessageSendPredeterminedTime = row.sendPredeterminedTime
            }
            model.snapshot = model.properties.clone()
        }
        log.info "selectBirthdayMessage - end."
    }

    def selectStatusMessage = { evt ->
        log.info "selectStatusMessage - start."
        if( evt.clickCount > 1 && evt.source instanceof JTable ) {
            def table = evt.source

            def list = model.statusMessageEventList
            def row = null;
            list.readWriteLock.readLock().lock()
            try {
                row = list[table.convertRowIndexToModel(table.selectedRow)]
            } finally {
                list.readWriteLock.readLock().unlock()
            }

            if ( row.sendTime ) {
                JOptionPane.showMessageDialog( view.mainFrame, "選択されたデータが既に送信済みのため開けません。" )
                return
            }

            model.with{
                statusMessageSubject = row.subject
                statusMessageBody = row.body
                statusMessageImageFilePath = row.imageFilePath
                statusMessageSendPredeterminedTime = row.sendPredeterminedTime
                statusMessageSendWaitingHours = row.sendWaitingHours

            }
            model.snapshot = model.properties.clone()
            model.statusMessageId = row.id

        }
        log.info "selectStatusMessage - end."
    }

    def reflushFriends = { evt ->
        log.info "reflushFriends - start."

        try {
            model.dialogStatusbarText = "最新情報を取得しています..."


            def list = model.friendsEventList

            def friends = this.facebookApi.get("me/friends", [ "fields":"id,name,picture", "locale":"ja_JP" ])

            list.readWriteLock.writeLock().lock()
            try {
                list.clear()
                list.addAll friends.data.findAll {
                    it.name.contains model.dialogConditionOfUserName
                }
            } finally {
                list.readWriteLock.writeLock().unlock()
            }

            model.dialogStatusbarText = "最新情報の取得が終わりました。"
        } catch ( IOException e ) {
            if( !( e instanceof UnknownHostException || e instanceof NoRouteToHostException ) )
                throw e

            JOptionPane.showMessageDialog( view.mainFrame, "インターネットに接続できません。\n【${model.appName}】を終了します。" )
            return
        } finally {
            log.info "reflushFriends - end."
        }
    }

    def selectFriends = { evt ->
        log.info "selectFriends - start."
        if( evt.clickCount > 1 && evt.source instanceof JTable ) {
            def table = evt.source

            def list = model.friendsEventList
            def row = null;
            list.readWriteLock.readLock().lock()
            try {
                row = list[table.convertRowIndexToModel(table.selectedRow)]
            } finally {
                list.readWriteLock.readLock().unlock()
            }
        }
        log.info "selectFriends - end."
    }

    def selectMessageToFriends = { evt ->
        log.info "selectMessageToFriends - start."
        def jlist = view.friendsListList
        if ( jlist.selectedIndices == null )
            return

        def list = model.friendsEventList
        def rows = [];
        list.readWriteLock.readLock().lock()
        try {
            ( jlist.selectedIndices ?: [] ).each{ i ->
                log.info list[i]?.toString()
                rows << list[i]
            }
        } finally {
            list.readWriteLock.readLock().unlock()
        }

        model.with{
            switch( model.dialogCaller ) {
                case "message":
                    messageToFriends = rows*.id.join("\n")
                    messageToFriendsName = rows*.name.join("\n")
                    break
                case "birthdayMessage":
                    birthdayMessageToFriends = rows*.id.join("\n")
                    birthdayMessageToFriendsName = rows*.name.join("\n")
                    break
            }
        }
        view.messageToFriendsSelectedDialog.hide()
        log.info "selectMessageToFriends - end."
    }

    def isEditing() {
        log.info "isEditing - start."
        switch( view.mainTab.selectedIndex ) {
            case 0:
                return !(
                    model.statusMessageSubject == model.snapshot.statusMessageSubject &&
                    model.statusMessageBody == model.snapshot.statusMessageBody &&
                    model.statusMessageImageFilePath == model.snapshot.statusMessageImageFilePath &&
                    model.statusMessageSendPredeterminedTime == model.snapshot.statusMessageSendPredeterminedTime &&
                    model.statusMessageSendWaitingHours == model.snapshot.statusMessageSendWaitingHours
                )
                break
            case 1:
                return !(
                    model.messageToFriends == model.snapshot.messageToFriends &&
                    model.messageSubject == model.snapshot.messageSubject &&
                    model.messageBody == model.snapshot.messageBody &&
                    model.messageImageFilePath == model.snapshot.messageImageFilePath &&
                    model.messageSendPredeterminedTime == model.snapshot.messageSendPredeterminedTime &&
                    model.messageSendWaitingHours == model.snapshot.messageSendWaitingHours
                )
                break
            case 2:
                return !(
                    model.birthdayMessageBody1 == model.snapshot.birthdayMessageBody1 &&
                    model.birthdayMessageBody2 == model.snapshot.birthdayMessageBody2 &&
                    model.birthdayMessageBody3 == model.snapshot.birthdayMessageBody3 &&
                    model.birthdayMessageBody4 == model.snapshot.birthdayMessageBody4 &&
                    model.birthdayMessageBody5 == model.snapshot.birthdayMessageBody5
                )
                break
            default:
                break
        }
        log.info "isEditing - end."
    }

    def windowClosing = { evt ->
        log.info "windowClosing - start."


        if( JOptionPane.showOptionDialog(
                view.mainFrame,
                "【${model.appName}】を終了します。\nよろしいですか？",
                "確認",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                [
                    "はい",
                    "いいえ"
                ] as String[],
                "いいえ",
            ) == JOptionPane.YES_OPTION ) {

            if( isEditing() && JOptionPane.showOptionDialog(
                        view.mainFrame,
                        "保存していない設定があります。\n【${model.appName}】を終了すると設定が失われます。終了してもよろしいですか？",
                        "確認",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        [
                            "はい",
                            "いいえ"
                        ] as String[],
                        "いいえ",
                    ) == JOptionPane.NO_OPTION ) {
                return
            }

            JOptionPane.showMessageDialog( view.mainFrame, "お疲れ様でした。" )
            view.mainFrame.dispose()
            System.exit(0)
        }
        log.info "windowClosing - end."
    }

    def reflushMessage = { evt ->
        log.info "reflushMessage - start."

        def dataList = this.gsql.rows("""
            SELECT
                id
                ,to_friends AS toFriends
                ,to_friends_name AS toFriendsName
                ,subject
                ,body
                ,image_file_path AS imageFilePath
                ,updated_time AS updatedTime
                ,send_predetermined_time AS sendPredeterminedTime
                ,send_waiting_hours AS sendWaitingHours
                ,send_time AS sendTime
            FROM
                message
            ORDER BY
                updated_time ASC LIMIT ${MAX_COUNT_OF_HAS_SEND} + (
                    SELECT
                            COUNT (*)
                        FROM
                            message
                        WHERE
                            send_time IS NULL
                )
        """)

        model.countOfNotYetSendMessageIsNotOverMax = dataList.findAll{
            !it.sendTime
        }.size() < MAX_COUNT_OF_NOT_YET_SEND

        def list = model.messageEventList
        list.readWriteLock.writeLock().lock()
        try {
            list.clear()
            list.addAll(dataList)
        } finally {
            list.readWriteLock.writeLock().unlock()
        }

        log.info "reflushMessage - end."
    }

    def reflushBirthdayMessage = { evt ->
        log.info "reflushBirthdayMessage - start."

        def dataList = this.gsql.rows("""
            SELECT
                id
                ,to_friends AS toFriends
                ,to_friends_name AS toFriendsName
                ,updated_time AS updatedTime
                ,send_predetermined_time AS sendPredeterminedTime
                ,send_friends AS sendFriends
            FROM
                birthday_message
            ORDER BY
                updated_time ASC
        """)

        def list = model.birthdayMessageEventList
        list.readWriteLock.writeLock().lock()
        try {
            list.clear()
            list.addAll(dataList)
        } finally {
            list.readWriteLock.writeLock().unlock()
        }

        log.info "reflushBirthdayMessage - end."
    }

    def reflushStatusMessage = {
        log.info "reflushStatusMessage - start."

        def dataList = this.gsql.rows("""
            SELECT
                id
                ,subject
                ,body
                ,image_file_path AS imageFilePath
                ,updated_time AS updatedTime
                ,send_predetermined_time AS sendPredeterminedTime
                ,send_waiting_hours AS sendWaitingHours
                ,send_time AS sendTime
            FROM
                status_message
            ORDER BY
                updated_time ASC LIMIT ${MAX_COUNT_OF_HAS_SEND} + (
                    SELECT
                            COUNT (*)
                        FROM
                            status_message
                        WHERE
                            send_time IS NULL
                )
        """)

        model.countOfNotYetSendStatusMessageIsNotOverMax = dataList.findAll{
            !it.sendTime
        }.size() < MAX_COUNT_OF_NOT_YET_SEND

        def list = model.statusMessageEventList
        list.readWriteLock.writeLock().lock()
        try {
            list.clear()
            list.addAll(dataList)
        } finally {
            list.readWriteLock.writeLock().unlock()
        }

        log.info "reflushStatusMessage - end."
    }

    def insertCurrentMessage = { evt = null ->
        log.info "insertCurrentMassage - start."
        try {
            def params = model.properties.clone()

            doOutside {
                if( params.messageImageFilePath?.trim()?.size() && !new File(params.messageImageFilePath).exists() ) {
                    JOptionPane.showMessageDialog( view.mainFrame, "指定されたファイルは存在しません。" )
                    return
                }

                def dao = new MessageDao("gsql":this.gsql)
                if( !params.messageId.empty )
                    def count = dao.delete(
                        params.messageId,
                    )

                dao.insert(
                    params.messageSubject,
                    params.messageBody,
                    params.messageImageFilePath,
                    0,
                    0,
                    params.messageToFriends,
                    params.messageToFriendsName
                )

                def dataList = this.gsql.rows("""
                    SELECT
                        id
                        ,to_friends AS toFriends
                        ,to_friends_name AS toFriendsName
                        ,subject
                        ,body
                        ,image_file_path AS imageFilePath
                        ,updated_time AS updatedTime
                        ,send_predetermined_time AS sendPredeterminedTime
                        ,send_waiting_hours AS sendWaitingHours
                        ,send_time AS sendTime
                    FROM
                        message
                    WHERE
                        send_waiting_hours = 0
                        AND send_predetermined_time = 0
                """)

                def now = Calendar.instance

                doLater {
                    postMessage( dataList.head(), client, now.timeInMillis )

                    model.statusbarText = "書き込みが完了しました。"
                    model.messageId = ""
                    model.messageToFriends = ""
                    model.messageToFriendsName = ""
                    model.messageSubject = ""
                    model.messageBody = ""
                    model.messageImageFilePath = ""
                }
            }

        } catch ( e ) {
            log.error "insertCurrentMassage - exception.", e
        } finally {
            log.info "insertCurrentMassage - end."
        }
    }

    def insertCurrentStatusMessage = { evt = null ->
        log.info "insertCurrentStatusMessage - start."
        try {
            def params = model.properties.clone()

            doOutside {
                if( params.statusMessageImageFilePath?.trim()?.size() && !new File(params.statusMessageImageFilePath).exists() ) {
                    JOptionPane.showMessageDialog( view.mainFrame, "指定されたファイルは存在しません。" )
                    return
                }

                def dao = new StatusMessageDao("gsql":this.gsql)
                if( !params.statusMessageId.empty )
                    def count = dao.delete(
                        params.statusMessageId,
                    )

                dao.insert(
                    params.statusMessageSubject,
                    params.statusMessageBody,
                    params.statusMessageImageFilePath,
                    0,
                    0,
                )

                def dataList = this.gsql.rows("""
                    SELECT
                        id
                        ,subject
                        ,body
                        ,image_file_path AS imageFilePath
                        ,updated_time AS updatedTime
                        ,send_predetermined_time AS sendPredeterminedTime
                        ,send_waiting_hours AS sendWaitingHours
                        ,send_time AS sendTime
                    FROM
                        status_message
                    WHERE
                        send_waiting_hours = 0
                        AND send_predetermined_time = 0
                """)

                def now = Calendar.instance

                doLater {
                    postStatusMessage( dataList.head(), client, now.timeInMillis )

                    model.statusbarText = "書き込みが完了しました。"
                    model.statusMessageId = ""
                    model.statusMessageSubject = ""
                    model.statusMessageBody = ""
                    model.statusMessageImageFilePath = ""
                    model.statusMessageSendPredeterminedTime = 0
                    model.statusMessageSendWaitingHours = 0
                }
            }
        } catch ( e ) {
            log.error "insertCurrentStatusMessage - exception.", e
        } finally {
            log.info "insertCurrentStatusMessage - end."
        }
    }

    def clearAutoClickLikeSettings = { evt = null ->
        log.info "clearAutoClickLikeSettings - start."
            try {
                doOutside {

                    edt {

                    }
                }
            } catch ( e ) {
                log.error "clearAutoClickLikeSettings - exception.", e
            } finally {
                log.info "clearAutoClickLikeSettings - end."
            }
    }

    def updateAutoClickLikeSettings = { evt = null ->
        log.info "updateAutoClickLikeSettings - start."
            try {

                model.with{
                    this.gsql.executeUpdate("""
                        update
                            settings
                        set
                            auto_click_like_hours_of_start_from_now = ${autoClickLikeHoursOfStartFromNow}
                            ,auto_click_like_hours_of_end_from_now = ${autoClickLikeHoursOfEndFromNow}
                            ,auto_click_like_to_me = ${autoClickLikeToMe}
                    """)

                    statusbarText = "書き込みが完了しました。"
                }

            } catch ( e ) {
                log.error "updateAutoClickLikeSettings - exception.", e
            } finally {
                log.info "updateAutoClickLikeSettings - end."
            }
    }

    int getCurrentPredetermineTime() {
        log.info "getCurrentPredetermineTime - start."
        try {
            def now = Calendar.instance.time
            return ( (int)( now.hours/3 ) ) + 1
        } catch ( e ) {
            log.error "getCurrentPredetermineTime - exception.", e
        } finally {
            log.info "getCurrentPredetermineTime - end."
        }
    }

    def postStatusMessage = { bean, api, sendTimeMills ->
        synchronized(statusMessageLockObject) {
            def content = Parameter.with("message", bean.body.toString())

            if( (bean.imageFilePath?:"").empty ) {
                log.info "post to wall : feed"
                FacebookType publishMessageResponse = api.publish(
                    "me/feed",
                    FacebookType.class,
                    content
                )
            } else {
                def imageFile = new File(bean.imageFilePath)
                if( imageFile.exists() ) {
                    log.info "post to wall : photos"
                    FacebookType publishMessageResponse = api.publish(
                        "me/photos",
                        FacebookType.class,
                        BinaryAttachment.with(imageFile.name, imageFile.newInputStream()),
                        content
                    )
                }
            }

            this.gsql.executeUpdate("update status_message set send_time = ${sendTimeMills} where id = ${bean.id}")

            edt {
                if( model.doEraseSendStatusMessage )
                    this.gsql.executeUpdate("delete from status_message where send_time is not null")
                this.reflushStatusMessage()
            }
        }

        log.info "statusMessage posted."
        log.info bean.toString()
    }

    def postMessage = { bean, api, sendTimeMills ->
        synchronized(messageLockObject) {
            bean.toFriends.split("\n").eachWithIndex{ friendId, i ->
                def content = Parameter.with("message", bean.body.toString().replaceAll(
                    /\[\[%myid%\]\]/, model.fullName
                ).replaceAll(
                    /\[\[%name%\]\]/, bean.toFriendsName.split("\n")[i]
                ))

                if( (bean.imageFilePath?:"").empty ) {
                    FacebookType publishMessageResponse = client.publish(
                        "${friendId}/feed",
                        FacebookType.class,
                        content
                    )
                } else {
                    def imageFile = new File(bean.imageFilePath)
                    if( imageFile.exists() ) {
                        FacebookType publishMessageResponse = client.publish(
                            "${friendId}/photos",
                            FacebookType.class,
                            BinaryAttachment.with(imageFile.name, imageFile.newInputStream()),
                            content
                        )
                    }
                }
            }

            this.gsql.executeUpdate("update message set send_time = ${sendTimeMills} where id = ${bean.id}")

            edt {
                if( model.doEraseSendMessage )
                    this.gsql.executeUpdate("delete from message where send_time is not null")
                this.reflushMessage()
            }
        }

        log.info "message posted."
        log.info bean.toString()
    }


}

class StatusMessageDao{
    def gsql

    def insert ={
        statusMessageSubject,
        statusMessageBody,
        statusMessageImageFilePath,
        statusMessageSendPredeterminedTime,
        statusMessageSendWaitingHours ->

        this.gsql.executeInsert("""
            insert into status_message (
                id,
                subject,
                body,
                image_file_path,
                send_time,
                send_predetermined_time,
                send_waiting_hours,
                updated_time
            )
            values (
                (select coalesce(max(id), -1) + 1 from status_message),
                ${statusMessageSubject},
                ${statusMessageBody},
                ${statusMessageImageFilePath},
                null,
                ${statusMessageSendPredeterminedTime},
                ${statusMessageSendWaitingHours},
                ${Calendar.instance.timeInMillis}
            )
        """)
    }

    def update = {
        statusMessageId,
        statusMessageSubject,
        statusMessageBody,
        statusMessageImageFilePath,
        statusMessageSendPredeterminedTime,
        statusMessageSendWaitingHours ->

        def count = this.gsql.executeUpdate("""
            update
                status_message
            set
                subject = ${statusMessageSubject},
                body = ${statusMessageBody},
                image_file_path = ${statusMessageImageFilePath},
                updated_time = ${Calendar.instance.timeInMillis},
                send_predetermined_time = ${statusMessageSendPredeterminedTime},
                send_waiting_hours = ${statusMessageSendWaitingHours}
            where
                id = ${statusMessageId}
                and send_time is null
        """)
        count
    }

    def delete = {
        statusMessageId ->

        def count = this.gsql.executeUpdate("""
            delete from
                status_message
            where
                id = ${statusMessageId}
        """)
        count
    }

}

class MessageDao{
    def gsql

    def insert ={
        messageSubject,
        messageBody,
        messageImageFilePath,
        messageSendPredeterminedTime,
        messageSendWaitingHours,
        messageToFriends,
        messageToFriendsName ->

        this.gsql.executeInsert("""
            insert into message (
                id,
                to_friends,
                to_friends_name,
                subject,
                body,
                image_file_path,
                send_time,
                send_predetermined_time,
                send_waiting_hours,
                updated_time
            )
            values (
                (select coalesce(max(id), -1) + 1 from message),
                ${messageToFriends},
                ${messageToFriendsName},
                ${messageSubject},
                ${messageBody},
                ${messageImageFilePath},
                null,
                ${messageSendPredeterminedTime},
                ${messageSendWaitingHours},
                ${Calendar.instance.timeInMillis}
            )
        """)
    }


    def update = {
        messageId,
        messageSubject,
        messageBody,
        messageImageFilePath,
        messageSendPredeterminedTime,
        messageSendWaitingHours,
        messageToFriends,
        messageToFriendsName ->

        def count = this.gsql.executeUpdate("""
            update
                message
            set
                to_friends = ${messageToFriends},
                to_friends_name = ${messageToFriendsName},
                subject = ${messageSubject},
                body = ${messageBody},
                image_file_path = ${messageImageFilePath},
                updated_time = ${Calendar.instance.timeInMillis},
                send_predetermined_time = ${messageSendPredeterminedTime},
                send_waiting_hours = ${messageSendWaitingHours}
            where
                id = ${messageId}
                and send_time is null
        """)
        count
    }

    def delete = {
        messageId ->

        def count = this.gsql.executeUpdate("""
            delete from
                message
            where
                id = ${messageId}
        """)
        count
    }

}
